package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.model.MoreStatisticalEvidenceNeeded;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LowerBoundOperatorTest {
	@Test
	public void evaluateMoreStatisticalEvidenceNeeded() {
		Node value = new NumberLiteral(2);
		LowerBoundOperator l = new LowerBoundOperator(value);
		Node expected = MoreStatisticalEvidenceNeeded.INSTANCE;
		Node actual = l.evaluate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateStatisticalEvidenceProvidedAllBounds() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{a:2}",
				"{a:3}",
				"{a:2}",
				"{a:4}",
				"{a:2}",
				"{a:6}",
				"{a:2}",
				"{a:5}"
		);

		Node value = new Variable("a");
		LowerBoundOperator l = new LowerBoundOperator(value);
		Constraint constraint = new Constraint(l);

		ConstraintResults results = constraint.applyData(data);

		assertEquals(1.0, results.applicationRate());
		assertFalse(results.foundCounterExample());
		assertEquals(0, results.numberOfViolations());
	}

	@Test
	public void evaluateStatisticalEvidenceProvidedZeroBound() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{a:2}",
				"{a:0}",
				"{a:2}",
				"{a:4}",
				"{a:0}",
				"{a:6}",
				"{a:0}",
				"{a:5}"
		);

		Node value = new Variable("a");
		LowerBoundOperator l = new LowerBoundOperator(value);
		Constraint constraint = new Constraint(l);

		ConstraintResults results = constraint.applyData(data);

		assertEquals(1.0, results.applicationRate());
		assertFalse(results.foundCounterExample());
		assertEquals(0, results.numberOfViolations());
	}

	@Test
	public void evaluateMissingStatisticalEvidence() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{a:2}",
				"{a:0}",
				"{a:2}",
				"{a:4}",
				"{a:0}",
				"{a:6}",
				"{a:5}"
		);

		Node value = new Variable("a");
		LowerBoundOperator l = new LowerBoundOperator(value);
		Constraint constraint = new Constraint(l);

		ConstraintResults results = constraint.applyData(data);

		assertEquals(0.0, results.applicationRate());
		assertTrue(results.foundCounterExample());
		assertEquals(7, results.numberOfViolations());
	}

	@Test
	public void evaluateBreakingBounds() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{a:2}",
				"{a:0}",
				"{a:2}",
				"{a:0}",
				"{a:4}",
				"{a:0}",
				"{a:6}",
				"{a:-5}",
				"{a:0}",
				"{a:5}"
		);

		Node value = new Variable("a");
		LowerBoundOperator l = new LowerBoundOperator(value);
		Constraint constraint = new Constraint(l);

		ConstraintResults results = constraint.applyData(data);

		assertEquals(0.2, results.applicationRate());
		assertTrue(results.foundCounterExample());
		assertEquals(8, results.numberOfViolations());
	}
}
