package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;

import java.util.*;

public class EvaluationResults<SCHEMA extends DataSchema, DATA> {

    private final Set<EvaluationResult<SCHEMA, DATA>> results;

    private final Map<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> constraintResults;

    private final Map<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> potentialConstraintResults;

    public EvaluationResults() {
        this.results = new HashSet<>();
        this.constraintResults = new HashMap<>();
        this.potentialConstraintResults = new HashMap<>();
    }

    public ConstraintResults<DATA> getConstraintResults(DataSchemaEntry<SCHEMA> schemaEntry, Constraint constraint, DataCollection<DATA> data){
        Set<ConstraintResults<DATA>> results = this.constraintResults.computeIfAbsent(schemaEntry, k -> new HashSet<>());
        for (ConstraintResults<DATA> result : results) {
            if(result.constraint().equals(constraint)){
                return result;
            }
        }
        ConstraintResults<DATA> result = new ConstraintResults<>(constraint, data);
        results.add(result);
        return result;
    }

    public ConstraintResults<DATA> getPotentialConstraintResults(DataSchemaEntry<SCHEMA> schemaEntry, Constraint constraint, DataCollection<DATA> data){
        Set<ConstraintResults<DATA>> results = this.potentialConstraintResults.computeIfAbsent(schemaEntry, k -> new HashSet<>());
        for (ConstraintResults<DATA> result : results) {
            if(result.constraint().equals(constraint)){
                return result;
            }
        }
        ConstraintResults<DATA> result = new ConstraintResults<>(constraint, data);
        results.add(result);
        return result;
    }

    public void addResult(EvaluationResult<SCHEMA, DATA> result) {
        this.results.add(result);
    }

    public Set<EvaluationResult<SCHEMA, DATA>> getEvaluationResults() {
        return results;
    }

    public Map<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> getConstraintResults() {
        return constraintResults;
    }

    public Map<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> getPotentialConstraintResults() {
        return potentialConstraintResults;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResults<?, ?> that = (EvaluationResults<?, ?>) o;
        return Objects.equals(results, that.results) && Objects.equals(constraintResults, that.constraintResults) && Objects.equals(potentialConstraintResults, that.potentialConstraintResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, constraintResults, potentialConstraintResults);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (EvaluationResult<SCHEMA, DATA> result : results) {
            if(!first){
                sb.append("\n");
            }
            first = false;
            sb.append("- ");
            sb.append(result);
        }

        if(!constraintResults.isEmpty()){
            if(!first){
                sb.append("\n");
            }
            sb.append("Constraints:");
            sb.append("\n");
        }
        for (Map.Entry<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> entry : constraintResults.entrySet()) {
            if(!first){
                sb.append("\n");
            }
            first = false;
            sb.append(entry.getKey().getQualifiedName());
            sb.append(":");
            for (ConstraintResults<DATA> constraintResults : entry.getValue()) {
                sb.append("\n");
                sb.append("- ");
                sb.append(constraintResults);
            }
        }

        if(!potentialConstraintResults.isEmpty()){
            if(!first){
                sb.append("\n");
            }
            sb.append("potential Constraints:");
            sb.append("\n");
        }
        for (Map.Entry<DataSchemaEntry<SCHEMA>, Set<ConstraintResults<DATA>>> entry : potentialConstraintResults.entrySet()) {
            if(!first){
                sb.append("\n");
            }
            first = false;
            sb.append(entry.getKey().getQualifiedName());
            sb.append(":");
            for (ConstraintResults<DATA> constraintResults : entry.getValue()) {
                sb.append("\n");
                sb.append("- ");
                sb.append(constraintResults);
            }
        }

        return sb.toString();
    }
}
