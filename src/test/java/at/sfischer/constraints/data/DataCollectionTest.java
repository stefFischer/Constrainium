package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataCollectionTest {

	@Test
	public void getDataTypesTest1() {
		SimpleDataCollection dataCollection = SimpleDataCollection.parseData(
		  "{size:0, isEmpty:true}",
		  "{size:1, isEmpty:false}",
		  "{size:3, isEmpty:false}");


		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.NUMBER);
		expected.put("isEmpty", TypeEnum.BOOLEAN);
		Map<String, Type> actual = dataCollection.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataTypesTest2() {
		SimpleDataCollection dataCollection = SimpleDataCollection.parseData(
		  "{size:0, isEmpty:true, object:{id:0, value:\"string1\"}}",
		  "{size:1, isEmpty:false, object:{id:1, value:\"string2\"}}");


		Map<String, Type> expected = new HashMap<>();
		expected.put("size", TypeEnum.NUMBER);
		expected.put("isEmpty", TypeEnum.BOOLEAN);
		expected.put("object", TypeEnum.COMPLEXTYPE);
		expected.put("object.id", TypeEnum.NUMBER);
		expected.put("object.value", TypeEnum.STRING);
		Map<String, Type> actual = dataCollection.getDataTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void getDataTypesInconsistencyErrorTest() {
		SimpleDataCollection dataCollection = SimpleDataCollection.parseData(
		  "{size:0, isEmpty:true}",
		  "{size:true, isEmpty:false}");


		Exception exception = assertThrows(IllegalStateException.class, dataCollection::getDataTypes);

		String expectedMessage = "Field size has inconsistent types: ";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.startsWith(expectedMessage));
	}
}
