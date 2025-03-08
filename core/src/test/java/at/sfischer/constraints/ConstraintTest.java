package at.sfischer.constraints;

import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstraintTest {

	@Test
	public void applyDataTestAllApply() {
		Constraint constraint = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new NumberLiteral(0)));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, data);
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyDataTestAllViolations() {
		Constraint constraint = new Constraint(new LessThanOperator(new Variable("size"), new NumberLiteral(0)));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, data.emptyDataCollection(), data, data.emptyDataCollection(), data.emptyDataCollection());
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyDataTestMixedApplyAndViolations() {
		Constraint constraint = new Constraint(new GreaterThanOperator(new Variable("size"), new NumberLiteral(0)));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);
		SimpleDataCollection expectedAppliedData = SimpleDataCollection.parseData(
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);
		SimpleDataCollection expectedInvalidData = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, expectedAppliedData, expectedInvalidData, data.emptyDataCollection(), data.emptyDataCollection());
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyDataTestNotApplicableData() {
		Constraint constraint = new Constraint(new GreaterThanOperator(new Variable("object.number"), new NumberLiteral(0)));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true}",
				"{size:1, isEmpty:false}",
				"{size:3, isEmpty:false}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, new SimpleDataCollection(), data);
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyDataTestArray() {
		Constraint constraint = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[1,2,3,4]}",
				"{size:1, object:{number:3}, array:[5,6,7,9]}",
				"{size:3, object:{number:7}, array:[10,1,2,3]}"
		);
		SimpleDataCollection expectedAppliedData = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[1,2,3,4]}",
				"{size:1, object:{number:3}, array:[5,6,7,9]}"
		);
		SimpleDataCollection expectedInvalidData = SimpleDataCollection.parseData(
				"{size:3, object:{number:7}, array:[10,1,2,3]}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, expectedAppliedData, expectedInvalidData, data.emptyDataCollection(), data.emptyDataCollection());
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyDataTestObjectArray() {
		Constraint constraint = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new NumberLiteral(10))));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}",
				"{size:1, object:{number:3}, array:[{number:4},{number:5},{number:6}]}",
				"{size:3, object:{number:7}, array:[{number:7},{number:8},{number:9}]}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, data);
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}
}
