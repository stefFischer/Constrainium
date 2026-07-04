package at.sfischer.traces.otel.dataextraction.sql;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.dataextraction.StorageOperation;
import at.sfischer.traces.otel.dataextraction.StorageSpanData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import static at.sfischer.traces.otel.dataextraction.SpanAttributeExtractor.attribute;

public class SQLDataExtractorTest {

    SQLDataExtractor dataExtractor = new SQLDataExtractor(
            attribute("jdbc.sql"),
            attribute("jdbc.arguments"),
            attribute("jdbc.results")
    );

    @Test
    public void parseSelectStatementData() {
        String sql = "select c1_0.id,c1_0.user_id from cart c1_0 where c1_0.user_id=?";
        String arguments = "{\"parameters\":[\"edafaa16-f057-45a1-b4db-9884937afb6f\"]}";
        String results = "{\"results\":[{\"row\":1,\"values\":{\"user_id\":\"edafaa16-f057-45a1-b4db-9884937afb6f\",\"id\":1}}]}";

        AbstractSpan span = new AbstractSpan("SELECT cartdb.cart", "CLIENT");
        Attributes attributes = new Attributes();
        attributes.put("jdbc.sql", sql);
        attributes.put("jdbc.arguments", arguments);
        attributes.put("jdbc.results", results);
        span.putAttributes(attributes);

        StorageSpanData data = dataExtractor.extractData(span);

        assertEquals(StorageOperation.READ, data.getOperation());

        assertEquals(Set.of("cart.user_id"), data.getSelectors());

        assertTrue(data.getAffectedData().isEmpty());

        DataObject inputData = data.getInputData();
        DataObject inputCart = (DataObject) inputData.getDataValue("cart").getValue();
        assertEquals("edafaa16-f057-45a1-b4db-9884937afb6f", inputCart.getDataValue("user_id").getValue());

        DataObject outputData = data.getOutputData();
        DataObject[] rows = (DataObject[]) outputData.getDataValue("results").getValue();
        assertEquals(1, rows.length);
        DataObject cartRow = (DataObject) rows[0].getDataValue("cart").getValue();
        assertEquals(Set.of("id", "user_id"), cartRow.getFieldNames());
        assertEquals("edafaa16-f057-45a1-b4db-9884937afb6f", cartRow.getDataValue("user_id").getValue());
        assertEquals(1, cartRow.getDataValue("id").getValue());
    }

    @Test
    public void parseSelectMultipleStatementData() {
        String sql = "select p1_0.id,p1_0.name,p1_0.price from product p1_0";
        String results = "{\"results\":[{\"row\":1,\"values\":{\"price\":70.23,\"name\":\"Lightweight Wooden Bench\",\"id\":\"a121f5fb\"}},{\"row\":2,\"values\":{\"price\":9.79,\"name\":\"Incredible Rubber Clock\",\"id\":\"5642e5d5\"}},{\"row\":3,\"values\":{\"price\":41.75,\"name\":\"Incredible Marble Gloves\",\"id\":\"747f7593\"}}]}";

        AbstractSpan span = new AbstractSpan("SELECT productdb.product", "CLIENT");
        Attributes attributes = new Attributes();
        attributes.put("jdbc.sql", sql);
        attributes.put("jdbc.results", results);
        span.putAttributes(attributes);

        StorageSpanData data = dataExtractor.extractData(span);

        assertEquals(StorageOperation.READ, data.getOperation());
        assertNull(data.getInputData());
        assertTrue(data.getAffectedData().isEmpty());
        assertTrue(data.getSelectors().isEmpty());

        DataObject outputData = data.getOutputData();
        DataObject[] rows = (DataObject[]) outputData.getDataValue("results").getValue();
        assertEquals(3, rows.length);

        // row 1
        DataObject p1 = (DataObject) rows[0].getDataValue("product").getValue();
        assertEquals(Set.of("id", "name", "price"), p1.getFieldNames());
        assertEquals("a121f5fb", p1.getDataValue("id").getValue());
        assertEquals("Lightweight Wooden Bench", p1.getDataValue("name").getValue());
        assertEquals(70.23, p1.getDataValue("price").getValue());

        // row 2
        DataObject p2 = (DataObject) rows[1].getDataValue("product").getValue();
        assertEquals("5642e5d5", p2.getDataValue("id").getValue());
        assertEquals("Incredible Rubber Clock", p2.getDataValue("name").getValue());
        assertEquals(9.79, p2.getDataValue("price").getValue());

        // row 3
        DataObject p3 = (DataObject) rows[2].getDataValue("product").getValue();
        assertEquals("747f7593", p3.getDataValue("id").getValue());
        assertEquals("Incredible Marble Gloves", p3.getDataValue("name").getValue());
        assertEquals(41.75, p3.getDataValue("price").getValue());
    }

