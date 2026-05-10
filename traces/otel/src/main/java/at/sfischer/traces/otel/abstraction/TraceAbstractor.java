package at.sfischer.traces.otel.abstraction;

import at.sfischer.traces.otel.Span;

public class TraceAbstractor {

    private final SpanAbstractor[] spanAbstractors;

    public TraceAbstractor(SpanAbstractor... spanAbstractors) {
        this.spanAbstractors = spanAbstractors;
    }

    public AbstractSpan abstractTrace(Span root) {
        AbstractSpan abstractRoot = applyAbstraction(root);
        if(abstractRoot == null){
            abstractRoot = new AbstractSpan(root.getName(), root.getKind());
        }
        abstractTrace(root, abstractRoot);
        return abstractRoot;
    }

    private void abstractTrace(Span span, AbstractSpan abstractSpan) {
        span.getChildren().forEach(child -> abstractSpan(child, abstractSpan));
    }

    private void abstractSpan(Span span, AbstractSpan abstractParent) {
        AbstractSpan current = applyAbstraction(span);
        if(current == null){
            abstractTrace(span, abstractParent);
        } else {
            abstractParent.addChild(current);
            abstractTrace(span, current);
        }
    }

    private AbstractSpan applyAbstraction(Span span) {
        for (SpanAbstractor spanAbstractor : spanAbstractors) {
            AbstractSpan current = spanAbstractor.abstractSpan(span);
            if (current != null)
                return current;
        }

        return null;
    }
}
