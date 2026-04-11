package at.sfischer.constraints.data.model;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.VariableVisitor;
import at.sfischer.constraints.model.operators.array.ArrayEquals;
import at.sfischer.constraints.model.operators.logic.EquivalentOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;

import java.util.*;


public class DataFlow {

    private final CallEdge from;

    private final CallEdge to;

    private final Map<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> dataFlows = new IdentityHashMap<>();

    public DataFlow(CallEdge from, CallEdge to) {
        this.from = from;
        this.to = to;
    }

    public CallEdge getFrom() {
        return from;
    }

    public CallEdge getTo() {
        return to;
    }

    public Map<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> getDataFlows() {
        return dataFlows;
    }

    public void inferDataFlows(InOutputDataCollection dataCollection){
        //noinspection unchecked
        InOutputDataSchema<SimpleDataSchema> schema = (InOutputDataSchema<SimpleDataSchema>)dataCollection.deriveSchema();

        schema.fillSchemaWithConstraints(new StringEquals(new Variable("a"), new Variable("b")));
        schema.fillSchemaWithConstraints(new EqualOperator(new Variable("a"), new Variable("b")));
        schema.fillSchemaWithConstraints(new EquivalentOperator(new Variable("a"), new Variable("b")));
        schema.fillSchemaWithConstraints(new ArrayEquals(new Variable("a"), new Variable("b")));
        // TODO What to do for cases when elements of an array are passed on individually? Need a constraint that checks if a values exists in the array.

        EvaluationResults<DataSchema, Pair<DataObject, DataObject>> results = schema.evaluate(dataCollection);
        schema.applyConstraintRetentionPolicy(results, new NoViolationsPolicy());

        // Get references from constraints and link the entries from the edge schemas.
        Map<DataSchemaEntry<InOutputDataSchema<SimpleDataSchema>>, Set<Constraint>> constraints = new HashMap<>();
        schema.collectAllConstraints(constraints, null);

        for (Set<Constraint> value : constraints.values()) {
            for (Constraint constraint : value) {
                DataSchemaEntry<SimpleDataSchema>[] entries = collectEntries(constraint);
                if(entries[0] == null || entries[1] == null){
                    continue;
                }

                this.dataFlows.put(entries[0], entries[1]);
            }
        }
    }

    private DataSchemaEntry<SimpleDataSchema>[] collectEntries(Constraint constraint){
        @SuppressWarnings("unchecked")
        DataSchemaEntry<SimpleDataSchema>[] entries = new DataSchemaEntry[2];
        constraint.term().visitNodes((VariableVisitor) variable -> {
            if(variable.getName().startsWith(InOutputDataSchema.INPUT_PREFIX)){
                String path = variable.getName().substring(InOutputDataSchema.INPUT_PREFIX.length() + 1);
                DataSchemaEntry<SimpleDataSchema> entry = from.getSchema().findDataSchemaEntry(path);
                entries[0] = entry;
            }

            if(variable.getName().startsWith(InOutputDataSchema.OUTPUT_PREFIX)){
                String path = variable.getName().substring(InOutputDataSchema.OUTPUT_PREFIX.length() + 1);
                DataSchemaEntry<SimpleDataSchema> entry = to.getSchema().findDataSchemaEntry(path);
                entries[1] = entry;
            }
        });

        return entries;
    }
}
