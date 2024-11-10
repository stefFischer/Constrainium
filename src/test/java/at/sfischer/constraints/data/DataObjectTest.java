package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataObjectTest {
	@Test
	public void parseSimpleData() {
		String jsonData = "{size:0, isEmpty:true}";
		DataObject expected = new DataObject();
		expected.putValue("size", 0);
		expected.putValue("isEmpty", true);

		DataObject actual = DataObject.parseData(jsonData);

		assertThat(actual).
		   usingRecursiveComparison().
		   isEqualTo(expected);
	}

	@Test
	public void parseComplexData() {
		String jsonData = "{size:1, isEmpty:false, object:{id:0, value:\"string\"}}";
		DataObject expected = new DataObject();
		expected.putValue("size", 1);
		expected.putValue("isEmpty", false);
		DataObject objectValue = new DataObject();
		objectValue.putValue("id", 0);
		objectValue.putValue("value", "string");
		expected.putValue("object", objectValue);

		DataObject actual = DataObject.parseData(jsonData);

		assertThat(actual).
		   usingRecursiveComparison().
		   isEqualTo(expected);
	}

	@Test
	public void getDataValuesTest() {
		String jsonData = "{size:1, isEmpty:false, object:{id:0, value:\"string\"}}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Literal<?>> expected = new HashMap<>();
		expected.put("size", new NumberLiteral(1));
		expected.put("isEmpty", new BooleanLiteral(false));
		expected.put("object", new ComplexValue(DataObject.parseData("{id:0, value:\"string\"}")));
		expected.put("object.id", new NumberLiteral(0));
		expected.put("object.value", new StringLiteral("string"));
		Map<String, Node> actual = d.getDataValues();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataValuesArrayTest() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Literal<?>> expected = new HashMap<>();
		expected.put("size", new NumberLiteral(0));
		expected.put("object", new ComplexValue(DataObject.parseData("{number:2}")));
		expected.put("object.number", new NumberLiteral(2));
		expected.put("array", new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{ 
		  new NumberLiteral(1), 
		  new NumberLiteral(2), 
		  new NumberLiteral(3), 
		  new NumberLiteral(4) }));
		Map<String, Node> actual = d.getDataValues();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.NUMBER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.NUMBER);
		expected.put("array", new ArrayType(TypeEnum.NUMBER));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataNestedArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[[1,2],[3,4]]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.NUMBER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.NUMBER);
		expected.put("array", new ArrayType(new ArrayType(TypeEnum.NUMBER)));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataObjectArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.NUMBER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.NUMBER);
		expected.put("array", new ArrayType(TypeEnum.COMPLEXTYPE));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataObjectArrayTypesIncludingArrays() {
		String jsonData = "{array:[{numbers:[1]},{numbers:[2]},{numbers:[3]}]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("array", new ArrayType(TypeEnum.COMPLEXTYPE));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}
}
