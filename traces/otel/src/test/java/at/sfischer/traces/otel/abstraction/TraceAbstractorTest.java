package at.sfischer.traces.otel.abstraction;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.matching.SpanMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TraceAbstractorTest {

    // -------------------------------------------------------------------------
    // Factory — minimal abstractor sufficient for testing
    // -------------------------------------------------------------------------

    /**
     * Abstracts SERVER spans by name/kind + service.name,
     * and CLIENT+jdbc spans by name/kind + db.name/db.operation/jdbc.sql.
     * Everything else is skipped (abstracted away transparently).
     */
    private static TraceAbstractor abstractor() {
        return new TraceAbstractor(
                new SpanAbstractor(SpanMatch.kind("SERVER")) {
                    @Override
                    public AbstractSpan abstractSpan(Span span) {
                        AbstractSpan abs = new AbstractSpan(span.getName(), span.getKind());
                        transferAttributes(span, abs, "service.name");
                        return abs;
                    }
                },
                new SpanAbstractor(SpanMatch.kind("CLIENT")
                        .and(SpanMatch.tracer("at.scch.jdbc"))) {
                    @Override
                    public AbstractSpan abstractSpan(Span span) {
                        AbstractSpan abs = new AbstractSpan(span.getName(), span.getKind());
                        transferAttributes(span, abs, "service.name", "db.name", "db.operation", "jdbc.sql");
                        return abs;
                    }
                }
        );
    }

    // -------------------------------------------------------------------------
    // Span construction helpers
    // -------------------------------------------------------------------------

    private Span serverSpan(String name, String serviceName) {
        Span span = new Span(name, "s1", "t1", null, "SERVER",
                "io.opentelemetry.tomcat-10.0", 0L, 100L);
        span.getAttributes().put("service.name", serviceName);
        return span;
    }

    private Span jdbcSpan(String name, String serviceName, String dbName,
                          String operation, String sql) {
        Span span = new Span(name, "s2", "t1", "s1", "CLIENT",
                "at.scch.jdbc", 10L, 50L);
        span.getAttributes().put("service.name", serviceName);
        span.getAttributes().put("db.name", dbName);
        span.getAttributes().put("db.operation", operation);
        span.getAttributes().put("jdbc.sql", sql);
        return span;
    }

    private Span unknownSpan(String name) {
        return new Span(name, "s3", "t1", "s1", "INTERNAL",
                "some.lib", 10L, 20L);
    }

    // -------------------------------------------------------------------------
    // Root span abstraction
    // -------------------------------------------------------------------------

    @Test
    void serverRootSpanIsAbstracted() {
        Span root = serverSpan("POST /products", "product-service");

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result.getName()).isEqualTo("POST /products");
        assertThat(result.getKind()).isEqualTo("SERVER");
    }

    @Test
    void serverRootSpanTransfersServiceNameAttribute() {
        Span root = serverSpan("POST /products", "product-service");

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result.<String>getAttribute("service.name"))
                .isEqualTo("product-service");
    }

    @Test
    void unrecognisedRootSpanProducesFallbackAbstractSpan() {
        // No abstractor matches INTERNAL kind — should still produce a node
        Span root = unknownSpan("some-internal-op");

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("some-internal-op");
        assertThat(result.getKind()).isEqualTo("INTERNAL");
    }

    // -------------------------------------------------------------------------
    // Child span abstraction
    // -------------------------------------------------------------------------

    @Test
    void jdbcChildSpanIsAddedAsChildOfRoot() {
        Span root = serverSpan("POST /products", "product-service");
        Span jdbc = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)");
        root.addChild(jdbc);

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().getFirst().getName())
                .isEqualTo("INSERT productdb.product");
    }

    @Test
    void jdbcChildTransfersDbAttributes() {
        Span root = serverSpan("POST /products", "product-service");
        Span jdbc = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)");
        root.addChild(jdbc);

        AbstractSpan result = abstractor().abstractTrace(root);
        AbstractSpan absJdbc = result.getChildren().getFirst();

        assertThat(absJdbc.<String>getAttribute("db.name")).isEqualTo("productdb");
        assertThat(absJdbc.<String>getAttribute("db.operation")).isEqualTo("INSERT");
        assertThat(absJdbc.<String>getAttribute("jdbc.sql"))
                .isEqualTo("insert into product (name,price,id) values (?,?,?)");
    }

    @Test
    void multipleJdbcChildrenAreAllAbstracted() {
        Span root = serverSpan("POST /products", "product-service");
        root.addChild(jdbcSpan("SELECT productdb.product", "product-service",
                "productdb", "SELECT",
                "select p1_0.id from product p1_0 where p1_0.id=?"));
        root.addChild(jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)"));

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result.getChildren()).hasSize(2);
        assertThat(result.getChildren().stream().map(AbstractSpan::getName))
                .containsExactlyInAnyOrder(
                        "SELECT productdb.product",
                        "INSERT productdb.product"
                );
    }

    // -------------------------------------------------------------------------
    // Transparent span skipping
    // -------------------------------------------------------------------------

    @Test
    void unrecognisedChildSpanIsSkippedTransparently() {
        Span root = serverSpan("POST /products", "product-service");
        Span internal = unknownSpan("framework-internal");
        Span jdbc = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)");
        // jdbc is nested under an unrecognised span
        internal.addChild(jdbc);
        root.addChild(internal);

        AbstractSpan result = abstractor().abstractTrace(root);

        // The INTERNAL span is skipped but its JDBC child is promoted to root's children
        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().getFirst().getName())
                .isEqualTo("INSERT productdb.product");
    }

    @Test
    void deeplyNestedJdbcSpanIsPromotedToNearestAbstractedAncestor() {
        Span root = serverSpan("POST /products", "product-service");
        Span wrapper1 = unknownSpan("wrapper-1");
        Span wrapper2 = unknownSpan("wrapper-2");
        Span jdbc = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)");

        wrapper2.addChild(jdbc);
        wrapper1.addChild(wrapper2);
        root.addChild(wrapper1);

        AbstractSpan result = abstractor().abstractTrace(root);

        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().getFirst().getName())
                .isEqualTo("INSERT productdb.product");
    }

    @Test
    void unrecognisedSpanBetweenTwoJdbcSpansIsSkipped() {
        Span root = serverSpan("POST /products", "product-service");
        Span internal = unknownSpan("framework-internal");
        Span select = jdbcSpan("SELECT productdb.product", "product-service",
                "productdb", "SELECT", "select p1_0.id from product p1_0 where p1_0.id=?");
        Span insert = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT", "insert into product (name,price,id) values (?,?,?)");

        internal.addChild(select);
        root.addChild(internal);
        root.addChild(insert);

        AbstractSpan result = abstractor().abstractTrace(root);

        // Both JDBC spans should be direct children of root
        assertThat(result.getChildren()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // batch process()
    // -------------------------------------------------------------------------

    @Test
    void processAbstractsAllTracesInList() {
        Span trace1 = serverSpan("POST /products", "product-service");
        Span trace2 = serverSpan("GET /orders",    "order-service");

        List<AbstractSpan> results = abstractor().process(List.of(trace1, trace2));

        assertThat(results).hasSize(2);
        assertThat(results.stream().map(AbstractSpan::getName))
                .containsExactlyInAnyOrder("POST /products", "GET /orders");
    }

    @Test
    void processOnEmptyListReturnsEmptyList() {
        List<AbstractSpan> results = abstractor().process(List.of());
        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Abstractor returns null — span should be skipped
    // -------------------------------------------------------------------------

    @Test
    void abstractorReturningNullCausesSpanToBeSkipped() {
        // Abstractor matches CLIENT+jdbc but deliberately returns null
        TraceAbstractor nullReturningAbstractor = new TraceAbstractor(
                new SpanAbstractor(SpanMatch.kind("SERVER")) {
                    @Override
                    public AbstractSpan abstractSpan(Span span) {
                        return new AbstractSpan(span.getName(), span.getKind());
                    }
                },
                new SpanAbstractor(SpanMatch.kind("CLIENT")
                        .and(SpanMatch.tracer("at.scch.jdbc"))) {
                    @Override
                    public AbstractSpan abstractSpan(Span span) {
                        return null; // signal: skip this span
                    }
                }
        );

        Span root = serverSpan("POST /products", "product-service");
        Span jdbc = jdbcSpan("INSERT productdb.product", "product-service",
                "productdb", "INSERT",
                "insert into product (name,price,id) values (?,?,?)");
        root.addChild(jdbc);

        AbstractSpan result = nullReturningAbstractor.abstractTrace(root);

        // JDBC span matched but returned null — should be absent from children
        assertThat(result.getChildren()).isEmpty();
    }
}
