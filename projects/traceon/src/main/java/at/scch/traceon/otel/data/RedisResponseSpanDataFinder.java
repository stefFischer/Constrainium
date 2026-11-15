package at.scch.traceon.otel.data;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;

public class RedisResponseSpanDataFinder extends AbstractRedisSpanDataFinder {

    public RedisResponseSpanDataFinder(DataCollection<DataObject> data) {
        super(data);
    }

    @Override
    protected String direction() {
        return "RESPONSE";
    }
}
