package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.data.TypePromotionPolicy;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.DataObject;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public abstract class CallEdge extends Edge {

    private final List<DataFlow> dataFlows = new ArrayList<>();
    private final Map<String, InOutputDataCollection> dataByTrace = new LinkedHashMap<>();

    public CallEdge(GraphNode from, GraphNode to) {
        super(from, to);
    }

    public List<DataFlow> getDataFlows() {
        return Collections.unmodifiableList(dataFlows);
    }

    public List<DataFlow> getDataFlowsTo(CallEdge target) {
        return dataFlows.stream().filter(df -> df.getTo() == target).toList();
    }

    public void addDataFlow(DataFlow dataFlow){
        checkMutable();
        this.dataFlows.add(dataFlow);
    }

    /** Appends paired rows and returns their starting index within this trace's collection. */
    public int addData(String traceId, InOutputDataCollection data) {
        checkMutable();
        Objects.requireNonNull(traceId, "traceId");
        InOutputDataCollection stored = dataByTrace.get(traceId);
        int start = stored == null ? 0 : stored.size();
        if (data.size() == 0) {
            return start;
        }

        if (stored == null) {
            stored = new InOutputDataCollection();
            dataByTrace.put(traceId, stored);
        }
        appendCopy(data, stored);
        return start;
    }

    public int addData(String traceId, DataObject input, DataObject output) {
        InOutputDataCollection row = new InOutputDataCollection();
        row.addDataEntry(input == null ? new DataObject() : input, output == null ? new DataObject() : output);
        return addData(traceId, row);
    }

    /** Returns defensive copies; rows in the stored collections are append-only until finalization. */
    public Map<String, InOutputDataCollection> getDataByTrace() {
        Map<String, InOutputDataCollection> copy = new LinkedHashMap<>();
        for (Map.Entry<String, InOutputDataCollection> entry : dataByTrace.entrySet()) {
            InOutputDataCollection data = new InOutputDataCollection();
            appendCopy(entry.getValue(), data);
            copy.put(entry.getKey(), data);
        }

        return Collections.unmodifiableMap(copy);
    }

    public boolean hasData() {
        return !dataByTrace.isEmpty();
    }

    public Set<String> getTraceIds() {
        return Set.copyOf(dataByTrace.keySet());
    }

    private static void appendCopy(InOutputDataCollection source, InOutputDataCollection target) {
        for (Pair<DataObject, DataObject> row : source.getDataCollection()) {
            target.addDataEntry(InOutputDataCollection.getInputData(row).clone(),
                    InOutputDataCollection.getOutputData(row).clone());
        }
    }

    Pair<DataObject, DataObject> dataRow(String traceId, int index) {
        InOutputDataCollection data = dataByTrace.get(traceId);
        if (data == null || index < 0 || index >= data.size()) {
            throw new IllegalArgumentException("No data row " + index + " for trace '" + traceId + "'.");
        }

        return data.getDataCollection().get(index);
    }

    void appendRowTo(String traceId, int index, InOutputDataCollection target) {
        Pair<DataObject, DataObject> row = dataRow(traceId, index);
        target.addDataEntry(InOutputDataCollection.getInputData(row), InOutputDataCollection.getOutputData(row));
    }

    protected InOutputDataCollection collectedData() {
        InOutputDataCollection data = new InOutputDataCollection();
        for (InOutputDataCollection trace : dataByTrace.values()) {
            data.getDataCollection().addAll(trace.getDataCollection());
        }

        return data;
    }

    protected abstract void deriveSchema(TypePromotionPolicy policy);

    void clearDataFlows() {
        dataFlows.clear();
    }

    void discardData() {
        dataByTrace.clear();
    }

    @Override
    void freeze() {
        for (DataFlow flow : dataFlows) {
            flow.freeze();
        }
        super.freeze();
    }

    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData){
        inferDataFlows(traceId, fromData, to , toData, null);
    }

    public void inferDataFlows(String traceId, DataCollection<?> fromData, CallEdge to, DataCollection<?> toData,
                               TypePromotionPolicy typePromotionPolicy) {
        checkMutable();
        to.checkMutable();
        for (DataFlow flow : deriveDataFlows(traceId, fromData, to, toData, typePromotionPolicy)) {
            flow.getFrom().addDataFlow(flow);
        }
    }

    void inferDataFlows(List<CallRelation> comparableCalls, TypePromotionPolicy policy) {
        checkMutable();
        CallRelation first = comparableCalls.getFirst();
        CallEdge target = first.target();
        target.checkMutable();
        InOutputDataCollection sourceData = new InOutputDataCollection();
        InOutputDataCollection targetData = new InOutputDataCollection();
        for (CallRelation relation : comparableCalls) {
            appendRowTo(relation.traceId(), relation.sourceRow(), sourceData);
            target.appendRowTo(relation.traceId(), relation.targetRow(), targetData);
        }

        List<DataFlow> accepted = deriveDataFlows(first.traceId(), sourceData, target, targetData, policy);
        for (DataFlow flow : accepted) {
            for (CallRelation relation : comparableCalls) {
                flow.getFrom().addDataFlow(flow.forOccurrence(relation));
            }
        }
    }

    protected abstract List<DataFlow> deriveDataFlows(String traceId, DataCollection<?> fromData, CallEdge to,
                                                     DataCollection<?> toData, TypePromotionPolicy policy);

    public abstract DataSchema getSchema();
}
