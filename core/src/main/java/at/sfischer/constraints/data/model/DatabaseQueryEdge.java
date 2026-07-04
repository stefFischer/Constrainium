package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.InOutputDataSchema;
import at.sfischer.constraints.data.SimpleDataSchema;

public class DatabaseQueryEdge extends SynchronousCallEdge {

    private final String query;

    public DatabaseQueryEdge(GraphNode from, GraphNode to, InOutputDataSchema<SimpleDataSchema> schema, String query) {
        super(from, to, schema);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public static DatabaseQueryEdge findQueryEdge(GraphNode from, String query){
        for (Edge edge : from.getOutgoing()) {
            if(!(edge instanceof DatabaseQueryEdge dbEdge)){
                continue;
            }

            if(dbEdge.query.equals(query)){
                return dbEdge;
            }
        }

        return null;
    }
}
