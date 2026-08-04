package at.sfischer.constraints;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.*;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import org.javatuples.Pair;

import java.util.*;

public class MetamorphicRelationHandler implements ConstraintConstructHandler<MetamorphicRelationTemplate>{

    public static final String SOURCE_OUtPUT_PREFIX = "source";
    public static final String FOLLOWUP_OUTPUT_PREFIX = "followup";

    private final SystemDriver driver;

    public MetamorphicRelationHandler(SystemDriver driver) {
        this.driver = driver;
    }

    @Override
    public Class<MetamorphicRelationTemplate> getSupportedType() {
        return MetamorphicRelationTemplate.class;
    }

    @Override
    public void instantiate(MetamorphicRelationTemplate construct, DataSchema schema) {
        if(schema instanceof InOutputDataSchema<?> inout){
            DataSchema in = inout.getInputSchema();
            DataSchema out = inout.getOutputSchema();
            if(in instanceof SimpleDataSchema inputSchema && out instanceof SimpleDataSchema outputSchema){
                // Fill transformation in input schema.
                Set<MetamorphicRelation> mrTransformationsTemp = new HashSet<>();
                SimpleDataSchema inClone = inputSchema.clone();
                inClone.fillSchemaWithConstraints(construct.getTransformation(), term -> {
                    MetamorphicRelation mr = new MetamorphicRelation(construct, term, null);
                    mrTransformationsTemp.add(mr);
                    return Set.of(mr);
                });

                // Replace data references from clone with the ones of the original schema,
                Set<MetamorphicRelation> mrTransformations = new HashSet<>();
                String inputPrefix = inout.getInputPrefix();
                for (MetamorphicRelation mrTransformation : mrTransformationsTemp) {
                    Set<Variable> variables = mrTransformation.getTransformation().findInvolvedVariables();
                    Map<Variable, Node> values = new HashMap<>();
                    for (Variable variable : variables) {
                        String name = variable.getName();
                        DataSchemaEntry<?> entry = inout.findDataSchemaEntry(inputPrefix + "." + name);
                        values.put(variable, new DataReference(entry));
                    }

                    Node transformation = mrTransformation.getTransformation().setVariableValues(values);
                    MetamorphicRelation mr = new MetamorphicRelation(construct, transformation, null);
                    mrTransformations.add(mr);
                }

                // Fill validation in output schemas.
                SimpleDataSchema outClone = outputSchema.clone();
                InOutputDataSchema<SimpleDataSchema> validationSchema = new InOutputDataSchema<>(outClone, outputSchema, SOURCE_OUtPUT_PREFIX, FOLLOWUP_OUTPUT_PREFIX);
                validationSchema.fillSchemaWithConstraints(construct.getValidation(), term -> {
                    Set<IConstraint> fullMrs = new HashSet<>();
                    for (MetamorphicRelation mrTransformation : mrTransformations) {
                        fullMrs.add(new MetamorphicRelation(construct, mrTransformation.getTransformation(), term.cloneNode()));
                    }
                    return fullMrs;
                });
            }
        }
    }

