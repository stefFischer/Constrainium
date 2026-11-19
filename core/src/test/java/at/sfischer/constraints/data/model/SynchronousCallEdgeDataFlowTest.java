package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.DefaultTypePromotionPolicy;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronousCallEdgeDataFlowTest {

    @Test
    void siblingCallsUseTheResponseInsteadOfTheEarlierRequest() {
        GraphNode caller = new GraphNode("caller", "caller");
        InOutputDataCollection sourceData = sample("{requestId:'STRW-001'}", "{returnedId:'HAY-002'}");
        InOutputDataCollection targetData = sample("{selectedId:'HAY-002',otherId:'STRW-001'}", "{}");
        SynchronousCallEdge source = synchronous(caller, "source", sourceData);
        SynchronousCallEdge target = synchronous(caller, "target", targetData);

        source.inferDataFlows("trace", sourceData, target, targetData, new DefaultTypePromotionPolicy());

        assertTrue(hasMapping(source, target,
                source.getSchema().getOutputSchema().findDataSchemaEntry("returnedId"),
                target.getSchema().getInputSchema().findDataSchemaEntry("selectedId")));
        assertFalse(hasMapping(source, target,
                source.getSchema().getInputSchema().findDataSchemaEntry("requestId"),
                target.getSchema().getInputSchema().findDataSchemaEntry("otherId")));
        assertTrue(target.getDataFlowsTo(source).isEmpty());
    }

    @Test
    void nestedCallsForwardRequestsAndPropagateResponsesBackToTheParent() {
        GraphNode caller = new GraphNode("caller", "caller");
        GraphNode service = new GraphNode("service", "service");
        InOutputDataCollection parentData = sample("{customerId:'C42'}", "{receiptNumber:'ORDER-1'}");
        InOutputDataCollection childData = sample("{buyer:'C42'}", "{orderId:'ORDER-1'}");
        SynchronousCallEdge parent = new SynchronousCallEdge(caller, service,
                parentData.deriveSchema(new DefaultTypePromotionPolicy()));
        SynchronousCallEdge child = synchronous(service, "database", childData);

        parent.inferDataFlows("trace", parentData, child, childData, new DefaultTypePromotionPolicy());

        assertTrue(hasMapping(parent, child,
                parent.getSchema().getInputSchema().findDataSchemaEntry("customerId"),
                child.getSchema().getInputSchema().findDataSchemaEntry("buyer")));
        assertTrue(hasMapping(child, parent,
                child.getSchema().getOutputSchema().findDataSchemaEntry("orderId"),
                parent.getSchema().getOutputSchema().findDataSchemaEntry("receiptNumber")));
    }

    @Test
    void aResponseCanSupplyThePayloadOfAnAsynchronousSiblingCall() {
        GraphNode caller = new GraphNode("caller", "caller");
        InOutputDataCollection sourceData = sample("{}", "{createdOrderId:'ORDER-1'}");
        SimpleDataCollection messageData = SimpleDataCollection.parseData("{orderId:'ORDER-1'}");
        SynchronousCallEdge source = synchronous(caller, "database", sourceData);
        AsynchronousCallEdge target = new AsynchronousCallEdge(caller,
                new GraphNode("topic", "topic"), messageData.deriveSchema(new DefaultTypePromotionPolicy()));

        source.inferDataFlows("trace", sourceData, target, messageData, new DefaultTypePromotionPolicy());

        assertTrue(hasMapping(source, target,
                source.getSchema().getOutputSchema().findDataSchemaEntry("createdOrderId"),
                target.getSchema().findDataSchemaEntry("orderId")));
    }

    @Test
    void resolvesTheNestedFieldInsideAProjectedArrayElement() {
        GraphNode caller = new GraphNode("caller", "caller");
        InOutputDataCollection sourceData = sample("{}",
                "{items:[{details:{productId:'OTHER'}},{details:{productId:'HAY-002'}}]}");
        InOutputDataCollection targetData = sample("{selectedId:'HAY-002'}", "{}");
        SynchronousCallEdge source = synchronous(caller, "source", sourceData);
        SynchronousCallEdge target = synchronous(caller, "target", targetData);

        source.inferDataFlows("trace", sourceData, target, targetData, new DefaultTypePromotionPolicy());

        assertTrue(hasMapping(source, target,
                source.getSchema().getOutputSchema().findDataSchemaEntry("items.details.productId"),
                target.getSchema().getInputSchema().findDataSchemaEntry("selectedId")));
        assertFalse(source.getDataFlowsTo(target).stream().anyMatch(flow ->
                flow.getDataFlows().containsKey(source.getSchema().getOutputSchema().findDataSchemaEntry("items"))));
    }

    @Test
    void rejectsMembershipWhenAnotherPairedSampleDoesNotMatch() {
        GraphNode caller = new GraphNode("caller", "caller");
        InOutputDataCollection sourceData = sample("{}", "{items:[{id:'OTHER'},{id:'HAY-002'}]}");
        sourceData.addDataEntry(DataObject.parseData("{}"), DataObject.parseData("{items:[{id:'STRW-001'}]}"));
        InOutputDataCollection targetData = sample("{selectedId:'HAY-002'}", "{}");
        targetData.addDataEntry(DataObject.parseData("{selectedId:'HAY-002'}"), DataObject.parseData("{}"));
        SynchronousCallEdge source = synchronous(caller, "source", sourceData);
        SynchronousCallEdge target = synchronous(caller, "target", targetData);

        source.inferDataFlows("trace", sourceData, target, targetData, new DefaultTypePromotionPolicy());

        assertFalse(hasMapping(source, target,
                source.getSchema().getOutputSchema().findDataSchemaEntry("items.id"),
                target.getSchema().getInputSchema().findDataSchemaEntry("selectedId")));
    }

    private static InOutputDataCollection sample(String input, String output) {
        InOutputDataCollection data = new InOutputDataCollection();
        data.addDataEntry(DataObject.parseData(input), DataObject.parseData(output));
        return data;
    }

    private static SynchronousCallEdge synchronous(GraphNode caller, String destination, InOutputDataCollection data) {
        return new SynchronousCallEdge(caller, new GraphNode(destination, destination),
                data.deriveSchema(new DefaultTypePromotionPolicy()));
    }

    private static boolean hasMapping(CallEdge source, CallEdge target,
                                      DataSchemaEntry<SimpleDataSchema> sourceField,
                                      DataSchemaEntry<SimpleDataSchema> targetField) {
        return source.getDataFlowsTo(target).stream()
                .anyMatch(flow -> flow.getDataFlows().get(sourceField) == targetField);
    }
}