    @Test
    public void parseInsertStatementData() {
        String sql = "insert into product (name,price,id) values (?,?,?)";
        String arguments = "{\"parameters\":[\"Heavy Duty Cotton Bottle\",99.18,\"20c857ef\"]}";

        AbstractSpan span = new AbstractSpan("INSERT productdb.product", "CLIENT");
        Attributes attributes = new Attributes();
        attributes.put("jdbc.sql", sql);
        attributes.put("jdbc.arguments", arguments);
        span.putAttributes(attributes);

        StorageSpanData data = dataExtractor.extractData(span);

        assertEquals(StorageOperation.WRITE, data.getOperation());
        assertNull(data.getOutputData());
        assertTrue(data.getSelectors().isEmpty());

        assertEquals(Set.of("product.id", "product.name", "product.price"), data.getAffectedData());

        DataObject inputData = data.getInputData();
        DataObject product = (DataObject) inputData.getDataValue("product").getValue();
        assertEquals(Set.of("price", "name", "id"), product.getFieldNames());
        assertEquals(99.18, product.getDataValue("price").getValue());
        assertEquals("Heavy Duty Cotton Bottle", product.getDataValue("name").getValue());
        assertEquals("20c857ef", product.getDataValue("id").getValue());
    }

    @Test
    public void parseUpdateStatementData() {
        String sql = "update cart_item set cart_id=? where id=?";
        String arguments = "{\"parameters\":[5,6]}";

        AbstractSpan span = new AbstractSpan("UPDATE cartdb.cart_item", "CLIENT");
        Attributes attributes = new Attributes();
        attributes.put("jdbc.sql", sql);
        attributes.put("jdbc.arguments", arguments);
        span.putAttributes(attributes);

        StorageSpanData data = dataExtractor.extractData(span);

        assertEquals(StorageOperation.UPDATE, data.getOperation());
        assertNull(data.getOutputData());

        assertEquals(Set.of("cart_item.cart_id"), data.getAffectedData());
        assertEquals(Set.of("cart_item.id"), data.getSelectors());

        DataObject inputData = data.getInputData();
        DataObject cartItem = (DataObject) inputData.getDataValue("cart_item").getValue();
        assertEquals(Set.of("cart_id", "id"), cartItem.getFieldNames());
        assertEquals(5, cartItem.getDataValue("cart_id").getValue());
        assertEquals(6, cartItem.getDataValue("id").getValue());
    }

    @Test
    public void parseDeleteStatementData() {
        String sql = "delete from product where id=?";
        String arguments = "{\"parameters\":[\"87a93e3f\"]}";

        AbstractSpan span = new AbstractSpan("DELETE productdb.product", "CLIENT");
        Attributes attributes = new Attributes();
        attributes.put("jdbc.sql", sql);
        attributes.put("jdbc.arguments", arguments);
        span.putAttributes(attributes);

        StorageSpanData data = dataExtractor.extractData(span);

        assertEquals(StorageOperation.DELETE, data.getOperation());
        assertNull(data.getOutputData());
        assertTrue(data.getAffectedData().isEmpty());

        assertEquals(Set.of("product.id"), data.getSelectors());

        DataObject inputData = data.getInputData();
        DataObject product = (DataObject) inputData.getDataValue("product").getValue();
        assertEquals(Set.of("id"), product.getFieldNames());
        assertEquals("87a93e3f", product.getDataValue("id").getValue());
    }
}
