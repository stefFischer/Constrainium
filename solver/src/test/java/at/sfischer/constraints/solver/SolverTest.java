package at.sfischer.constraints.solver;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import at.sfischer.constraints.model.operators.strings.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SolverTest {

	private Map<Variable, Value<?>> solve(Node... terms){
		Constraint[] constraints = new Constraint[terms.length];
		for (int i = 0; i < terms.length; i++) {
			constraints[i] = new Constraint(terms[i]);
		}

		SolverZ3 s = new SolverZ3();
		return s.solveConstraint(constraints);
	}

	private void testUnsolvable(Node... terms){
		Map<Variable, Value<?>> values = solve(terms);
		assertNull(values);
	}

	private void testSolver(Node... terms){
		Map<Variable, Value<?>> values = solve(terms);

		System.out.println(values);

        Map<Variable, Node> vals = new HashMap<>(values);
		Node term = buildTerm(terms);
		Node replacedTerm = term.setVariableValues(vals);

		assertEquals(BooleanLiteral.TRUE, replacedTerm.evaluate());
	}

	private Node buildTerm(Node... terms){
		if (terms == null || terms.length == 0) {
			throw new IllegalArgumentException("At least one term must be provided");
		}
		if(terms.length == 1){
			return terms[0];
		}

		Node result = new AndOperator(terms[0], terms[1]);
		for (int i = 2; i < terms.length; i++) {
			result = new AndOperator(result, terms[i]);
		}

		return result;
	}

	@Test
	public void solveSimpleEqualConstraint()  {
		Node term = new EqualOperator(new Variable("a"), new NumberLiteral(5));
		testSolver(term);
	}

	@Test
	public void solveSimpleNotEqualConstraint()  {
		Node term = new NotOperator(new EqualOperator(new Variable("a"), new NumberLiteral(5)));
		testSolver(term);
	}

	@Test
	public void solveSimpleLessThanConstraint()  {
		Node term = new LessThanOperator(new Variable("a"), new NumberLiteral(5));
		testSolver(term);
	}

	@Test
	public void solveSimpleLessEqualConstraint()  {
		Node term = new LessThanOrEqualOperator(new Variable("a"), new NumberLiteral(5));
		testSolver(term);
	}

	@Test
	public void solveBooleanLogicConstraint()  {
		Node term = new AndOperator(new Variable("a"), new OrOperator(new NotOperator(new Variable("b")), new Variable("c")));
		testSolver(term);
	}

	@Test
	public void solveArithmeticOperationsConstraint1()  {
		Node term = new EqualOperator(new AdditionOperator(new Variable("a"), new SubtractionOperator(new Variable("b"), new Variable("c"))), new NumberLiteral(5));
		testSolver(term);
	}

	@Test
	public void solveArithmeticOperationsConstraint2()  {
		Node term = new EqualOperator(new MultiplicationOperator(new Variable("a"), new DivisionOperator(new Variable("b"), new Variable("c"))), new NumberLiteral(5));
		testSolver(term);
	}

	@Test
	public void solveOneOfNumberConstraint()  {
		@SuppressWarnings("unchecked")
		ArrayValues<NumberLiteral> options = (ArrayValues<NumberLiteral>) ArrayValues.createArrayValuesFromList(List.of(new NumberLiteral(1), new NumberLiteral(2)));
		Node term = new OneOfNumber(new Variable("a"), options);
		testSolver(term);
	}

	@Test
	public void solveMultipleConstraints()  {
		@SuppressWarnings("unchecked")
		ArrayValues<NumberLiteral> options = (ArrayValues<NumberLiteral>) ArrayValues.createArrayValuesFromList(List.of(new NumberLiteral(1), new NumberLiteral(2)));
		Node term1 = new OneOfNumber(new Variable("a"), options);
		Node term2 = new GreaterThanOperator(new Variable("a"), new NumberLiteral(1));
		testSolver(term1, term2);
	}

	@Test
	public void solveUnsatisfiableConstraints()  {
		@SuppressWarnings("unchecked")
		ArrayValues<NumberLiteral> options = (ArrayValues<NumberLiteral>) ArrayValues.createArrayValuesFromList(List.of(new NumberLiteral(1), new NumberLiteral(2)));
		Node term1 = new OneOfNumber(new Variable("a"), options);
		Node term2 = new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(3));
		testUnsolvable(term1, term2);
	}

	@Test
	public void solveStringEqualsConstraint()  {
		Node term = new StringEquals(new Variable("a"), new StringLiteral("Hello World!"));
		testSolver(term);
	}

	@Test
	public void solveStringNotEqualsConstraint()  {
		Node term = new NotOperator(new StringEquals(new Variable("a"), new StringLiteral("Hello World!")));
		testSolver(term);
	}

	@Test
	public void solveIsSubStringConstantConstraint()  {
		Node term = new SubString(new Variable("a"), new StringLiteral("Hello World!"));
		testSolver(term);
	}

	@Test
	public void solveIsSubStringConstraint()  {
		Node term = new SubString(new Variable("a"), new Variable("b"));
		testSolver(term);
	}

	@Test
	public void solveStringMatchesConstraint()  {
		Node term = new StringMatches(new Variable("a"), new StringLiteral("ab+"));
		testSolver(term);
	}

	@Test
	public void solveStringMatchesUnsupportedConstraint()  {
		Node term = new StringMatches(new Variable("a"), new Variable("b"));
		assertThrows(UnsupportedOperationException.class, () -> testSolver(term));
	}

	@Test
	public void solveMatchesRegexConstraint()  {
		Node term = new MatchesRegex(new Variable("a"), new StringLiteral("ab*"));
		testSolver(term);
	}

	@Test
	public void solveIsNumericConstraint()  {
		Node term = new IsNumeric(new Variable("a"));
		Node term2 = new EqualOperator(new StringLength(new Variable("a")), new NumberLiteral(5));
		testSolver(term, term2);
	}

	@Test
	public void solveIsEmailConstraint()  {
		Node term = new IsEmail(new Variable("a"));
		Node term2 = new GreaterThanOrEqualOperator(new StringLength(new Variable("a")), new NumberLiteral(25));
		testSolver(term, term2);
	}

	@Test
	public void solveIsUrlConstraint()  {
		Node term = new IsUrl(new Variable("a"));
		Node term2 = new GreaterThanOrEqualOperator(new StringLength(new Variable("a")), new NumberLiteral(20));
		testSolver(term, term2);
	}

	@Test
	public void solveIsHourConstraint()  {
		Node term = new IsHour(new Variable("a"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_24H_S));
		testSolver(term);
	}

	@Test
	public void solveIsDateConstraint()  {
		Node term = new IsDate(new Variable("a"), new ArrayValues<>(TypeEnum.STRING, IsDate.YMD_DATE_PATTERNS));
		testSolver(term);
	}

	@Test
	public void solveIsDateTimeConstraint()  {
		Node term = new IsDateTime(new Variable("a"), new ArrayValues<>(TypeEnum.STRING, IsDateTime.DATE_PATTERNS));
		testSolver(term);
	}

	@Test
	public void solveOneOfStringConstraint()  {
		@SuppressWarnings("unchecked")
		ArrayValues<StringLiteral> options = (ArrayValues<StringLiteral>) ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("Hello"), new StringLiteral("World")));
		Node term = new OneOfString(new Variable("a"), options);
		testSolver(term);
	}

//	@Test
//	public void solveArrayLengthConstraint()  {
//		Node term = new EqualOperator(new ArrayLength(new Variable("a")), new NumberLiteral(5));
//		testSolver(term);
//	}
}
