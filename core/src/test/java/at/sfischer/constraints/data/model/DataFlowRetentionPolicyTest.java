package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.miner.MinApplicationsPolicy;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.strings.SubString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFlowRetentionPolicyTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void customPolicyCanAcceptCounterexamplesForBuiltInAndAdditionalTerms(boolean additionalMatchingTerm) {
        InOutputDataCollection observations = new InOutputDataCollection();
        String prefix = additionalMatchingTerm ? "prefix:" : "";
        observations.addDataEntry(DataObject.parseData("{value:'" + prefix + "A1'}"),
                DataObject.parseData("{id:'A1'}"));
        observations.addDataEntry(DataObject.parseData("{value:'" + prefix + "B2'}"),
                DataObject.parseData("{id:'B2'}"));
        observations.addDataEntry(DataObject.parseData("{value:'" + prefix + "C3'}"),
                DataObject.parseData("{id:'OTHER'}"));
        SimpleDataSchema sourceSchema = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema targetSchema = observations.getOutputDataCollection().deriveSchema(null);
        GraphNode service = new GraphNode("service", "service");
        AsynchronousCallEdge source = new AsynchronousCallEdge(new GraphNode("caller", "caller"), service, sourceSchema);
        AsynchronousCallEdge target = new AsynchronousCallEdge(service, new GraphNode("target", "target"), targetSchema);
        List<Node> terms = additionalMatchingTerm
                ? List.of(new SubString(new Variable("a"), new Variable("b"))) : List.of();
        DataFlow defaults = new DataFlow("trace", source, target);
        defaults.inferDataFlows(observations, sourceSchema, targetSchema, null, terms);
        assertTrue(defaults.getDataFlows().isEmpty());

        ConstraintPolicy retentionPolicy = new MinApplicationsPolicy(2);
        DataFlow custom = new DataFlow("trace", source, target);
        custom.inferDataFlows(observations, sourceSchema, targetSchema, null, terms, retentionPolicy);

        DataSchemaEntry<SimpleDataSchema> value = sourceSchema.findDataSchemaEntry("value");
        DataSchemaEntry<SimpleDataSchema> id = targetSchema.findDataSchemaEntry("id");
        assertNotNull(value);
        assertNotNull(id);
        assertSame(id, custom.getDataFlows().get(value));

        // The public edge entry point must forward the same policy too.
        source.inferDataFlows("trace", observations.getInputDataCollection(), target,
                observations.getOutputDataCollection(), null, terms, retentionPolicy);
        assertSame(id, source.getDataFlowsTo(target).getFirst().getDataFlows().get(value));

        DataFlow laterDefaults = new DataFlow("trace", source, target);
        laterDefaults.inferDataFlows(observations, sourceSchema, targetSchema, null, terms);
        assertTrue(laterDefaults.getDataFlows().isEmpty());
    }

    @Test
    void customPolicyCanRejectAMatchThatTheDefaultWouldKeep() {
        InOutputDataCollection observations = new InOutputDataCollection();
        observations.addDataEntry(DataObject.parseData("{value:'A1'}"), DataObject.parseData("{id:'A1'}"));
        SimpleDataSchema sourceSchema = observations.getInputDataCollection().deriveSchema(null);
        SimpleDataSchema targetSchema = observations.getOutputDataCollection().deriveSchema(null);
        GraphNode service = new GraphNode("service", "service");
        AsynchronousCallEdge source = new AsynchronousCallEdge(new GraphNode("caller", "caller"), service, sourceSchema);
        AsynchronousCallEdge target = new AsynchronousCallEdge(service, new GraphNode("target", "target"), targetSchema);
        DataFlow defaults = new DataFlow("trace", source, target);
        defaults.inferDataFlows(observations, sourceSchema, targetSchema, null);
        assertFalse(defaults.getDataFlows().isEmpty());

        DataFlow custom = new DataFlow("trace", source, target);
        custom.inferDataFlows(observations, sourceSchema, targetSchema, null, List.of(), new MinApplicationsPolicy(2));

        assertTrue(custom.getDataFlows().isEmpty());
    }
}
