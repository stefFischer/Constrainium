package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Attribute;
import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Event;
import at.sfischer.traces.otel.Span;

import java.util.*;

public class StructuralSpanComparator implements SpanComparator<Span> {

    @Override
    public boolean isSame(Span a, Span b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Objects.equals(a.getName(), b.getName())
                && Objects.equals(a.getTraceId(), b.getTraceId())
                && Objects.equals(a.getParentSpanId(), b.getParentSpanId())
                && Objects.equals(a.getKind(), b.getKind())
                && Objects.equals(a.getTracer(), b.getTracer())
                && attributesEqual(a.getAttributes(), b.getAttributes())
                && eventsEqual(a.getEvents(), b.getEvents());
    }

    private boolean attributesEqual(Attributes a, Attributes b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        Collection<Attribute<?>> attrsA = a.getAttributes();
        Collection<Attribute<?>> attrsB = b.getAttributes();
        if (attrsA.size() != attrsB.size()) {
            return false;
        }

        Map<String, Object> mapA = toMap(attrsA);
        Map<String, Object> mapB = toMap(attrsB);
        return mapA.equals(mapB);
    }

    private Map<String, Object> toMap(Collection<Attribute<?>> attributes) {
        Map<String, Object> map = new HashMap<>();
        for (Attribute<?> attribute : attributes) {
            map.put(attribute.getKey(), attribute.getValue());
        }

        return map;
    }

    private boolean eventsEqual(List<Event> a, List<Event> b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            Event eventA = a.get(i);
            Event eventB = b.get(i);
            if (!eventEqual(eventA, eventB)) {
                return false;
            }
        }

        return true;
    }

    private boolean eventEqual(Event a, Event b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Objects.equals(a.getName(), b.getName())
                && attributesEqual(a.getAttributes(), b.getAttributes());
    }
}
