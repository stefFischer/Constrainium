package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.SpanDataFinder;

public class MongoRequestSpanDataFinder
        extends SpanDataFinder<DataObject> {

    public MongoRequestSpanDataFinder(DataCollection<DataObject> data) {
        super(data);
    }

    @Override
    protected Constraint createConstraint(Span span) {
        long requestId = Long.parseLong(span.getAttribute("db.mongodb.request_id"));
        String operation = span.getAttribute("db.operation.name");
        return new Constraint(and(
                new Exists(new Variable("headers"), and(
                        new StringEquals(
                                new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("name")),
                                new StringLiteral("mongo.request_id")
                        ),
                        new StringEquals(
                                new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("value")),
                                new StringLiteral(requestId + 1 + "")
                        )
                )),
                new Exists(new Variable("headers"), and(
                        new StringEquals(
                                new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("name")),
                                new StringLiteral("mongo.command")
                        ),
                        new StringEquals(
                                new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("value")),
                                new StringLiteral(operation)
                        )
                ))
        ));
    }
}
