package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
		Map<String, Value<?>> expected = new HashMap<>();
		expected.put("size", new IntegerLiteral(1));
		expected.put("isEmpty", BooleanLiteral.FALSE);
		expected.put("object", new ComplexValue(DataObject.parseData("{id:0, value:\"string\"}")));
		expected.put("object.id", new IntegerLiteral(0));
		expected.put("object.value", new StringLiteral("string"));
		Map<String, Node> actual = d.getDataValues();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataValuesArrayTest() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Value<?>> expected = new HashMap<>();
		expected.put("size", new IntegerLiteral(0));
		expected.put("object", new ComplexValue(DataObject.parseData("{number:2}")));
		expected.put("object.number", new IntegerLiteral(2));
		expected.put("array", new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
		  new IntegerLiteral(1),
		  new IntegerLiteral(2),
		  new IntegerLiteral(3),
		  new IntegerLiteral(4) }));
		Map<String, Node> actual = d.getDataValues();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.INTEGER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.INTEGER);
		expected.put("array", new ArrayType(TypeEnum.INTEGER));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataNestedArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[[1,2],[3,4]]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.INTEGER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.INTEGER);
		expected.put("array", new ArrayType(new ArrayType(TypeEnum.INTEGER)));

		Map<String, Type> actual = d.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataObjectArrayTypes() {
		String jsonData = "{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}";
		DataObject d = DataObject.parseData(jsonData);
		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.INTEGER);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.number", TypeEnum.INTEGER);
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

	@Test
	void putNodeValueBoolean() {
		DataObject object = new DataObject();

		object.putNodeValue("value", BooleanLiteral.TRUE);

		DataValue<?> value = object.getDataValue("value");
		assertNotNull(value);
		assertEquals(TypeEnum.BOOLEAN, value.getType());
		assertEquals(true, value.getValue());
	}

	@Test
	void putNodeValueInteger() {
		DataObject object = new DataObject();

		object.putNodeValue("value", new IntegerLiteral(42));

		DataValue<?> value = object.getDataValue("value");
		assertNotNull(value);
		assertEquals(TypeEnum.INTEGER, value.getType());
		assertEquals(42, value.getValue());
	}

	@Test
	void putNodeValueNumber() {
		DataObject object = new DataObject();

		object.putNodeValue("value", new NumberLiteral(42.5));

		DataValue<?> value = object.getDataValue("value");
		assertNotNull(value);
		assertEquals(TypeEnum.NUMBER, value.getType());
		assertEquals(42.5, value.getValue());
	}

	@Test
	void putNodeValueString() {
		DataObject object = new DataObject();

		object.putNodeValue("value", new StringLiteral("hello"));

		DataValue<?> value = object.getDataValue("value");
		assertNotNull(value);
		assertEquals(TypeEnum.STRING, value.getType());
		assertEquals("hello", value.getValue());
	}

	@Test
	void putNodeValueComplexValue() {
		DataObject nested = new DataObject();
		nested.putValue("number", 42);

		DataObject object = new DataObject();

		object.putNodeValue("value", new ComplexValue(nested));

		DataValue<?> value = object.getDataValue("value");
		assertNotNull(value);
		assertEquals(TypeEnum.COMPLEXTYPE, value.getType());

		DataObject result = (DataObject) value.getValue();
		assertEquals(42, result.getDataValue("number").getValue());

		// Verify that ComplexValue was cloned rather than sharing the object.
		assertNotSame(nested, result);
	}

	@Test
	void putNodeValueIntegerArray() {
		DataObject object = new DataObject();

		ArrayValues<IntegerLiteral> values = new ArrayValues<>(
				TypeEnum.INTEGER,
				new IntegerLiteral[]{
						new IntegerLiteral(1),
						new IntegerLiteral(2),
						new IntegerLiteral(3)
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(new ArrayType(TypeEnum.INTEGER), value.getType());

		Integer[] elements = (Integer[]) value.getValue();
		assertEquals(3, elements.length);
		assertEquals(1, elements[0]);
		assertEquals(2, elements[1]);
		assertEquals(3, elements[2]);
	}

	@Test
	void putNodeValueBooleanArray() {
		DataObject object = new DataObject();
		ArrayValues<BooleanLiteral> values = new ArrayValues<>(
				TypeEnum.BOOLEAN,
				new BooleanLiteral[]{
						BooleanLiteral.TRUE,
						BooleanLiteral.FALSE,
						BooleanLiteral.TRUE
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(new ArrayType(TypeEnum.BOOLEAN), value.getType());

		boolean[] elements = (boolean[]) value.getValue();
		assertEquals(3, elements.length);
		assertTrue(elements[0]);
		assertFalse(elements[1]);
		assertTrue(elements[2]);
	}

	@Test
	void putNodeValueNumberArray() {
		DataObject object = new DataObject();
		ArrayValues<NumberLiteral> values = new ArrayValues<>(
				TypeEnum.NUMBER,
				new NumberLiteral[]{
						new NumberLiteral(1.5),
						new NumberLiteral(2.5),
						new NumberLiteral(3.5)
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(new ArrayType(TypeEnum.NUMBER), value.getType());

		Number[] elements = (Number[]) value.getValue();
		assertEquals(3, elements.length);
		assertEquals(1.5, elements[0]);
		assertEquals(2.5, elements[1]);
		assertEquals(3.5, elements[2]);
	}

	@Test
	void putNodeValueStringArray() {
		DataObject object = new DataObject();
		ArrayValues<StringLiteral> values = new ArrayValues<>(
				TypeEnum.STRING,
				new StringLiteral[]{
						new StringLiteral("one"),
						new StringLiteral("two"),
						new StringLiteral("three")
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(new ArrayType(TypeEnum.STRING), value.getType());

		String[] elements = (String[]) value.getValue();
		assertEquals(3, elements.length);
		assertEquals("one", elements[0]);
		assertEquals("two", elements[1]);
		assertEquals("three", elements[2]);
	}

	@Test
	void putNodeValueComplexArray() {
		DataObject object = new DataObject();
		DataObject first = new DataObject();
		first.putValue("value", 1);
		DataObject second = new DataObject();
		second.putValue("value", 2);
		ArrayValues<ComplexValue> values = new ArrayValues<>(
				TypeEnum.COMPLEXTYPE,
				new ComplexValue[]{
						new ComplexValue(first),
						new ComplexValue(second)
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(new ArrayType(TypeEnum.COMPLEXTYPE), value.getType());

		DataObject[] elements = (DataObject[]) value.getValue();
		assertEquals(2, elements.length);
		assertEquals(first, elements[0]);
		assertEquals(second, elements[1]);

		// Verify that the objects were cloned.
		assertNotSame(first, elements[0]);
		assertNotSame(second, elements[1]);
	}

	@Test
	void putNodeValueNestedIntegerArray() {
		DataObject object = new DataObject();
		ArrayValues<IntegerLiteral> first = new ArrayValues<>(
				TypeEnum.INTEGER,
				new IntegerLiteral[]{
						new IntegerLiteral(1),
						new IntegerLiteral(2)
				});
		ArrayValues<IntegerLiteral> second = new ArrayValues<>(
				TypeEnum.INTEGER,
				new IntegerLiteral[]{
						new IntegerLiteral(3),
						new IntegerLiteral(4)
				});
		ArrayValues<ArrayValues<IntegerLiteral>> values = new ArrayValues<>(
				new ArrayType(TypeEnum.INTEGER),
				new ArrayValues[]{
						first,
						second
				});

		object.putNodeValue("values", values);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(
				new ArrayType(new ArrayType(TypeEnum.INTEGER)),
				value.getType()
		);

		DataValue<?>[] elements = (DataValue<?>[]) value.getValue();
		assertEquals(2, elements.length);
		assertEquals(
				new ArrayType(TypeEnum.INTEGER),
				elements[0].getType()
		);
		assertEquals(
				new ArrayType(TypeEnum.INTEGER),
				elements[1].getType()
		);

		Integer[] firstElements = (Integer[]) elements[0].getValue();
		Integer[] secondElements = (Integer[]) elements[1].getValue();
		assertArrayEquals(
				new Integer[]{1, 2},
				firstElements
		);
		assertArrayEquals(
				new Integer[]{3, 4},
				secondElements
		);
	}

	@Test
	void putNodeValueDeeplyNestedIntegerArray() {
		DataObject object = new DataObject();
		ArrayValues<IntegerLiteral> inner = new ArrayValues<>(
				TypeEnum.INTEGER,
				new IntegerLiteral[]{
						new IntegerLiteral(1),
						new IntegerLiteral(2)
				});
		ArrayValues<ArrayValues<IntegerLiteral>> middle = new ArrayValues<>(
				new ArrayType(TypeEnum.INTEGER),
				new ArrayValues[]{
						inner
				});
		ArrayValues<ArrayValues<ArrayValues<IntegerLiteral>>> outer =
				new ArrayValues<>(
						new ArrayType(new ArrayType(TypeEnum.INTEGER)),
						new ArrayValues[]{
								middle
						});

		object.putNodeValue("values", outer);

		DataValue<?> value = object.getDataValue("values");
		assertNotNull(value);
		assertEquals(
				new ArrayType(
						new ArrayType(
								new ArrayType(TypeEnum.INTEGER)
						)
				),
				value.getType()
		);

		DataValue<?>[] level1 = (DataValue<?>[]) value.getValue();
		assertEquals(1, level1.length);

		DataValue<?>[] level2 = (DataValue<?>[]) level1[0].getValue();
		assertEquals(1, level2.length);

		Integer[] level3 = (Integer[]) level2[0].getValue();
		assertArrayEquals(
				new Integer[]{1, 2},
				level3
		);
	}
}
