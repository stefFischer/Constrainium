package at.sfischer.traces.otel.abstraction;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;

public abstract class SpanDataFinder<T> {

    private final DataCollection<T> data;

    protected SpanDataFinder(DataCollection<T> data) {
        this.data = data;
    }

    protected abstract Constraint createConstraint(Span span);

    public DataCollection<T> findData(Span span) {
        return data.filter(createConstraint(span));
    }

    protected Node and(Node left, Node right) {
        return new AndOperator(left, right);
    }

    protected Node equal(String field, String value) {
        return new StringEquals(
            new Variable(field),
            new StringLiteral(value)
        );
    }

    protected Node equal(String field, long value) {
        return new EqualOperator(
            new Variable(field),
            new NumberLiteral(value)
        );
    }
}
