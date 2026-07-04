package at.sfischer.traces.otel.dataextraction.rest;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static at.sfischer.traces.otel.dataextraction.SpanAttributeExtractor.attribute;

public class RestSpanDataExtractorTest {

    RestSpanDataExtractor dataExtractor = new RestSpanDataExtractor(
            attribute("http.route"),
            attribute("url.path"),
            attribute("http.request.body"),
            attribute("http.response.body")
    );

    @Test
    public void parsePostAddToCart() {
        String httpRoute = "/carts/{userId}/add/{productId}";
        String urlPath = "/carts/3a54f12b-cae8-4d26-979c-d8061cfa1f67/add/9141b1ef";
        String responseBody = "{\"userId\":\"3a54f12b-cae8-4d26-979c-d8061cfa1f67\",\"items\":[{\"product\":{\"id\":\"9141b1ef\",\"name\":\"Sleek Linen Shoes\",\"price\":47.25},\"quantity\":1}]}";

        AbstractSpan span = new AbstractSpan("POST /carts/{userId}/add/{productId}", "SERVER");
        Attributes attributes = new Attributes();
        attributes.put("http.route", httpRoute);
        attributes.put("url.path", urlPath);
        attributes.put("http.response.body", responseBody);
        span.putAttributes(attributes);

        SpanData data = dataExtractor.extractData(span);

        // path parameters
        DataObject path = (DataObject) data.getInputData().getDataValue("path").getValue();
        assertEquals(Set.of("userId", "productId"), path.getFieldNames());
        assertEquals("3a54f12b-cae8-4d26-979c-d8061cfa1f67", path.getDataValue("userId").getValue());
        assertEquals("9141b1ef", path.getDataValue("productId").getValue());

        // no request body
        assertNull(data.getInputData().getDataValue("body"));

        // response body
        DataObject output = data.getOutputData();
        assertEquals("3a54f12b-cae8-4d26-979c-d8061cfa1f67", output.getDataValue("userId").getValue());

        DataObject[] items = (DataObject[]) output.getDataValue("items").getValue();
        assertEquals(1, items.length);
        assertEquals(1, items[0].getDataValue("quantity").getValue());

        DataObject product = (DataObject) items[0].getDataValue("product").getValue();
        assertEquals("9141b1ef", product.getDataValue("id").getValue());
        assertEquals("Sleek Linen Shoes", product.getDataValue("name").getValue());
        assertEquals(new BigDecimal("47.25"), product.getDataValue("price").getValue());
    }

    @Test
    public void parseGetProduct() {
        String httpRoute = "/products/{id}";
        String urlPath = "/products/9141b1ef";
        String responseBody = "{\"id\":\"9141b1ef\",\"name\":\"Sleek Linen Shoes\",\"price\":47.25}";

        AbstractSpan span = new AbstractSpan("GET /products/{id}", "SERVER");
        Attributes attributes = new Attributes();
        attributes.put("http.route", httpRoute);
        attributes.put("url.path", urlPath);
        attributes.put("http.response.body", responseBody);
        span.putAttributes(attributes);

        SpanData data = dataExtractor.extractData(span);

        // path parameters
        DataObject path = (DataObject) data.getInputData().getDataValue("path").getValue();
        assertEquals(Set.of("id"), path.getFieldNames());
        assertEquals("9141b1ef", path.getDataValue("id").getValue());

        // no request body
        assertNull(data.getInputData().getDataValue("body"));

        // response body
        DataObject output = data.getOutputData();
        assertEquals(Set.of("id", "name", "price"), output.getFieldNames());
        assertEquals("9141b1ef", output.getDataValue("id").getValue());
        assertEquals("Sleek Linen Shoes", output.getDataValue("name").getValue());
        assertEquals(new BigDecimal("47.25"), output.getDataValue("price").getValue());
    }

