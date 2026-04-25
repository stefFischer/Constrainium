package at.sfischer.traces.otel.collector.grpc;

import at.sfischer.testing.Arguments;
import at.sfischer.testing.systems.PayrollApplication;
import at.sfischer.traces.otel.collector.RecordingTraceListener;
import at.sfischer.traces.otel.testing.AndSpanMatch;
import at.sfischer.traces.otel.testing.JSONMatch;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static at.sfischer.traces.otel.testing.SpanMatch.*;
import static at.sfischer.traces.otel.testing.TraceAssertion.assertSpans;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayrollApplicationOtelServerTest {

    private static PayrollApplication TEST_SYSTEM;

    private static OtelGrpcCollector otelServer;

    private static final RecordingTraceListener listener = new RecordingTraceListener();

    @BeforeAll
    public static void setup() {
        otelServer = new OtelGrpcCollector(OtelGrpcCollector.DEFAULT_GRPC_PORT);
        otelServer.collect();
        otelServer.addTraceListener(listener);

        Map<String, String> environmentVariables = Map.of("OTEL_SERVICE_NAME", "PayrollApplication");

        TEST_SYSTEM = new PayrollApplication();
        TEST_SYSTEM.start(environmentVariables,
                Arguments.getOtelJavaAgentArgument(),
                Arguments.getServiceNameArgument("PayrollApplication"),
                Arguments.getServiceNameAttributeArgument("PayrollApplication"),
                Arguments.OTEL_DEBUG_TRUE,
                Arguments.OTEL_EXPORTER_LOGGING,
                Arguments.OTEL_EXPORTER_OTLP,
                Arguments.OTEL_EXPORTER_OTLP_PROTOCOL,
                Arguments.getOtlpExportEndpointArgument("http://localhost:" + OtelGrpcCollector.DEFAULT_GRPC_PORT)
        );
    }

    @AfterAll
    public static void cleanup() {
        TEST_SYSTEM.stop();
        otelServer.stop();
    }

    @Test
    public void testCollectionGetAllEmployees() throws IOException {
        HttpUriRequest request = new HttpGet( "http://localhost:8080/employees");

        CloseableHttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );
        assertEquals(200, httpResponse.getCode());
        httpResponse.close();

        assertSpans(listener, 5000,
                new AndSpanMatch(name("GET /employees"), kind("SPAN_KIND_SERVER")),
                new AndSpanMatch(name("SELECT at.sfischer.spring.payroll.Employee"), kind("SPAN_KIND_INTERNAL")),
                new AndSpanMatch(nameRegex("SELECT .*employee"), kind("SPAN_KIND_CLIENT"), attribute("db.operation", "SELECT"))
        );
    }

    @Test
    public void testCollectionGetOneEmployee() throws IOException {
        HttpUriRequest request = new HttpGet( "http://localhost:8080/employee/1");

        CloseableHttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );
        assertEquals(200, httpResponse.getCode());
        httpResponse.close();

        assertSpans(listener, 5000,
            new AndSpanMatch(name("GET /employee/{id}"), kind("SPAN_KIND_SERVER"), attribute("url.path", "/employee/1")),
            new AndSpanMatch(nameRegex("SELECT .*employee"), kind("SPAN_KIND_CLIENT"), attribute("db.operation", "SELECT"))
        );
    }

    @Test
    public void testPostNewEmployee() throws IOException {
        String body = "{\"name\":\"Frank Stallone\",\"role\":\"Actor\"}";
        HttpUriRequest request = new HttpPost( "http://localhost:8080/employee");
        request.setHeader("Content-type", "application/json");

        StringEntity stringEntity = new StringEntity(body);
        request.setEntity(stringEntity);

        CloseableHttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );
        assertEquals(200, httpResponse.getCode());
        String responseString = new BasicHttpClientResponseHandler().handleResponse(httpResponse);
        httpResponse.close();

        assertSpans(listener, 5000,
                new AndSpanMatch(name("POST /employee"), kind("SPAN_KIND_SERVER"), attribute("url.path", "/employee")),
                new AndSpanMatch(nameRegex("INSERT .*employee"), kind("SPAN_KIND_CLIENT"), attribute("db.operation", "INSERT"))
        );


        // PUT request to change the new employee.
        int newId = JSONMatch.getFieldValue(responseString, new String[]{"id"});
        body = "{\"name\":\"Frank Stallone\",\"role\":\"Singer\"}";

        request = new HttpPut("http://localhost:8080/employee/" + newId);
        request.setHeader("Content-type", "application/json");

        stringEntity = new StringEntity(body);
        request.setEntity(stringEntity);

        httpResponse = HttpClientBuilder.create().build().execute( request );
        assertEquals(200, httpResponse.getCode());

        assertSpans(listener, 5000,
                new AndSpanMatch(name("PUT /employee/{id}"), kind("SPAN_KIND_SERVER"), attribute("url.path", "/employee/3")),
                new AndSpanMatch(nameRegex("UPDATE .*employee"), kind("SPAN_KIND_CLIENT"), attribute("db.operation", "UPDATE"))
        );


        // DELETE request to remove the new employee.
        request = new HttpDelete("http://localhost:8080/employee/" + newId);
        request.setHeader("Content-type", "application/json");

        httpResponse = HttpClientBuilder.create().build().execute( request );
        assertEquals(200, httpResponse.getCode());

        assertSpans(listener, 5000,
                new AndSpanMatch(name("DELETE /employee/{id}"), kind("SPAN_KIND_SERVER"), attribute("url.path", "/employee/3")),
                new AndSpanMatch(nameRegex("DELETE .*employee"), kind("SPAN_KIND_CLIENT"), attribute("db.operation", "DELETE"))
        );
    }
}
