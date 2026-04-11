package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphNodeTest {

    @Test
    public void inferDataFlowsTest() {
        GraphNode client = new GraphNode("n1", "Client");
        GraphNode createOrder = new GraphNode("n2", "createOrder");
        GraphNode validateOrder = new GraphNode("n3", "validateOrder");
        GraphNode calculatePrice = new GraphNode("n4", "calculatePrice");

        SimpleDataCollection createOrderCall = SimpleDataCollection.parseData(
                """
                        {
                          "orderId": "O123",
                          "customerId": "C42",
                          "items": [
                            { "productId": "P1", "quantity": 2 },
                            { "productId": "P2", "quantity": 1 }
                          ]
                        }
                        """
        );

        SimpleDataCollection validateOrderCall = SimpleDataCollection.parseData(
                """
                        {
                          "orderRef": "O123",
                          "userId": "C42"
                        }
                        """
        );

        SimpleDataCollection calculatePriceCall = SimpleDataCollection.parseData(
                """
                        {
                          "orderId": "O123",
                          "items": [
                            { "productId": "P1", "quantity": 2 },
                            { "productId": "P2", "quantity": 1 }
                          ]
                        }
                        """
        );

        SimpleDataCollection calculatePriceResponse = SimpleDataCollection.parseData(
                """
                        {
                          "orderId": "O123",
                          "totalPrice": 300
                        }
                        """
        );

        SimpleDataCollection createOrderResponse = SimpleDataCollection.parseData(
                """
                        {
                          "orderId": "O123",
                          "status": "CONFIRMED",
                          "totalPrice": 300
                        }
                        """
        );

        SimpleDataSchema createOrderCallSchema = (SimpleDataSchema) createOrderCall.deriveSchema();
        SimpleDataSchema validateOrderCallSchema = (SimpleDataSchema) validateOrderCall.deriveSchema();
        SimpleDataSchema calculatePriceCallSchema = (SimpleDataSchema) calculatePriceCall.deriveSchema();
        SimpleDataSchema calculatePriceResponseSchema = (SimpleDataSchema) calculatePriceResponse.deriveSchema();
        SimpleDataSchema createOrderResponseSchema = (SimpleDataSchema) createOrderResponse.deriveSchema();

        CallEdge e1 = new CallEdge(client, createOrder, createOrderCallSchema);
        CallEdge e2 = new CallEdge(createOrder, validateOrder, validateOrderCallSchema);
        CallEdge e3 = new CallEdge(createOrder, calculatePrice, calculatePriceCallSchema);
        CallEdge e4 = new CallEdge(calculatePrice, createOrder, calculatePriceResponseSchema);
        CallEdge e5 = new CallEdge(createOrder, client, createOrderResponseSchema);

        DataFlow e1_e2 = createOrder.inferDataFlow(e1, createOrderCall, e2, validateOrderCall);
        DataFlow e1_e3 = createOrder.inferDataFlow(e1, createOrderCall, e3, calculatePriceCall);
        DataFlow e3_e4 = calculatePrice.inferDataFlow(e3, calculatePriceCall, e4, calculatePriceResponse);
        DataFlow e4_e5 = createOrder.inferDataFlow(e4, calculatePriceResponse, e5, createOrderResponse);

        assertMapping(e1_e2, "orderId", "orderRef");
        assertMapping(e1_e2, "customerId", "userId");

        assertMapping(e1_e3, "items", "items");
        assertMapping(e1_e3, "orderId", "orderId");

        assertMapping(e3_e4, "orderId", "orderId");

        assertMapping(e4_e5, "totalPrice", "totalPrice");
        assertMapping(e4_e5, "orderId", "orderId");

        DataFlowTreeNode expectedOrderIdFlow = new DataFlowTreeNode(client);
        DataFlowTreeNode expectedOrderIdFlowN2_1 = new DataFlowTreeNode(createOrder);
        DataFlowTreeNode expectedOrderIdFlowN3 = new DataFlowTreeNode(validateOrder);
        DataFlowTreeNode expectedOrderIdFlowN4 = new DataFlowTreeNode(calculatePrice);
        DataFlowTreeNode expectedOrderIdFlowN2_2 = new DataFlowTreeNode(createOrder);
        DataFlowTreeNode expectedOrderIdFlowN1 = new DataFlowTreeNode(client);

        expectedOrderIdFlow.addChild(e1.getSchema().findDataSchemaEntry("orderId"), expectedOrderIdFlowN2_1);
        expectedOrderIdFlowN2_1.addChild(e2.getSchema().findDataSchemaEntry("orderRef"), expectedOrderIdFlowN3);
        expectedOrderIdFlowN2_1.addChild(e3.getSchema().findDataSchemaEntry("orderId"), expectedOrderIdFlowN4);
        expectedOrderIdFlowN4.addChild(e4.getSchema().findDataSchemaEntry("orderId"), expectedOrderIdFlowN2_2);
        expectedOrderIdFlowN2_2.addChild(e5.getSchema().findDataSchemaEntry("orderId"), expectedOrderIdFlowN1);

        DataFlowTreeNode orderIdFlow = client.buildTree(e1.getSchema().findDataSchemaEntry("orderId"));

        assertThat(orderIdFlow).
                usingRecursiveComparison().
                isEqualTo(expectedOrderIdFlow);

        DataFlowTreeNode expectedCustomerIdFlow = new DataFlowTreeNode(client);
        DataFlowTreeNode expectedCustomerIdFlowN2 = new DataFlowTreeNode(createOrder);
        DataFlowTreeNode expectedCustomerIdFlowN3 = new DataFlowTreeNode(validateOrder);

        expectedCustomerIdFlow.addChild(e1.getSchema().findDataSchemaEntry("customerId"), expectedCustomerIdFlowN2);
        expectedCustomerIdFlowN2.addChild(e2.getSchema().findDataSchemaEntry("userId"), expectedCustomerIdFlowN3);

        DataFlowTreeNode customerIdFlow = client.buildTree(e1.getSchema().findDataSchemaEntry("customerId"));

        assertThat(customerIdFlow)
                .usingRecursiveComparison()
                .isEqualTo(expectedCustomerIdFlow);

        DataFlowTreeNode expectedItemsFlow = new DataFlowTreeNode(client);
        DataFlowTreeNode expectedItemsFlowN2 = new DataFlowTreeNode(createOrder);
        DataFlowTreeNode expectedItemsFlowN4 = new DataFlowTreeNode(calculatePrice);

        expectedItemsFlow.addChild(e1.getSchema().findDataSchemaEntry("items"), expectedItemsFlowN2);
        expectedItemsFlowN2.addChild(e3.getSchema().findDataSchemaEntry("items"), expectedItemsFlowN4);

        DataFlowTreeNode itemsFlow = client.buildTree(e1.getSchema().findDataSchemaEntry("items"));

        assertThat(itemsFlow)
                .usingRecursiveComparison()
                .isEqualTo(expectedItemsFlow);
    }

    private static void assertMapping(
            DataFlow flow,
            String fromPath,
            String toPath
    ) {
        boolean found = flow.getDataFlows().entrySet().stream()
                .anyMatch(e ->
                        e.getKey().getQualifiedName().equals(fromPath) && e.getValue().getQualifiedName().equals(toPath)
                );

        if (!found) {
            throw new AssertionError("Expected mapping not found: " + fromPath + " -> " + toPath);
        }
    }
}
