package at.scch.traceon.otel.data;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;

public class RedisSpanDataFinder extends AbstractRedisSpanDataFinder {

    public RedisSpanDataFinder(DataCollection<DataObject> data) {
        super(data);
    }

    @Override
    protected String direction() {
        return "REQUEST";
    }
}
