package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.util.Collection;
import java.util.List;

public class AsynchronousCallEdge extends CallEdge {

    private SimpleDataSchema schema;

    public AsynchronousCallEdge(GraphNode from, GraphNode to) {
        super(from, to);
    }

    public AsynchronousCallEdge(GraphNode from, GraphNode to, SimpleDataSchema schema) {
        super(from, to);
        this.schema = schema;
    }

    @Override
    public SimpleDataSchema getSchema() {
        if (schema == null) {
            throw new IllegalStateException("Finalize the graph before reading its schemas.");
        }

        return schema;
    }

    @Override
    protected void deriveSchema(TypePromotionPolicy policy) {
        checkMutable();
        if (hasData()) {
            schema = collectedData().getInputDataCollection().deriveSchema(policy);
        } else if (schema == null) {
            schema = new SimpleDataSchema();
        }
    }

    @Override
    protected List<DataFlow> deriveDataFlows(String traceId, DataCollection<?> fromData, CallEdge to,
                                             DataCollection<?> toData, TypePromotionPolicy typePromotionPolicy,
                                             Collection<? extends Node> additionalTerms,
                                             ConstraintPolicy retentionPolicy) {
        InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(getData(fromData), getData(toData));
        DataFlow dataFlow = new DataFlow(traceId, this, to);
        dataFlow.inferDataFlows(inOut, schema, getSchema(to.getSchema()), typePromotionPolicy,
                additionalTerms, retentionPolicy);
        return List.of(dataFlow);
    }

    private SimpleDataCollection getData(DataCollection<?> data){
        if(data instanceof SimpleDataCollection){
            return (SimpleDataCollection) data;
        }

        if(data instanceof InOutputDataCollection){
            return ((InOutputDataCollection) data).getInputDataCollection();
        }

        throw new IllegalArgumentException("Data type " + data.getClass().getCanonicalName() + " is not supported.");
    }

    private SimpleDataSchema getSchema(DataSchema schema){
        if(schema instanceof SimpleDataSchema){
            return (SimpleDataSchema) schema;
        }

        if(schema instanceof InOutputDataSchema){
            return getSchema(((InOutputDataSchema<?>) schema).getInputSchema());
        }

        throw new IllegalArgumentException("Data type " + schema.getClass().getCanonicalName() + " is not supported.");
    }
}
