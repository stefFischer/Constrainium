package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static at.sfischer.constraints.data.Utils.Group;
import static at.sfischer.constraints.data.Utils.Path;
import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {
	@Test
	public void groupByCollapsedHierarchySingleGroup() {
		Set<Variable> variables = Set.of(
				new Variable("a.b.x"),
				new Variable("a.b.y"),
				new Variable("a.d.z")
		);

		Map<Path, Group> expected = Map.of(
			Path.of("a"), new Group(Path.of("a"),
				Map.of(
					Path.of("b"), new Group(Path.of("b"),
						Map.of(
							Path.of("x"), new Group(Path.of("x"), new Variable("a.b.x"), Map.of()),
							Path.of("y"), new Group(Path.of("y"), new Variable("a.b.y"), Map.of())
						)
					),
					Path.of("d.z"), new Group(Path.of("d.z"), new Variable("a.d.z"), Map.of())
				)
			)
		);

		Map<Path, Group> actual = Utils.groupByCollapsedHierarchy(variables);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void groupByCollapsedHierarchyMultipleIndependentGroups() {
		Set<Variable> variables = Set.of(
				new Variable("a.b.x"),
				new Variable("a.b.y"),
				new Variable("c.d.z")
		);

		Map<Path, Group> expected = Map.of(
			Path.of("a.b"), new Group(Path.of("a.b"),
				Map.of(
					Path.of("x"), new Group(Path.of("x"), new Variable("a.b.x"), Map.of()),
					Path.of("y"), new Group(Path.of("y"), new Variable("a.b.y"), Map.of())
				)
			),
			Path.of("c.d.z"), new Group(Path.of("c.d.z"), new Variable("c.d.z"), Map.of())
		);

		Map<Path, Group> actual = Utils.groupByCollapsedHierarchy(variables);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void groupByCollapsedHierarchyNoneLeafVariableGroup() {
		Set<Variable> variables = Set.of(
				new Variable("a.b"),
				new Variable("a.b.y"),
				new Variable("a.d.z")
		);

		Map<Path, Group> expected = Map.of(
				Path.of("a"), new Group(Path.of("a"),
						Map.of(
								Path.of("b"), new Group(Path.of("b"), new Variable("a.b"),
										Map.of(
												Path.of("y"), new Group(Path.of("y"), new Variable("a.b.y"), Map.of())
										)
								),
								Path.of("d.z"), new Group(Path.of("d.z"), new Variable("a.d.z"), Map.of())
						)
				)
		);

		Map<Path, Group> actual = Utils.groupByCollapsedHierarchy(variables);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void groupByCollapsedHierarchyNestedAndFlat() {
		Set<Variable> variables = Set.of(
				new Variable("r.s.t.a.b.x"),
				new Variable("r.s.t.a.b.y"),
				new Variable("r.s.t.a.d.e"),
				new Variable("r.s.t.f.g.h.i")
		);

		Map<Path, Group> expected = Map.of(
			Path.of("r.s.t"), new Group(Path.of("r.s.t"),
				Map.of(
					Path.of("a"), new Group(Path.of("a"),
						Map.of(
							Path.of("b"), new Group(Path.of("b"),
								Map.of(
									Path.of("x"), new Group(Path.of("x"), new Variable("r.s.t.a.b.x"), Map.of()),
									Path.of("y"), new Group(Path.of("y"), new Variable("r.s.t.a.b.y"), Map.of())
								)
							),
							Path.of("d.e"), new Group(Path.of("d.e"), new Variable("r.s.t.a.d.e"), Map.of())
						)
					),
					Path.of("f.g.h.i"), new Group(Path.of("f.g.h.i"), new Variable("r.s.t.f.g.h.i"), Map.of())
				)
			)
		);

		Map<Path, Group> actual = Utils.groupByCollapsedHierarchy(variables);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void collectValueCombinationsSimpleData() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{id:10}}"
		);
		DataObject dao = data.getDataCollection().get(0);

		Set<Variable> targetVariables = Set.of(new Variable("size"), new Variable("object.id"));

		List<Map<Variable, Node>> expected = List.of(
				Map.of(new Variable("size"), new NumberLiteral(0), new Variable("object.id"), new NumberLiteral(10))
		);
		List<Map<Variable, Node>> actual = Utils.collectValueCombinations(dao, targetVariables);

		Assertions.assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void collectValueCombinationsComplexArray() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{array:[{object:{id:1,number:0}}, {object:{id:3,number:2}}]}"
		);
		DataObject dao = data.getDataCollection().get(0);

		Set<Variable> targetVariables = Set.of(new Variable("array.object.id"), new Variable("array.object.number"));

		List<Map<Variable, Node>> expected = List.of(
				Map.of(new Variable("array.object.id"), new NumberLiteral(1), new Variable("array.object.number"), new NumberLiteral(0)),
				Map.of(new Variable("array.object.id"), new NumberLiteral(3), new Variable("array.object.number"), new NumberLiteral(2))
		);
		List<Map<Variable, Node>> actual = Utils.collectValueCombinations(dao, targetVariables);

		Assertions.assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}
}
