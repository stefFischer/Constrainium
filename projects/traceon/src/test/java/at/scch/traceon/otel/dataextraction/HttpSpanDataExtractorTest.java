package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.HttpPayloadDecoder;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.Span;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpSpanDataExtractorTest {
    @Test
    void extractsCapturedParametersWithoutBodyOrSpanUrl() {
        Span span = span();
        span.getAttributes().put("http.route", "/products/{id}");
        DataObject input = extract(span, "/products/42?search=red%20shoes&available=true");

        assertEquals("42", value(input, "path_params", "id"));
        assertEquals("red shoes", value(input, "query", "search"));
        assertEquals("true", value(input, "query", "available"));
        assertEquals(2, input.getFieldNames().size());
        assertNull(input.getDataValue("authorization"));
        assertNull(input.getDataValue("headers"));
    }

    @Test
    void preservesSpanParametersWhenCapturePathWasRewritten() {
        Span span = span();
        span.getAttributes().put("http.route", "/api/products/{id}");
        span.getAttributes().put("url.path", "/api/products/42");
        DataObject input = extract(span, "/products/42?quantity=3");

        assertEquals("42", value(input, "path_params", "id"));
        assertEquals("3", value(input, "query", "quantity"));
    }

    @Test
    void ignoresUnmatchedCaptureAndNonParameterHeaders() {
        Span span = span();
        span.getAttributes().put("url.path", "/products?quantity=1");
        DataObject input = extract(span, "/products?quantity=3");
        assertEquals("3", value(input, "query", "quantity"));

        Span other = new Span("GET", "other", "trace", null, "client", "test", 0, 100);
        assertNull(extract(other, "/products?quantity=3"));
        assertNull(extract(span(), "/products"));
    }

    private Object value(DataObject input, String group, String name) {
        return ((DataObject) input.getDataValue(group).getValue()).getDataValue(name).getValue();
    }

    private Span span() {
        return new Span("GET", "span", "trace", null, "client", "test", 0, 100);
    }

    private DataObject extract(Span span, String path) {
        DataObject capture = new DataObject();
        capture.putValue("source", "http");
        capture.putValue("direction", "REQUEST");
        capture.putValue("headers", new DataObject[]{
                header("x-b3-spanid", "span"), header(":path", path),
                header("authorization", "irrelevant"), header("x-request-id", "irrelevant")
        });
        SimpleDataCollection captures = new SimpleDataCollection();
        captures.addDataEntry(capture);
        return new HttpSpanDataExtractor(captures, new HttpPayloadDecoder()).extractData(span).getInputData();
    }

    private DataObject header(String name, String value) {
        DataObject header = new DataObject();
        header.putValue("name", name);
        header.putValue("value", value);
        return header;
    }
}
