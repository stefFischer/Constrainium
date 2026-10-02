package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.parser.ParseException;
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainComputeProperties {

    public static void main(String[] args) throws IOException, ParseException {
        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/time-series_datasets/m4monthly";

        ConstraintTemplateFile file = Constants.parse(Constants.RELATION_PARTS);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);

        File dataDir = new File(dataPath);
        for (File jsonl : Objects.requireNonNull(dataDir.listFiles(
                f -> f.getName().endsWith(".jsonl") && !f.getName().endsWith("-properties.jsonl"))
        )) {
            InOutputDataCollection data = InOutputDataCollection.parseData(jsonl);
            InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(null);

            InOutputDataCollection properties = new InOutputDataCollection();

            DataSchemaEntry<SimpleDataSchema> context = schema.findDataSchemaEntry("input.forecastrequest.context");
            DataSchemaEntry<SimpleDataSchema> horizon = schema.findDataSchemaEntry("input.forecastrequest.horizon");
            DataSchemaEntry<SimpleDataSchema> forecast = schema.findDataSchemaEntry("output.forecast");

            for (Pair<DataObject, DataObject> dataPair : data.getDataCollection()) {
                DataObject input = dataPair.getValue0();
                DataObject output = dataPair.getValue1();

                DataObject inputProperties = new DataObject();
                DataObject outputProperties = new DataObject();

                for (MetamorphicRelationSetTemplate relation : relations) {
                    for (NamedExpression expression : relation.getValidations()) {
                        String propertyName = expression.name();
                        Node inputPropertyComputation = expression.expression().setVariableNameValues(Map.of(
                                "x", new DataReference(context),
                                "horizon", new DataReference(horizon)
                        ));
                        transform(inputPropertyComputation, input, propertyName, inputProperties);

                        Node outputPropertyComputation = expression.expression().setVariableNameValue("x", new DataReference(forecast));
                        transform(outputPropertyComputation, output, propertyName, outputProperties);
                    }
                }

                properties.addDataEntry(inputProperties, outputProperties);
            }

            File outDir = jsonl.getParentFile();
            File target = new File(outDir, jsonl.getName().replace(".jsonl", "-properties.jsonl"));
            properties.toJsonl(target);
        }
    }

    public static void transform(Node propertyComputation, DataObject value, String propertyName, DataObject propertyObject){
        Set<Variable> constraintVariables = propertyComputation.findInvolvedVariables();
        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(value, constraintVariables);
        for (Map<Variable, Node> valueCombination : valueCombinations) {
            Node instantiated = propertyComputation.setVariableValues(valueCombination);
            instantiated = instantiated.evaluate();
            if(instantiated instanceof Value<?> val) {
                propertyObject.putNodeValue(propertyName, val);
                return;
            }
        }

        System.err.println("Could not compute property: " + propertyName);
    }
}
