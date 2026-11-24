package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.operators.strings.SubString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DataFlowAdditionalTermsTest {

    @Test
    void additionalTermsSupplementDefaultsWithoutChangingTheTemplate() {
        InOutputDataCollection observations = new InOutputDataCollection();
        observations.addDataEntry(DataObject.parseData("{encoded:'prefix:A1',stable:'unchanged'}"),
                DataObject.parseData("{id:'A1',copy:'unchanged'}"));
        observations.addDataEntry(DataObject.parseData("{encoded:'prefix:B2',stable:'unchanged'}"),
                DataObject.parseData("{id:'B2',copy:'unchanged'}"));
        SimpleDataSchema source = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema target = observations.getOutputDataCollection().deriveSchema(null);
        DataFlow defaults = flow();
        defaults.inferDataFlows(observations, source, target, null);
        DataFlow emptyTerms = flow();
        emptyTerms.inferDataFlows(observations, source, target, null, List.of());
        assertEquals(defaults.getDataFlows(), emptyTerms.getDataFlows());
        assertFalse(defaults.getDataFlows().containsKey(source.findDataSchemaEntry("encoded")));

        Node template = new SubString(new Variable("left"), new Variable("right"));
        String original = template.nodeString();
        DataFlow extended = flow();
        extended.inferDataFlows(observations, source, target, null, List.of(template));

        assertMapping(extended, source, "encoded", target, "id");
        // SubString excludes equality, so this mapping must still come from the built-in terms.
        assertMapping(extended, source, "stable", target, "copy");
        assertEquals(original, template.nodeString());
        assertEquals(Set.of(new Variable("left"), new Variable("right")), template.findInvolvedVariables());
        DataFlow laterDefaults = flow();
        laterDefaults.inferDataFlows(observations, source, target, null);
        assertEquals(defaults.getDataFlows(), laterDefaults.getDataFlows());
    }

    @Test
    void composedTermsMatchFieldsOfDifferentTypes() {
        InOutputDataCollection observations = new InOutputDataCollection();
        observations.addDataEntry(DataObject.parseData("{text:'abc'}"), DataObject.parseData("{size:3}"));
        observations.addDataEntry(DataObject.parseData("{text:'hello'}"), DataObject.parseData("{size:5}"));
        SimpleDataSchema source = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema target = observations.getOutputDataCollection().deriveSchema(null);
        Node template = new EqualOperator(new StringLength(new Variable("text")), new Variable("length"));
        DataFlow flow = flow();

        flow.inferDataFlows(observations, source, target, null, List.of(template));

        assertMapping(flow, source, "text", target, "size");
    }

    @Test
    void aCounterexampleRejectsAnAdditionalTermWithoutRejectingDefaultMatches() {
        InOutputDataCollection observations = new InOutputDataCollection();
        observations.addDataEntry(DataObject.parseData("{encoded:'prefix:A1',stable:'unchanged'}"),
                DataObject.parseData("{id:'A1',copy:'unchanged'}"));
        observations.addDataEntry(DataObject.parseData("{encoded:'prefix:B2',stable:'unchanged'}"),
                DataObject.parseData("{id:'OTHER',copy:'unchanged'}"));
        SimpleDataSchema source = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema target = observations.getOutputDataCollection().deriveSchema(null);
        DataFlow flow = flow();

        flow.inferDataFlows(observations, source, target, null,
                List.of(new SubString(new Variable("a"), new Variable("b"))));

        assertFalse(flow.getDataFlows().containsKey(source.findDataSchemaEntry("encoded")));
        assertMapping(flow, source, "stable", target, "copy");
    }

    @Test
    void additionalMembershipTermsResolveNestedFieldsInMultipleArrayElements() {
        InOutputDataCollection observations = new InOutputDataCollection();
        observations.addDataEntry(DataObject.parseData(
                        "{items:[{details:{encoded:'prefix:A1'}},{details:{encoded:'prefix:B2'}}]}"),
                DataObject.parseData("{selected:'B2'}"));
        observations.addDataEntry(DataObject.parseData(
                        "{items:[{details:{encoded:'prefix:C3'}},{details:{encoded:'prefix:D4'}}]}"),
                DataObject.parseData("{selected:'C3'}"));
        SimpleDataSchema source = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema target = observations.getOutputDataCollection().deriveSchema(null);
        Node template = new Exists(new Variable("values"),
                new SubString(new Variable(ArrayOperation.ELEMENT_NAME), new Variable("candidate")));
        String original = template.nodeString();
        DataFlow defaults = flow();
        defaults.inferDataFlows(observations, source, target, null);
        assertFalse(defaults.getDataFlows().containsKey(source.findDataSchemaEntry("items.details.encoded")));

        DataFlow extended = flow();
        extended.inferDataFlows(observations, source, target, null, List.of(template));

        assertMapping(extended, source, "items.details.encoded", target, "selected");
        assertFalse(extended.getDataFlows().containsKey(source.findDataSchemaEntry("items")));
        assertEquals(original, template.nodeString());
    }

    private static DataFlow flow() {
        GraphNode first = new GraphNode("first", "first");
        GraphNode second = new GraphNode("second", "second");
        GraphNode third = new GraphNode("third", "third");
        return new DataFlow("trace", new AsynchronousCallEdge(first, second),
                new AsynchronousCallEdge(second, third));
    }

    private static void assertMapping(DataFlow flow, SimpleDataSchema source, String sourcePath,
                                      SimpleDataSchema target, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> sourceField = source.findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> targetField = target.findDataSchemaEntry(targetPath);
        assertNotNull(sourceField);
        assertNotNull(targetField);
        assertSame(targetField, flow.getDataFlows().get(sourceField));
    }
}
