package at.sfischer.traces.otel.collector.grpc;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

import java.util.List;

public class OtelGrpcReceiver extends TraceServiceGrpc.TraceServiceImplBase {

    private final TraceListener listener;

    public OtelGrpcReceiver(TraceListener listener) {
        this.listener = listener;
    }

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {

        List<Span> spans = request.getResourceSpansList()
                .stream()
                .flatMap(this::convertResourceSpans)
                .toList();

        listener.spansCollected(spans);

        responseObserver.onNext(
                ExportTraceServiceResponse.newBuilder().build()
        );
        responseObserver.onCompleted();
    }

    private java.util.stream.Stream<Span> convertResourceSpans(ResourceSpans rs) {
        String tracer = rs.getScopeSpansList().stream()
                .findFirst()
                .map(scope -> scope.getScope().getName())
                .orElse(null);

        return rs.getScopeSpansList().stream()
                .flatMap(scopeSpans ->
                        scopeSpans.getSpansList().stream()
                                .map(span -> {
                                    long start = span.getStartTimeUnixNano();
                                    long end = span.getEndTimeUnixNano();

                                    Attributes attributes = convertAttributes(span.getAttributesList());
                                    Span s = new Span(
                                            span.getName(),
                                            toHex(span.getSpanId()),
                                            toHex(span.getTraceId()),
                                            span.getParentSpanId() != ByteString.EMPTY
                                                    ? span.getParentSpanId().toStringUtf8()
                                                    : null,
                                            span.getKind().name(),
                                            tracer,
                                            start,
                                            end
                                    );
                                    s.putAttributes(attributes);
                                    return s;
                                })
                );
    }

    private Attributes convertAttributes(List<KeyValue> attrs) {
        Attributes result = new Attributes();
        for (KeyValue attr : attrs) {
            String key = attr.getKey();
            AnyValue value = attr.getValue();
            switch (value.getValueCase()) {
                case STRING_VALUE:
                    result.put(key, value.getStringValue());
                    break;
                case INT_VALUE:
                    result.put(key, value.getIntValue());
                    break;
                case DOUBLE_VALUE:
                    result.put(key, value.getDoubleValue());
                    break;
                case BOOL_VALUE:
                    result.put(key, value.getBoolValue());
                    break;
                case ARRAY_VALUE:
                    ArrayValue array = value.getArrayValue();
                    List<AnyValue> values = array.getValuesList();
                    boolean allInt = values.stream().allMatch(v -> v.getValueCase() == AnyValue.ValueCase.INT_VALUE);
                    boolean allDouble = values.stream().allMatch(v -> v.getValueCase() == AnyValue.ValueCase.DOUBLE_VALUE);
                    boolean allBool = values.stream().allMatch(v -> v.getValueCase() == AnyValue.ValueCase.BOOL_VALUE);
                    boolean allString = values.stream().allMatch(v -> v.getValueCase() == AnyValue.ValueCase.STRING_VALUE);
                    if (allInt) {
                        long[] arr = values.stream().mapToLong(AnyValue::getIntValue).toArray();
                        result.put(key, arr);
                    }
                    else if (allDouble) {
                        double[] arr = values.stream().mapToDouble(AnyValue::getDoubleValue).toArray();
                        result.put(key, arr);
                    }
                    else if (allBool) {
                        boolean[] arr = new boolean[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = values.get(i).getBoolValue();
                        }
                        result.put(key, arr);
                    }
                    else if (allString) {
                        String[] arr = values.stream().map(AnyValue::getStringValue).toArray(String[]::new);
                        result.put(key, arr);
                    }
            }
        }

        return result;
    }

    private static String toHex(ByteString bytes) {
        byte[] arr = bytes.toByteArray();
        StringBuilder sb = new StringBuilder(arr.length * 2);
        for (byte b : arr) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
