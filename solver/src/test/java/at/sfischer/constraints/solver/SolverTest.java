package at.sfischer.constraints.solver;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import org.junit.jupiter.api.Test;

public class SolverTest {
	@Test
	public void solveBooleanLogicConstraint()  {
		SolverZ3 s = new SolverZ3();

		Node term = new AndOperator(new Variable("a"), new OrOperator(new NotOperator(new Variable("b")), new Variable("c")));
		Constraint constraint = new Constraint(term);

		s.solveConstraint(constraint);
	}

	@Test
	public void solveArithmeticOperationsConstraint1()  {
		SolverZ3 s = new SolverZ3();

		Node term = new EqualOperator(new AdditionOperator(new Variable("a"), new SubtractionOperator(new Variable("b"), new Variable("c"))), new NumberLiteral(5));
		Constraint constraint = new Constraint(term);

		s.solveConstraint(constraint);
	}

	@Test
	public void solveArithmeticOperationsConstraint2()  {
		SolverZ3 s = new SolverZ3();

		Node term = new EqualOperator(new MultiplicationOperator(new Variable("a"), new DivisionOperator(new Variable("b"), new Variable("c"))), new NumberLiteral(5));
		Constraint constraint = new Constraint(term);

		s.solveConstraint(constraint);
	}
}
