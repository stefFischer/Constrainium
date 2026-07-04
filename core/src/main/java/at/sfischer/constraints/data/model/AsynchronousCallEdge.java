package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.*;

public class AsynchronousCallEdge extends CallEdge {

    private final SimpleDataSchema schema;

    public AsynchronousCallEdge(GraphNode from, GraphNode to, SimpleDataSchema schema) {
        super(from, to);
        this.schema = schema;
    }

    @Override
    public SimpleDataSchema getSchema() {
        return schema;
    }

    @Override
    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData) {
        InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(getData(fromData), getData(toData));
        DataFlow dataFlow = new DataFlow(traceId, this, to);
        dataFlow.inferDataFlows(inOut, schema, getSchema(to.getSchema()));
        this.addDataFlow(dataFlow);
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
