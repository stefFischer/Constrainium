package at.scch.traceon.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Span;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class HttpUrlParameterExtractorTest {
    private final HttpUrlParameterExtractor extractor = new HttpUrlParameterExtractor();
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"/carts/{cartId}/items/{productId}", "/carts/:cartId/items/:productId"})
    void extractsNamedParametersWithEitherRouteSyntax(String route) throws Exception {
        Span span = span("GET");
        span.getAttributes().put("http.route", route);
        span.getAttributes().put("url.path", "/carts/42/items/red%20shoes/");
        assertEquals(json.readTree("{\"path_params\":{\"cartId\":\"42\",\"productId\":\"red shoes\"}}"), tree(extractor.extract(span)));
    }

    @Test
    void extractsClientUrlAndDecodesQueryValuesWithoutLosingEqualsSigns() throws Exception {
        Span span = span("GET");
        span.getAttributes().put("url.full", "http://products:8080/products/STRW-001?search=red+shoes&token=a%3Db&empty=&flag&city=Linz%20S%C3%BCd");
        JsonNode data = tree(extractor.extract(span));
        assertEquals("STRW-001", data.path("path_params").path("productId").asText());
        assertEquals(json.readTree("{\"search\":\"red shoes\",\"token\":\"a=b\",\"empty\":\"\",\"flag\":\"\",\"city\":\"Linz Süd\"}"), data.path("query"));
    }

    @Test
    void usesRouteFromSpanNameWhenRouteAttributeIsMissing() throws Exception {
        Span span = span("GET /carts/:cartId");
        span.getAttributes().put("url.path", "/carts/42");
        assertEquals("42", tree(extractor.extract(span)).path("path_params").path("cartId").asText());
    }

    @Test
    void fallsBackToIdHeuristicsWhenRouteDoesNotMatch() throws Exception {
        Span span = span("GET");
        span.getAttributes().put("http.route", "/orders/{orderId}/items/{itemId}");
        span.getAttributes().put("url.path", "/carts/f56ac64e-2146-4166-9894-66d6fcf544fc/products/STRW-001");
        JsonNode params = tree(extractor.extract(span)).path("path_params");
        assertEquals("f56ac64e-2146-4166-9894-66d6fcf544fc", params.path("cartId").asText());
        assertEquals("STRW-001", params.path("productId").asText());
        assertFalse(params.has("orderId"));
        assertFalse(params.has("itemId"));
    }

    @Test
    void rejectsPartialRouteMatchesAfterExtractingAParameter() {
        Span span = span("GET");
        span.getAttributes().put("http.route", "/carts/{cartId}/items");
        span.getAttributes().put("url.path", "/carts/42/checkout");
        assertNull(extractor.extract(span));
    }

    @Test
    void extractsOnlyParametersFromCapturePathInsteadOfSpanUrl() throws Exception {
        Span span = span("GET");
        span.getAttributes().put("http.route", "/products/{id}");
        span.getAttributes().put("url.path", "/products/old?quantity=1");
        DataObject capture = DataObject.parseData("""
                {"headers":[{"name":":path","value":"/products/42?quantity=3"},
                            {"name":"authorization","value":"ignored"},
                            {"name":"x-request-id","value":"ignored"}]}
                """);
        assertEquals(json.readTree("{\"path_params\":{\"id\":\"42\"},\"query\":{\"quantity\":\"3\"}}"), tree(extractor.extract(span, capture)));
    }

    @Test
    void toleratesMissingOrUnusableCaptureHeaders() {
        Span span = span("GET");
        assertNull(extractor.extract(span, new DataObject()));
        assertNull(extractor.extract(span, DataObject.parseData("{headers:'invalid'}")));
        assertNull(extractor.extract(span, DataObject.parseData("{headers:[{name:'authorization',value:'ignored'}]}")));
        assertNull(extractor.extract(span, DataObject.parseData("{headers:[{name:':path',value:42}]}")));
    }

    @Test
    void retainsMalformedPercentEncodingAndReturnsNullWhenNoParametersExist() throws Exception {
        Span span = span("GET");
        assertNull(extractor.extract(span));
        span.getAttributes().put("url.path", "/health");
        assertNull(extractor.extract(span));
        span.getAttributes().put("url.path", "/search?value=%ZZ");
        assertEquals("%ZZ", tree(extractor.extract(span)).path("query").path("value").asText());
    }

    private Span span(String name) {
        return new Span(name, "span", "trace", null, "server", "test", 0, 1);
    }

    @Test
    void infersRouteUsingTheSameParameterNamesAsDataExtraction() throws Exception {
        Span span = span("GET /products/HAY-002");
        span.getAttributes().put("url.path", "/products/HAY-002");
        assertEquals("/products/{productId}", extractor.inferRoute(span));
        assertEquals("HAY-002", tree(extractor.extract(span)).path("path_params").path("productId").asText());
        span.getAttributes().put("url.path", "/carts/f56ac64e-2146-4166-9894-66d6fcf544fc/items/STRW-001");
        assertEquals("/carts/{cartId}/items/{itemId}", extractor.inferRoute(span));
    }

    @Test
    void prefersDeclaredRoutesAndDoesNotGeneralizeStaticPaths() {
        Span span = span("GET");
        span.getAttributes().put("url.full", "http://products:8080/products/HAY-002?quantity=1");
        span.getAttributes().put("http.route", "/products/:sku");
        assertEquals("/products/{sku}", extractor.inferRoute(span));
        span.removeAttribute("http.route");
        span.getAttributes().put("url.full", "http://products:8080/products?category=roughage");
        assertEquals("/products", extractor.inferRoute(span));
        assertNull(extractor.inferRoute(span("GET")));
    }

    private JsonNode tree(DataObject data) throws Exception {
        assertNotNull(data);
        return json.readTree(data.toJson());
    }
}
