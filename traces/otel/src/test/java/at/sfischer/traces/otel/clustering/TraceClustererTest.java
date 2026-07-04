package at.sfischer.traces.otel.clustering;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.differ.AndSpanComparator;
import at.sfischer.traces.otel.differ.AttributesSpanComparator;
import at.sfischer.traces.otel.differ.NameAndKindSpanComparator;
import at.sfischer.traces.otel.differ.SpanComparator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TraceClustererTest {

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    private static TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer() {
        SpanComparator<AbstractSpan> clusterComparator = new AndSpanComparator<>(
                new NameAndKindSpanComparator<AbstractSpan>(),
                new AttributesSpanComparator<>()
        );
        return new TraceClusterer<>(clusterComparator, new ClusteredAbstractSpanAccumulator());
    }

    // -------------------------------------------------------------------------
    // Span construction helpers
    // -------------------------------------------------------------------------

    private AbstractSpan productServiceTrace(String productName, double price, String generatedId) {
        AbstractSpan root = new AbstractSpan("POST /products", "SERVER");
        root.getAttributes().put("service.name", "product-service");
        root.getAttributes().put("http.request.method", "POST");
        root.getAttributes().put("http.route", "/products");
        root.getAttributes().put("http.response.status_code", 200L);
        root.getAttributes().put("tracer", "io.opentelemetry.tomcat-10.0");
        root.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "name": "%s", "price": %s }
                        """.formatted(productName, price)),
                DataObject.parseData("""
                        { "id": "%s", "name": "%s", "price": %s }
                        """.formatted(generatedId, productName, price))
        ));

        AbstractSpan select = new AbstractSpan("SELECT productdb.product", "CLIENT");
        select.getAttributes().put("service.name", "product-service");
        select.getAttributes().put("db.operation", "SELECT");
        select.getAttributes().put("db.name", "productdb");
        select.getAttributes().put("tracer", "at.scch.jdbc");
        select.getAttributes().put("jdbc.sql",
                "select p1_0.id from product p1_0 where p1_0.id=? fetch first ? rows only");
        select.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "id": "%s", "limit": 1 }
                        """.formatted(generatedId)),
                DataObject.parseData("{}")
        ));

        AbstractSpan insert = new AbstractSpan("INSERT productdb.product", "CLIENT");
        insert.getAttributes().put("service.name", "product-service");
        insert.getAttributes().put("db.operation", "INSERT");
        insert.getAttributes().put("db.name", "productdb");
        insert.getAttributes().put("tracer", "at.scch.jdbc");
        insert.getAttributes().put("jdbc.sql",
                "insert into product (name,price,id) values (?,?,?)");
        insert.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "id": "%s", "name": "%s", "price": %s }
                        """.formatted(generatedId, productName, price)),
                DataObject.parseData("{}")
        ));

        root.addChild(select);
        root.addChild(insert);
        return root;
    }

    /** Same structure as productServiceTrace but with a different http.route — should not cluster together. */
    private AbstractSpan differentRouteTrace() {
        AbstractSpan root = new AbstractSpan("GET /products/{id}", "SERVER");
        root.getAttributes().put("service.name", "product-service");
        root.getAttributes().put("http.request.method", "GET");
        root.getAttributes().put("http.route", "/products/{id}");
        root.getAttributes().put("http.response.status_code", 200L);
        root.getAttributes().put("tracer", "io.opentelemetry.tomcat-10.0");
        root.setSpanData(new SpanData(
                DataObject.parseData("{}"),
                DataObject.parseData("""
                        { "id": "abc-123", "name": "Laptop", "price": 999.99 }
                        """)
        ));

        AbstractSpan select = new AbstractSpan("SELECT productdb.product", "CLIENT");
        select.getAttributes().put("service.name", "product-service");
        select.getAttributes().put("db.operation", "SELECT");
        select.getAttributes().put("db.name", "productdb");
        select.getAttributes().put("tracer", "at.scch.jdbc");
        select.getAttributes().put("jdbc.sql",
                "select p1_0.id from product p1_0 where p1_0.id=? fetch first ? rows only");
        select.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "id": "abc-123" }
                        """),
                DataObject.parseData("""
                        { "id": "abc-123" }
                        """)
        ));

        root.addChild(select);
        return root;
    }

    // -------------------------------------------------------------------------
    // Clustering behaviour tests
    // -------------------------------------------------------------------------

    @Test
    void firstAddedTraceCreatesSingleCluster() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));

        assertThat(clusterer.getClusters()).hasSize(1);
    }

    @Test
    void twoIdenticalStructureTracesProduceOneCluster() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        clusterer.add(productServiceTrace("Phone",  499.99, "def-456"));

        assertThat(clusterer.getClusters()).hasSize(1);
    }

    @Test
    void tracesWithDifferentStructureProduceSeparateClusters() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        clusterer.add(differentRouteTrace());

        assertThat(clusterer.getClusters()).hasSize(2);
    }

    @Test
    void addReturnsExistingClusterForMatchingTrace() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        TraceCluster<AbstractSpan, ClusteredAbstractSpan> first  = clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        TraceCluster<AbstractSpan, ClusteredAbstractSpan> second = clusterer.add(productServiceTrace("Phone",  499.99, "def-456"));

        assertThat(second).isSameAs(first);
    }

    @Test
    void addReturnsNewClusterForNonMatchingTrace() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        TraceCluster<AbstractSpan, ClusteredAbstractSpan> first  = clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        TraceCluster<AbstractSpan, ClusteredAbstractSpan> second = clusterer.add(differentRouteTrace());

        assertThat(second).isNotSameAs(first);
    }

    // -------------------------------------------------------------------------
    // Accumulated state tests
    // -------------------------------------------------------------------------

    @Test
    void clusterStateAfterOneTraceHasSingleSpanData() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        TraceCluster<AbstractSpan, ClusteredAbstractSpan> cluster =
                clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));

        assertThat(cluster.getState().getSpanData()).hasSize(1);
    }

    @Test
    void clusterStateAfterTwoMatchingTracesHasTwoSpanData() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        clusterer.add(productServiceTrace("Phone",  499.99, "def-456"));

        ClusteredAbstractSpan state = clusterer.getClusters().getFirst().getState();
        assertThat(state.getSpanData()).hasSize(2);
    }

    @Test
    void clusterStateChildrenAlsoAccumulateSpanData() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        clusterer.add(productServiceTrace("Phone",  499.99, "def-456"));

        ClusteredAbstractSpan state = clusterer.getClusters().getFirst().getState();

        // Both SELECT and INSERT children should each have 2 SpanData entries
        assertThat(state.getChildren()).allSatisfy(child ->
                assertThat(child.getSpanData()).hasSize(2)
        );
    }

    @Test
    void clusterRepresentativeIsFirstAddedTrace() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        AbstractSpan first  = productServiceTrace("Laptop", 999.99, "abc-123");
        AbstractSpan second = productServiceTrace("Phone",  499.99, "def-456");

        clusterer.add(first);
        clusterer.add(second);

        assertThat(clusterer.getClusters().getFirst().getRepresentative()).isSameAs(first);
    }

    @Test
    void threeTracesWithTwoDistinctStructuresProduceTwoClusters() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();

        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));
        clusterer.add(productServiceTrace("Phone",  499.99, "def-456"));
        clusterer.add(differentRouteTrace());

        assertThat(clusterer.getClusters()).hasSize(2);

        // POST cluster should have 2 accumulated spans, GET cluster should have 1
        List<ClusteredAbstractSpan> states = clusterer.getClusters().stream()
                .map(TraceCluster::getState)
                .toList();

        assertThat(states).anySatisfy(s -> assertThat(s.getSpanData()).hasSize(2));
        assertThat(states).anySatisfy(s -> assertThat(s.getSpanData()).hasSize(1));
    }

    @Test
    void getClustersReturnsUnmodifiableList() {
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = clusterer();
        clusterer.add(productServiceTrace("Laptop", 999.99, "abc-123"));

        assertThatThrownBy(() ->
                clusterer.getClusters().add(null)
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
