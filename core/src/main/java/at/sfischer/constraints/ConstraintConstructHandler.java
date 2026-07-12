package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.data.EvaluationResults;

public interface ConstraintConstructHandler<T extends ConstraintConstruct> {

    Class<T> getSupportedType();

    void instantiate(T construct, DataSchema schema);

    <SCHEMA extends DataSchema, DATA> EvaluationResults<SCHEMA, DATA> evaluate(
            T construct,
            DataSchema schema,
            DataCollection<DATA> data);

    <SCHEMA extends DataSchema, DATA> void retain(
            T construct,
            DataSchema schema,
            EvaluationResults<SCHEMA, DATA> results);
}
