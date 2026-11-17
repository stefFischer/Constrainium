package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.data.TypePromotionPolicy;

import java.util.ArrayList;
import java.util.List;

public abstract class CallEdge extends Edge {

    private final List<DataFlow> dataFlows = new ArrayList<>();

    public CallEdge(GraphNode from, GraphNode to) {
        super(from, to);
    }

    public List<DataFlow> getDataFlows() {
        return dataFlows;
    }

    public List<DataFlow> getDataFlowsTo(CallEdge target) {
        return dataFlows.stream().filter(df -> df.getTo() == target).toList();
    }

    protected void addDataFlow(DataFlow dataFlow){
        this.dataFlows.add(dataFlow);
    }

    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData){
        inferDataFlows(traceId, fromData, to , toData, null);
    }

    public abstract void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData, TypePromotionPolicy typePromotionPolicy);

    public abstract DataSchema getSchema();
}
