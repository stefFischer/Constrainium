import at.sfischer.constraints.data.*;
import at.sfischer.constraints.data.model.*;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.TypeEnum;
import java.nio.file.Path;

/** Generate the checked-in sample using the repository's actual exporter. */
public class GenerateSample {
    public static void main(String[] args) throws Exception {
        Graph g = new Graph();
        GraphNode gateway = g.getOrCreateNode(new GraphNode("gateway", "API Gateway"));
        GraphNode orders = g.getOrCreateNode(new GraphNode("orders", "Order Service"));
        GraphNode inventory = g.getOrCreateNode(new GraphNode("inventory", "Inventory"));
        GraphNode database = g.getOrCreateNode(new GraphNode("database", "Orders Database"));
        GraphNode events = g.getOrCreateNode(new GraphNode("events", "Event Worker"));
        g.getOrCreateNode(new GraphNode("isolated", "Disconnected Service"));
        g.addEntryNode(gateway);

        SimpleDataSchema request = new SimpleDataSchema(), response = new SimpleDataSchema();
        var requestId = request.stringEntry("id", true);
        var products = request.objectArrayEntry("products", true);
        var sku = products.dataSchema.stringEntry("sku", true);
        products.dataSchema.integerEntry("quantity", true);
        products.dataSchema.arrayEntryFor(new ArrayType(TypeEnum.INTEGER), "matrix", false);
        var responseId = response.stringEntry("id", true);
        response.stringEntry("status", true);
        var call = new SynchronousCallEdge(gateway, orders, new InOutputDataSchema<>(request, response, "request", "response"));

        SimpleDataSchema stockRequest = new SimpleDataSchema(), stockResponse = new SimpleDataSchema();
        var stockSku = stockRequest.stringEntry("sku", true);
        stockResponse.booleanEntry("available", true);
        var stock = new SynchronousCallEdge(orders, inventory, new InOutputDataSchema<>(stockRequest, stockResponse));

        SimpleDataSchema queryInput = new SimpleDataSchema(), queryOutput = new SimpleDataSchema();
        var queryId = queryInput.stringEntry("id", true);
        var rowId = queryOutput.objectArrayEntry("rows", false).dataSchema.stringEntry("id", true);
        var query = new DatabaseQueryEdge(orders, database, new InOutputDataSchema<>(queryInput, queryOutput), "SELECT id FROM orders WHERE id = ?");
        // Parallel edge with intentionally absent schema.
        new SynchronousCallEdge(gateway, orders, null);
        SimpleDataSchema eventSchema = new SimpleDataSchema();
        var eventId = eventSchema.stringEntry("id", true);
        var event = new AsynchronousCallEdge(orders, events, eventSchema);
        new DependencyEdge(events, gateway);
        new DependencyEdge(events, events);

        flow("trace-order-001", call, stock, sku, stockSku);
        flow("trace-order-001", call, query, requestId, queryId);
        flow("trace-order-001", query, call, rowId, responseId);
        flow("trace-order-001", call, event, responseId, eventId);
        // Explicit field-flow cycle, independent of service adjacency.
        flow("trace-cycle-002", event, call, eventId, responseId);
        GraphJsonExporter.write(g, Path.of(args[0]));
    }
    private static void flow(String trace, CallEdge from, CallEdge to,
                             DataSchemaEntry<SimpleDataSchema> source, DataSchemaEntry<SimpleDataSchema> target) {
        DataFlow flow = new DataFlow(trace, from, to);
        flow.getDataFlows().put(source, target);
        from.getDataFlows().add(flow);
    }
}
