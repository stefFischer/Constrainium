package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SuperSetTest {
	@Test
	public void evaluateNumberSuperSet() {
		Node a = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3),
				new NumberLiteral(4)
		});
		Node b = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(4)
		});
		SuperSet s = new SuperSet(TypeEnum.NUMBER, a, b);
		Node result = s.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateNumberSuperSetFalse() {
		Node a = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3)
		});
		Node b = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(4)
		});
		SuperSet s = new SuperSet(TypeEnum.NUMBER, a, b);
		Node result = s.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateStringSuperSet() {
		Node a = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("FOUR")
		});
		Node b = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("FOUR")
		});
		SuperSet s = new SuperSet(TypeEnum.STRING, a, b);
		Node result = s.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateStringSuperSetFalse() {
		Node a = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE")
		});
		Node b = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("FOUR")
		});
		SuperSet s = new SuperSet(TypeEnum.STRING, a, b);
		Node result = s.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateMixedTypesSuperSet() {
		Node a = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("FOUR")
		});
		Node b = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(4)
		});
		SuperSet s = new SuperSet(a, b);
		Node result = s.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}