    @Override
    public <SCHEMA extends DataSchema, DATA> EvaluationResults<SCHEMA, DATA> evaluate(MetamorphicRelationTemplate construct, DataSchema schema, DataCollection<DATA> data) {
        // TODO We could also support SimpleDataCollection and call the driver to also generate source outputs.
        if(schema instanceof InOutputDataSchema<?> inout && data instanceof InOutputDataCollection inoutData) {
            DataSchema in = inout.getInputSchema();
            DataSchema out = inout.getOutputSchema();
            if (in instanceof SimpleDataSchema inputSchema && out instanceof SimpleDataSchema outputSchema) {
                // Evaluation:
                //  1. Collect the derived MRs from construct
                //  2. Group them by the same transformation to only apply transformation and call driver once
                //  3. Apply the transformation to input data
                //  4. Use System driver with transformed input to get transformed output
                //  5. Create a data collection of only using the outputs, with the prefixes as decided above.
                //  6. Evaluate the transformation on the outputs, only on the collected MRs

                //  1. Collect the derived MRs from construct
                Map<DataSchemaEntry<SimpleDataSchema>, Set<IConstraint>> potentialConstraints = new HashMap<>();
                inout.collectAllConstraints(null, potentialConstraints, construct);

                //  2. Group them by the same transformation to only apply transformation and call driver once
                Map<Node, List<MetamorphicRelation>> groups = new HashMap<>();
                for (Set<IConstraint> set : potentialConstraints.values()) {
                    for (IConstraint c : set) {
                        MetamorphicRelation mr = (MetamorphicRelation) c;
                        groups.computeIfAbsent(
                                mr.getTransformation(),
                                k -> new ArrayList<>()
                        ).add(mr);
                    }
                }

                EvaluationResults<SCHEMA, DATA> evaluationResults = new EvaluationResults<>();
                for (Map.Entry<Node, List<MetamorphicRelation>> group : groups.entrySet()) {
                    Node transformation = group.getKey();
                    InOutputDataCollection sourceFollowupData = new InOutputDataCollection();
                    for (Pair<DataObject, DataObject> dataEntry : inoutData.getDataCollection()) {
                        //  3. Apply the transformation to input data
                        Set<Variable> constraintVariables = transformation.findInvolvedVariables();
                        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(dataEntry.getValue0(), constraintVariables);
                        for (Map<Variable, Node> valueCombination : valueCombinations) {
                            Node transformed = transformation.setVariableValues(valueCombination);
                            transformed = transformed.evaluate();

                            if(transformed instanceof Value<?> val){
                                DataObject transformedInput = dataEntry.getValue0().clone();
                                transformedInput.putNodeValue(valueCombination.keySet().iterator().next().getName(), val);

                                //  4. Use System driver with transformed input to get transformed output
                                try {
                                    DataObject transformedOutput = this.driver.execute(transformedInput);
                                    DataObject sourceOutput = (DataObject)dataEntry.getValue1().getDataValue(inout.getOutputPrefix()).getValue();

                                    //  5. Create a data collection of only using the outputs, with the prefixes as decided above.
                                    sourceFollowupData.addDataEntry(sourceOutput, transformedOutput, SOURCE_OUtPUT_PREFIX, FOLLOWUP_OUTPUT_PREFIX);

                                } catch (DriverException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                // Mark as inapplicable in the results.
                                for (MetamorphicRelation metamorphicRelation : group.getValue()) {
                                    Map<DataSchemaEntry<SCHEMA>, Set<IConstraint>> pc = new HashMap<>();
                                    inout.collectConstraints(null, pc, Set.of(metamorphicRelation));
                                    ConstraintResults<DATA> cr = evaluationResults.getPotentialConstraintResults(pc.keySet().iterator().next(), metamorphicRelation, data);
                                    //noinspection unchecked
                                    cr.inapplicableConstraintData().addDataEntry((DATA) dataEntry);
                                }
                            }
                        }
                    }

                    SimpleDataSchema outClone = outputSchema.clone();
                    outClone.clearConstrains();

                    InOutputDataSchema<SimpleDataSchema> validationSchema = new InOutputDataSchema<>(outClone, outputSchema, SOURCE_OUtPUT_PREFIX, FOLLOWUP_OUTPUT_PREFIX);

                    //  6. Evaluate the transformation on the outputs, only on the collected MRs
                    Map<DataSchemaEntry<SCHEMA>, Set<IConstraint>> c = new HashMap<>();
                    Map<DataSchemaEntry<SCHEMA>, Set<IConstraint>> pc = new HashMap<>();
                    validationSchema.collectConstraints(c, pc, group.getValue());

                    @SuppressWarnings("unchecked")
                    EvaluationResults<SCHEMA, DATA> res = (EvaluationResults<SCHEMA, DATA>) validationSchema.evaluate(sourceFollowupData, c, pc);

                    evaluationResults.addResults(res);
                }

                return evaluationResults;
            }
        }

        return null;
    }

    @Override
    public <SCHEMA extends DataSchema, DATA> void retain(MetamorphicRelationTemplate construct, DataSchema schema, EvaluationResults<SCHEMA, DATA> results) {
        schema.applyConstraintRetentionPolicy(results, construct);
    }
}
