package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.data.EvaluationResults;
import at.sfischer.constraints.data.InOutputDataSchema;

public class ConstraintHandler implements ConstraintConstructHandler<ConstraintTemplate>{

    private final boolean fillSubSchemata;

    public ConstraintHandler() {
        this(false);
    }

    public ConstraintHandler(boolean fillSubSchemata) {
        this.fillSubSchemata = fillSubSchemata;
    }

    @Override
    public Class<ConstraintTemplate> getSupportedType() {
        return ConstraintTemplate.class;
    }

    @Override
    public void instantiate(ConstraintTemplate construct, DataSchema schema) {
        schema.fillSchemaWithConstraints(construct.getTerm(), term -> new Constraint(term, construct));

        if(this.fillSubSchemata && schema instanceof InOutputDataSchema<?> inOutputDataSchema){
            inOutputDataSchema.getInputSchema().fillSchemaWithConstraints(construct.getTerm(), term -> new Constraint(term, construct));
            inOutputDataSchema.getOutputSchema().fillSchemaWithConstraints(construct.getTerm(), term -> new Constraint(term, construct));
        }
    }

    @Override
    public <SCHEMA extends DataSchema, DATA> EvaluationResults<SCHEMA, DATA> evaluate(ConstraintTemplate construct, DataSchema schema, DataCollection<DATA> data) {
        return schema.evaluate(data);
    }

    @Override
    public <SCHEMA extends DataSchema, DATA> void retain(ConstraintTemplate construct, DataSchema schema, EvaluationResults<SCHEMA, DATA> results) {
        schema.applyConstraintRetentionPolicy(results, construct);
    }
}
