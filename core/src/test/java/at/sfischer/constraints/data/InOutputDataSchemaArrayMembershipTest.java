package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.IConstraint;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.logic.EquivalentOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InOutputDataSchemaArrayMembershipTest {

    @Test
    public void objectArrayMembershipAcceptsEitherPositionAndRejectsANonmember() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(
                data, "{payload:{items:[{productId:'OTHER'},{productId:'HAY-002'}]}}",
                "{path_params:{productId:'HAY-002'}}"
        );
        addSample(
                data, "{payload:{items:[{productId:'HAY-002'},{productId:'OTHER'}]}}",
                "{path_params:{productId:'HAY-002'}}"
        );
        addSample(
                data, "{payload:{items:[{productId:'OTHER'},{productId:'STRW-001'}]}}",
                "{path_params:{productId:'HAY-002'}}"
        );

        List<ConstraintResults<Pair<DataObject, DataObject>>> results = generateAndEvaluate(data, stringMembership());

        assertHasMembershipResult(results, 2, 1);
    }

    @Test
    public void ordinaryEqualityOnlySucceedsForTheSingleItemCase() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{items:[{productId:'HAY-002'}]}", "{productId:'HAY-002'}");
        addSample(data, "{items:[{productId:'OTHER'},{productId:'HAY-002'}]}", "{productId:'HAY-002'}");
        addSample(data, "{items:[{productId:'HAY-002'},{productId:'OTHER'}]}", "{productId:'HAY-002'}");

        Node equality = new StringEquals(new Variable("a"), new Variable("b"));
        List<ConstraintResults<Pair<DataObject, DataObject>>> results = generateAndEvaluate(data, equality);

        assertTrue(results.stream().anyMatch(result ->
                result.numberOfValidDataEntries() == 1 && result.numberOfViolations() == 2
        ), "Scalar equality does not express that one of several product IDs matches");
    }

    @Test
    public void primitiveArrayMembershipDoesNotRequireObjectFieldProjection() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{productIds:['OTHER','HAY-002']}", "{productId:'HAY-002'}");
        addSample(data, "{productIds:['HAY-002','OTHER']}", "{productId:'HAY-002'}");
        addSample(data, "{productIds:['OTHER','STRW-001']}", "{productId:'HAY-002'}");

        assertHasMembershipResult(generateAndEvaluate(data, stringMembership()), 2, 1);
    }

    @Test
    public void preservesTheObjectPathBetweenTheArrayAndItsLeafField() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(
                data,
                "{warehouse:{bins:[{details:{sku:'OTHER'}},{details:{sku:'HAY-002'}}]}}",
                "{selected:{sku:'HAY-002'}}"
        );

        assertHasMembershipResult(generateAndEvaluate(data, stringMembership()), 1, 0);
    }

    @Test
    public void generatesMembershipWithTheArrayOnEitherSideOfThePairedSchema() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{selectedId:'HAY-002'}", "{items:[{id:'OTHER'},{id:'HAY-002'}]}");

        assertHasMembershipResult(generateAndEvaluate(data, stringMembership()), 1, 0);
    }

    @ParameterizedTest
    @MethodSource("numericAndBooleanMembership")
    public void projectsFieldsUsingTheProvidedComparisonTemplate(Node template, String array, String selection) {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, array, selection);

        assertHasMembershipResult(generateAndEvaluate(data, template), 1, 0);
    }

    private static Stream<Arguments> numericAndBooleanMembership() {
        Node numberComparison = new EqualOperator(
                new Variable(ArrayOperation.ELEMENT_NAME), new Variable("b")
        );
        Node booleanComparison = new EquivalentOperator(
                new Variable(ArrayOperation.ELEMENT_NAME), new Variable("b")
        );
        return Stream.of(
                Arguments.of(
                        new Exists(new Variable("a"), numberComparison),
                        "{items:[{quantity:1},{quantity:2}]}", "{selectedQuantity:2}"
                ),
                Arguments.of(
                        new Exists(new Variable("a"), booleanComparison),
                        "{items:[{available:false},{available:true}]}", "{selectedAvailability:true}"
                )
        );
    }

    private Exists stringMembership() {
        return new Exists(
                new Variable("a"),
                new StringEquals(new Variable(ArrayOperation.ELEMENT_NAME), new Variable("b"))
        );
    }

    private void addSample(InOutputDataCollection data, String source, String target) {
        data.addDataEntry(DataObject.parseData(source), DataObject.parseData(target));
    }

    private List<ConstraintResults<Pair<DataObject, DataObject>>> generateAndEvaluate(
            InOutputDataCollection data,
            Node template
    ) {
        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
        schema.fillSchemaWithConstraints(template, term -> Set.of(new Constraint(term)));
        EvaluationResults<DataSchema, Pair<DataObject, DataObject>> evaluation = schema.evaluate(data);
        List<ConstraintResults<Pair<DataObject, DataObject>>> results = new ArrayList<>();
        for (Set<ConstraintResults<Pair<DataObject, DataObject>>> fieldResults : evaluation.getPotentialConstraintResults().values()) {
            results.addAll(fieldResults);
        }

        return results;
    }

    private void assertHasMembershipResult(
            List<ConstraintResults<Pair<DataObject, DataObject>>> results,
            int matchingSamples,
            int nonmatchingSamples
    ) {
        assertTrue(results.stream().anyMatch(result ->
                result.constraint() instanceof Constraint constraint
                        && constraint.term() instanceof Exists
                        && result.numberOfValidDataEntries() == matchingSamples
                        && result.numberOfViolations() == nonmatchingSamples
        ), "Expected a generated Exists constraint with " + matchingSamples
                + " matching and " + nonmatchingSamples + " nonmatching samples");
    }
}