    @Test
    public void parsePostCheckoutNotFound() {
        String httpRoute = "/checkout/{userId}";
        String urlPath = "/checkout/3a54f12b-cae8-4d26-979c-d8061cfa1f67";
        String responseBody = "{\"timestamp\":\"2026-05-01T15:07:19.038+00:00\",\"status\":404,\"error\":\"Not Found\",\"path\":\"/checkout/3a54f12b-cae8-4d26-979c-d8061cfa1f67\"}";

        AbstractSpan span = new AbstractSpan("POST /checkout/{userId}", "SERVER");
        Attributes attributes = new Attributes();
        attributes.put("http.route", httpRoute);
        attributes.put("url.path", urlPath);
        attributes.put("http.response.body", responseBody);
        span.putAttributes(attributes);

        SpanData data = dataExtractor.extractData(span);

        // path parameters
        DataObject path = (DataObject) data.getInputData().getDataValue("path").getValue();
        assertEquals(Set.of("userId"), path.getFieldNames());
        assertEquals("3a54f12b-cae8-4d26-979c-d8061cfa1f67", path.getDataValue("userId").getValue());

        // response body — error response is still parsed
        DataObject output = data.getOutputData();
        assertEquals(404, output.getDataValue("status").getValue());
        assertEquals("Not Found", output.getDataValue("error").getValue());
        assertEquals("/checkout/3a54f12b-cae8-4d26-979c-d8061cfa1f67", output.getDataValue("path").getValue());
    }

    @Test
    public void parseDeleteClearCart() {
        String httpRoute = "/carts/{userId}/clear";
        String urlPath = "/carts/3a54f12b-cae8-4d26-979c-d8061cfa1f67/clear";

        AbstractSpan span = new AbstractSpan("DELETE /carts/{userId}/clear", "SERVER");
        Attributes attributes = new Attributes();
        attributes.put("http.route", httpRoute);
        attributes.put("url.path", urlPath);
        span.putAttributes(attributes);

        SpanData data = dataExtractor.extractData(span);

        // path parameters
        DataObject path = (DataObject) data.getInputData().getDataValue("path").getValue();
        assertEquals(Set.of("userId"), path.getFieldNames());
        assertEquals("3a54f12b-cae8-4d26-979c-d8061cfa1f67", path.getDataValue("userId").getValue());

        // no body either way
        assertNull(data.getInputData().getDataValue("body"));
        assertNull(data.getOutputData());
    }

    @Test
    public void parsePostCreateProduct() {
        String httpRoute = "/products";
        String urlPath = "/products";
        String requestBody = "{\"id\":\"f94983a1-92d3-43e8-87c8-14bda2a32a8a\",\"name\":\"Lightweight Wooden Bench\",\"price\":70.23}";
        String responseBody = "{\"id\":\"a121f5fb\",\"name\":\"Lightweight Wooden Bench\",\"price\":70.23}";

        AbstractSpan span = new AbstractSpan("POST /products", "SERVER");
        Attributes attributes = new Attributes();
        attributes.put("http.route", httpRoute);
        attributes.put("url.path", urlPath);
        attributes.put("http.request.body", requestBody);
        attributes.put("http.response.body", responseBody);
        span.putAttributes(attributes);

        SpanData data = dataExtractor.extractData(span);

        // no path parameters on this route
        assertNull(data.getInputData().getDataValue("path"));

        // request body
        DataObject body = (DataObject) data.getInputData().getDataValue("body").getValue();
        assertEquals(Set.of("id", "name", "price"), body.getFieldNames());
        assertEquals("f94983a1-92d3-43e8-87c8-14bda2a32a8a", body.getDataValue("id").getValue());
        assertEquals("Lightweight Wooden Bench", body.getDataValue("name").getValue());
        assertEquals(new BigDecimal("70.23"), body.getDataValue("price").getValue());

        // response body — note different id than request
        DataObject output = data.getOutputData();
        assertEquals(Set.of("id", "name", "price"), output.getFieldNames());
        assertEquals("a121f5fb", output.getDataValue("id").getValue());
        assertEquals("Lightweight Wooden Bench", output.getDataValue("name").getValue());
        assertEquals(new BigDecimal("70.23"), output.getDataValue("price").getValue());
    }
}
