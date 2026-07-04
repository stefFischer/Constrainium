package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.InOutputDataSchema;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraphNodeTest {

    @Test
    public void inferDataFlowsTest() {
        GraphNode client = new GraphNode("n1", "Client");
        GraphNode createOrder = new GraphNode("n2", "createOrder");
        GraphNode validateOrder = new GraphNode("n3", "validateOrder");
        GraphNode calculatePrice = new GraphNode("n4", "calculatePrice");

        Graph graph = new Graph();
        graph.addEntryNode(client);
        createOrder = graph.getOrCreateNode(createOrder);
        validateOrder = graph.getOrCreateNode(validateOrder);
        calculatePrice = graph.getOrCreateNode(calculatePrice);

        @SuppressWarnings("unchecked")
        InOutputDataCollection createOrderData = InOutputDataCollection.parseData(
                new Pair<>(
                    """
                    {
                      "orderId": "O123",
                      "customerId": "C42",
                      "items": [
                        { "productId": "P1", "quantity": 2 },
                        { "productId": "P2", "quantity": 1 }
                      ]
                    }
                    """,
                    """
                    {
                      "orderId": "O123",
                      "status": "CONFIRMED",
                      "totalPrice": 300
                    }
                    """
                )
        );

        SimpleDataCollection validateOrderCall = SimpleDataCollection.parseData(
                """
                        {
                          "orderRef": "O123",
                          "userId": "C42"
                        }
                        """
        );

        @SuppressWarnings("unchecked")
        InOutputDataCollection calculatePriceData = InOutputDataCollection.parseData(
                new Pair<>(
                        """
                        {
                          "orderId": "O123",
                          "items": [
                            { "productId": "P1", "quantity": 2 },
                            { "productId": "P2", "quantity": 1 }
                          ]
                        }
                        """,
                        """
                        {
                          "orderId": "O123",
                          "totalPrice": 300
                        }
                        """
                )
        );

        InOutputDataSchema<SimpleDataSchema> createOrderSchema = createOrderData.deriveSchema(null);
        SimpleDataSchema validateOrderCallSchema = (SimpleDataSchema) validateOrderCall.deriveSchema();
        InOutputDataSchema<SimpleDataSchema> calculatePriceSchema = createOrderData.deriveSchema(null);

        CallEdge clientEdge = new SynchronousCallEdge(client, createOrder, createOrderSchema);
        CallEdge validateEdge = new AsynchronousCallEdge(createOrder, validateOrder, validateOrderCallSchema);
        CallEdge calcPriceEdge = new SynchronousCallEdge(createOrder, calculatePrice, calculatePriceSchema);

        clientEdge.inferDataFlows("1", createOrderData, validateEdge, validateOrderCall);
        clientEdge.inferDataFlows("1", createOrderData, calcPriceEdge, calculatePriceData);

        DataFlow e1_e2 = clientEdge.getDataFlowsTo(validateEdge).getFirst();
        DataFlow e1_e3 = clientEdge.getDataFlowsTo(calcPriceEdge).getFirst();
        DataFlow e4_e5 = calcPriceEdge.getDataFlowsTo(clientEdge).getFirst();

        assertMapping(e1_e2, "input.orderId", "orderRef");
        assertMapping(e1_e2, "input.customerId", "userId");

        assertMapping(e1_e3, "input.items", "input.items");
        assertMapping(e1_e3, "input.orderId", "input.orderId");

        assertMapping(e4_e5, "output.totalPrice", "output.totalPrice");
        assertMapping(e4_e5, "output.orderId", "output.orderId");


        DataPaths orderIdFlow = graph.deriveDataPaths("input.orderId");

        assertEquals(1, orderIdFlow.getRoots().size());

        DataPathNode root = orderIdFlow.getRoots().getFirst();
        assertSame(clientEdge, root.getEdge());
        assertEquals("input.orderId", root.getEntry().getQualifiedName());

        assertEquals(2, root.getNext().size());

        DataPathNode validate = root.getNext().stream()
                .filter(n -> n.getEdge() == validateEdge)
                .findFirst()
                .orElseThrow();

        assertEquals("orderRef", validate.getEntry().getQualifiedName());
        assertTrue(validate.getNext().isEmpty());

        DataPathNode calculate = root.getNext().stream()
                .filter(n -> n.getEdge() == calcPriceEdge)
                .findFirst()
                .orElseThrow();

        assertEquals("input.orderId", calculate.getEntry().getQualifiedName());
        assertTrue(calculate.getNext().isEmpty());


        DataPaths customerIdFlow = graph.deriveDataPaths("input.customerId");

        assertEquals(1, customerIdFlow.getRoots().size());

        root = customerIdFlow.getRoots().getFirst();

        assertSame(clientEdge, root.getEdge());
        assertEquals("input.customerId", root.getEntry().getQualifiedName());

        assertEquals(1, root.getNext().size());

        DataPathNode child = root.getNext().getFirst();
        assertSame(validateEdge, child.getEdge());
        assertEquals("userId", child.getEntry().getQualifiedName());
        assertTrue(child.getNext().isEmpty());


        DataPaths itemsFlow = graph.deriveDataPaths("input.items");

        assertEquals(1, itemsFlow.getRoots().size());

        root = itemsFlow.getRoots().getFirst();

        assertSame(clientEdge, root.getEdge());
        assertEquals("input.items", root.getEntry().getQualifiedName());

        assertEquals(1, root.getNext().size());

        child = root.getNext().getFirst();
        assertSame(calcPriceEdge, child.getEdge());
        assertEquals("input.items", child.getEntry().getQualifiedName());
        assertTrue(child.getNext().isEmpty());
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
