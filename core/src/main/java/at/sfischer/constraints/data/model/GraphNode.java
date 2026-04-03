package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphNode {
    private final String id;
    private final String name;

    private final List<Edge> incoming = new ArrayList<>();
    private final List<Edge> outgoing = new ArrayList<>();

    private final List<DataFlow> dataFlows = new ArrayList<>();

    protected GraphNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void addIncoming(Edge e) { incoming.add(e); }
    public void addOutgoing(Edge e) { outgoing.add(e); }

    public List<Edge> getIncoming() { return Collections.unmodifiableList(incoming); }
    public List<Edge> getOutgoing() { return Collections.unmodifiableList(outgoing); }

    public List<DataFlow> getDataFlows() {
        return dataFlows;
    }

    public DataFlow inferDataFlow(CallEdge from, SimpleDataCollection fromData, CallEdge to, SimpleDataCollection toData) {
        if(!incoming.contains(from)){
            throw new IllegalArgumentException("Edge from which data flow should be checked is not available in incoming edges.");
        }
        if(!outgoing.contains(to)){
            throw new IllegalArgumentException("Edge from which data flow should be checked is not available in outgoing edges.");
        }

        InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(fromData, toData);
        DataFlow dataFlow = new DataFlow(from, to);
        dataFlow.inferDataFlows(inOut);
        this.dataFlows.add(dataFlow);
        return dataFlow;
    }

    public void fillSpanningEdgeConstraints(Node term){
        for (Edge inEdge : this.incoming) {
            if(!(inEdge instanceof CallEdge inCallEdge)){
                continue;
            }

            for (Edge outEdge : this.outgoing) {
                if(!(outEdge instanceof CallEdge outCallEdge)){
                    continue;
                }

                InOutputDataSchema<SimpleDataSchema> inout = new InOutputDataSchema<>(inCallEdge.getSchema(), outCallEdge.getSchema());
                inout.fillSchemaWithConstraints(term);
            }
        }
    }

    public void fillSimpleEdgeConstraints(Node term){
        for (Edge inEdge : this.incoming) {
            if(!(inEdge instanceof CallEdge inCallEdge)){
                continue;
            }

            inCallEdge.getSchema().fillSchemaWithConstraints(term);
        }

        for (Edge outEdge : this.outgoing) {
            if(!(outEdge instanceof CallEdge outCallEdge)){
                continue;
            }

            outCallEdge.getSchema().fillSchemaWithConstraints(term);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

    public DataFlowTreeNode buildTree(DataSchemaEntry<SimpleDataSchema> startField) {
        DataFlowTreeNode root = new DataFlowTreeNode(this);
        for (DataFlow dataFlow : this.dataFlows) {
            DataSchemaEntry<SimpleDataSchema> targetField = dataFlow.getDataFlows().get(startField);
            if(targetField == null){
                continue;
            }

            GraphNode targetNode = dataFlow.getTo().getTo();
            DataFlowTreeNode targetFlow = targetNode.buildTree(targetField);
            root.addChild(targetField,  targetFlow);
        }

        if(root.getChildren().isEmpty()) {
            SimpleDataSchema rootSchema = startField.getRootSchema();
            for (Edge edge : this.outgoing) {
                if (edge instanceof CallEdge callEdge) {
                    SimpleDataSchema schema = callEdge.getSchema();
                    if (rootSchema != schema) {
                        continue;
                    }

                    DataFlowTreeNode targetFlow = edge.getTo().buildTree(startField);
                    root.addChild(startField, targetFlow);
                }
            }
        }

        return root;
    }
}