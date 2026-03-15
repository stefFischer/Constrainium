package at.sfischer.constraints.model.operators.objects;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReferenceTest {

	@Test
	public void evaluateReferenceNumber() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject object = DataObject.parseData(jsonData);
		Reference r = new Reference(new ComplexValue(object), new StringLiteral("size"));
		Node expected = new IntegerLiteral(0);
		Node actual = r.evaluate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateReferenceComplexValue() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject object = DataObject.parseData(jsonData);
		Reference r = new Reference(new ComplexValue(object), new StringLiteral("object"));
		Node expected = new ComplexValue(DataObject.parseData("{number:2}"));
		Node actual = r.evaluate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateReferenceNestedNumber() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject object = DataObject.parseData(jsonData);
		Reference r = new Reference(new ComplexValue(object), new StringLiteral("object.number"));
		Node expected = new IntegerLiteral(2);
		Node actual = r.evaluate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateReferenceNumberArray() {
		String jsonData = "{size:0, object:{number:2}, array:[1,2,3,4]}";
		DataObject object = DataObject.parseData(jsonData);
		Reference r = new Reference(new ComplexValue(object), new StringLiteral("array"));
		Node expected = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
				new IntegerLiteral(1),
				new IntegerLiteral(2),
				new IntegerLiteral(3),
				new IntegerLiteral(4)
		});
		Node actual = r.evaluate();

		assertEquals(expected, actual);
	}

}
