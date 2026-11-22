package at.sfischer.constraints.data.model;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.IConstraint;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.operators.array.ArrayEquals;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.array.ForEach;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.logic.EquivalentOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;

import java.util.*;


public class DataFlow {

    private final String traceId;

    private final CallEdge from;

    private final CallEdge to;

    private final Integer sourceRow;

    private final Integer targetRow;

    private final Map<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> dataFlows = new IdentityHashMap<>();
    private boolean frozen;

    public DataFlow(String traceId, CallEdge from, CallEdge to) {
        this(traceId, from, to, null, null);
    }

    /** Row references identify occurrences within each edge and trace, even after payload disposal. */
    public DataFlow(String traceId, CallEdge from, CallEdge to, Integer sourceRow, Integer targetRow) {
        if ((sourceRow == null) != (targetRow == null)
                || (sourceRow != null && (sourceRow < 0 || targetRow < 0))) {
            throw new IllegalArgumentException("Provide two non-negative row references, or neither for legacy flows.");
        }

        this.traceId = traceId;
        this.from = from;
        this.to = to;
        this.sourceRow = sourceRow;
        this.targetRow = targetRow;
    }

    public String getTraceId() {
        return traceId;
    }

    public CallEdge getFrom() {
        return from;
    }

    public CallEdge getTo() {
        return to;
    }

    public Integer getSourceRow() {
        return sourceRow;
    }

    public Integer getTargetRow() {
        return targetRow;
    }

    DataFlow forOccurrence(CallRelation relation) {
        boolean forward = from == relation.source() && to == relation.target();
        boolean reverse = from == relation.target() && to == relation.source();
        if (!forward && !reverse) {
            throw new IllegalArgumentException("The occurrence must refer to this flow's edges.");
        }
        DataFlow flow = new DataFlow(relation.traceId(), from, to,
                forward ? relation.sourceRow() : relation.targetRow(),
                forward ? relation.targetRow() : relation.sourceRow());
        flow.dataFlows.putAll(dataFlows);
        return flow;
    }

    public Map<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> getDataFlows() {
        return Collections.unmodifiableMap(dataFlows);
    }

    public void addMapping(DataSchemaEntry<SimpleDataSchema> source, DataSchemaEntry<SimpleDataSchema> target) {
        checkMutable();
        dataFlows.put(source, target);
    }

    private void checkMutable() {
        if (frozen) {
            throw new IllegalStateException("The data-flow is finalized.");
        }
    }

    void freeze() {
        frozen = true;
    }

    public void inferDataFlows(InOutputDataCollection dataCollection, SimpleDataSchema fromSchema, SimpleDataSchema toSchema, TypePromotionPolicy typePromotionPolicy){
        checkMutable();
        //noinspection unchecked
        InOutputDataSchema<SimpleDataSchema> schema = dataCollection.deriveSchema(typePromotionPolicy);

        schema.fillSchemaWithConstraints(new StringEquals(new Variable("a"), new Variable("b")), term -> Set.of(new Constraint(term)));
        schema.fillSchemaWithConstraints(new EqualOperator(new Variable("a"), new Variable("b")), term -> Set.of(new Constraint(term)));
        schema.fillSchemaWithConstraints(new EquivalentOperator(new Variable("a"), new Variable("b")), term -> Set.of(new Constraint(term)));
        schema.fillSchemaWithConstraints(new ArrayEquals(new Variable("a"), new Variable("b")), term -> Set.of(new Constraint(term)));
        Variable element = new Variable(ArrayOperation.ELEMENT_NAME);
        Variable candidate = new Variable("b");
        List<Node> membershipComparisons = List.of(
                new StringEquals(element, candidate),
                new EqualOperator(element, candidate),
                new EquivalentOperator(element, candidate),
                new ArrayEquals(element, candidate)
        );
        for (Node comparison : membershipComparisons) {
            Node membership = new Exists(new Variable("a"), comparison);
            schema.fillSchemaWithConstraints(membership, term -> Set.of(new Constraint(term)));
        }

        EvaluationResults<DataSchema, Pair<DataObject, DataObject>> results = schema.evaluate(dataCollection);
        schema.applyConstraintRetentionPolicy(results, new NoViolationsPolicy());

        // Get references from constraints and link the entries from the edge schemas.
        Map<DataSchemaEntry<InOutputDataSchema<SimpleDataSchema>>, Set<IConstraint>> constraints = new HashMap<>();
        schema.collectAllConstraints(constraints, null);

        for (Set<IConstraint> value : constraints.values()) {
            for (IConstraint constraint : value) {
                if(!(constraint instanceof Constraint c)){
                    continue;
                }
                DataSchemaEntry<SimpleDataSchema>[] entries = collectEntries(c, fromSchema, toSchema);
                if(entries[0] == null || entries[1] == null){
                    continue;
                }

                this.dataFlows.put(entries[0], entries[1]);
            }
        }
    }

    private DataSchemaEntry<SimpleDataSchema>[] collectEntries(Constraint constraint, SimpleDataSchema fromSchema, SimpleDataSchema toSchema){
        @SuppressWarnings("unchecked")
        DataSchemaEntry<SimpleDataSchema>[] entries = new DataSchemaEntry[2];
        collectEntries(constraint.term(), null, fromSchema, toSchema, entries);
        return entries;
    }

    private void collectEntries(Node term, String elementPath, SimpleDataSchema fromSchema,
                                SimpleDataSchema toSchema, DataSchemaEntry<SimpleDataSchema>[] entries) {
        if (term instanceof ArrayQuantifier quantifier) {
            String arrayPath = referencePath(quantifier.getParameter(0), elementPath);
            collectEntries(quantifier.getParameter(1), arrayPath, fromSchema, toSchema, entries);
            return;
        }

        String path = referencePath(term, elementPath);
        if (path != null) {
            if (path.startsWith(InOutputDataSchema.INPUT_PREFIX + ".")) {
                entries[0] = fromSchema.findDataSchemaEntry(path.substring(InOutputDataSchema.INPUT_PREFIX.length() + 1));
            } else if (path.startsWith(InOutputDataSchema.OUTPUT_PREFIX + ".")) {
                entries[1] = toSchema.findDataSchemaEntry(path.substring(InOutputDataSchema.OUTPUT_PREFIX.length() + 1));
            }
            return;
        }

        if (term.getChildren() != null) {
            for (Node child : term.getChildren()) {
                collectEntries(child, elementPath, fromSchema, toSchema, entries);
            }
        }
    }

    private String referencePath(Node term, String elementPath) {
        if (term instanceof ForEach projection) {
            String arrayPath = referencePath(projection.getParameter(0), elementPath);
            return referencePath(projection.getParameter(1), arrayPath);
        }

        if (term instanceof Variable variable) {
            return variable.getName().equals(ArrayOperation.ELEMENT_NAME) ? elementPath : variable.getName();
        }

        if (term instanceof Reference reference && reference.getParameter(1) instanceof StringLiteral field) {
            String objectPath = referencePath(reference.getParameter(0), elementPath);
            return objectPath == null ? null : objectPath + "." + field.getValue();
        }

        return null;
    }
}
