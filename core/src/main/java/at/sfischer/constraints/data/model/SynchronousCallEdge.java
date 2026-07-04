package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.*;

public class SynchronousCallEdge extends CallEdge {

    private final InOutputDataSchema<SimpleDataSchema> schema;

    public SynchronousCallEdge(GraphNode from, GraphNode to, InOutputDataSchema<SimpleDataSchema> schema) {
        super(from, to);
        this.schema = schema;
    }

    @Override
    public InOutputDataSchema<SimpleDataSchema> getSchema() {
        return schema;
    }

    @Override
    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData) {
        if(to instanceof SynchronousCallEdge){
            if(fromData instanceof InOutputDataCollection && toData instanceof InOutputDataCollection){
                InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(
                        ((InOutputDataCollection) fromData).getOutputDataCollection(),
                        ((InOutputDataCollection) toData).getOutputDataCollection()
                );
                DataFlow dataFlow = new DataFlow(traceId, to, this);
                dataFlow.inferDataFlows(inOut, ((SynchronousCallEdge) to).getSchema().getOutputSchema(), this.schema.getOutputSchema());
                to.addDataFlow(dataFlow);
            }
        }

        InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(getData(fromData), getData(toData));
        DataFlow dataFlow = new DataFlow(traceId, this, to);
        dataFlow.inferDataFlows(inOut, schema.getInputSchema(), getSchema(to.getSchema()));
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
