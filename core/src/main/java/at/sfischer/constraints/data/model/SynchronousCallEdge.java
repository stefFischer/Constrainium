package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.*;

public class SynchronousCallEdge extends CallEdge {

    private InOutputDataSchema<SimpleDataSchema> schema;

    public SynchronousCallEdge(GraphNode from, GraphNode to) {
        super(from, to);
    }

    public SynchronousCallEdge(GraphNode from, GraphNode to, InOutputDataSchema<SimpleDataSchema> schema) {
        super(from, to);
        this.schema = schema;
    }

    @Override
    public InOutputDataSchema<SimpleDataSchema> getSchema() {
        if (schema == null) {
            throw new IllegalStateException("Finalize the graph before reading its schemas.");
        }

        return schema;
    }

    @Override
    protected void deriveSchema(TypePromotionPolicy policy) {
        checkMutable();
        if (hasData()) {
            schema = collectedData().deriveSchema(policy);
        } else if (schema == null) {
            schema = new InOutputDataSchema<>(new SimpleDataSchema(), new SimpleDataSchema());
        }
    }

    @Override
    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData, TypePromotionPolicy typePromotionPolicy) {
        checkMutable();
        to.checkMutable();
        // An earlier call's response is available to a later call made by the same node.
        if (getFrom() == to.getFrom()) {
            if (fromData instanceof InOutputDataCollection sourceData) {
                InOutputDataCollection responseToRequest = InOutputDataCollection.createFromSimpleCollections(
                        sourceData.getOutputDataCollection(), getData(toData)
                );
                DataFlow dataFlow = new DataFlow(traceId, this, to);
                dataFlow.inferDataFlows(responseToRequest, schema.getOutputSchema(), getSchema(to.getSchema()), typePromotionPolicy);
                addDataFlow(dataFlow);
            }

            return;
        }

        if(to instanceof SynchronousCallEdge){
            if(fromData instanceof InOutputDataCollection && toData instanceof InOutputDataCollection){
                InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(
                        ((InOutputDataCollection) toData).getOutputDataCollection(),
                        ((InOutputDataCollection) fromData).getOutputDataCollection()
                );
                DataFlow dataFlow = new DataFlow(traceId, to, this);
                dataFlow.inferDataFlows(inOut, ((SynchronousCallEdge) to).getSchema().getOutputSchema(), this.schema.getOutputSchema(), typePromotionPolicy);
                to.addDataFlow(dataFlow);
            }
        }

        InOutputDataCollection inOut = InOutputDataCollection.createFromSimpleCollections(getData(fromData), getData(toData));
        DataFlow dataFlow = new DataFlow(traceId, this, to);
        dataFlow.inferDataFlows(inOut, schema.getInputSchema(), getSchema(to.getSchema()), typePromotionPolicy);
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
