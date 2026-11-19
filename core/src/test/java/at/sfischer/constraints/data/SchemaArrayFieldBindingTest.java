package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.IConstraint;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.ArrayEquals;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.array.ForEach;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaArrayFieldBindingTest {

    @Test
    void matchesRequestsForBothProductsFromTheSameCart() {
        InOutputDataCollection data = new InOutputDataCollection();
        String cart = "{payload:{items:[{productId:'HAY-002'},{productId:'STRW-001'}]}}";
        addSample(data, cart, "{path_params:{productId:'HAY-002'}}");
        addSample(data, cart, "{path_params:{productId:'STRW-001'}}");
        addSample(data, cart, "{path_params:{productId:'MISSING'}}");

        assertResult(evaluate(data, membership()), 2, 1);
    }

    @Test
    void preservesForAllInsteadOfReplacingItWithMembership() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{items:[{id:'HAY-002'},{id:'HAY-002'}]}", "{id:'HAY-002'}");
        addSample(data, "{items:[{id:'HAY-002'},{id:'STRW-001'}]}", "{id:'HAY-002'}");
        addSample(data, "{items:[{id:'STRW-001'}]}", "{id:'HAY-002'}");

        Node template = new ForAll(new Variable("a"), comparison());
        List<ConstraintResults<Pair<DataObject, DataObject>>> results = evaluate(data, template);

        assertResult(results, 1, 2);
        assertTrue(results.stream().allMatch(result ->
                ((Constraint) result.constraint()).term() instanceof ForAll));
    }

    @Test
    void bindsQuantifiersInsideOtherOperatorsWithoutChangingTheTemplate() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{items:[{id:'OTHER'},{id:'HAY-002'}]}", "{id:'HAY-002'}");
        addSample(data, "{items:[{id:'OTHER'}]}", "{id:'HAY-002'}");
        Node template = new AndOperator(new NotOperator(membership()), BooleanLiteral.TRUE);
        Node original = template.cloneNode();

        assertResult(evaluate(data, template), 1, 1);
        assertEquals(original, template);
    }

    @Test
    void projectsFieldsForArrayEqualityWithoutAQuantifier() {
        InOutputDataCollection data = new InOutputDataCollection();
        String cart = "{items:[{productId:'HAY-002'},{productId:'STRW-001'}]}";
        addSample(data, cart, "{productIds:['HAY-002','STRW-001']}");
        addSample(data, cart, "{productIds:['HAY-002','MISSING']}");
        Node template = new ArrayEquals(new Variable("a"), new Variable("b"));

        assertResult(evaluate(data, template), 1, 1);
    }

    @Test
    void preservesQuantifierSemanticsWhenTheProjectedArrayIsEmpty() {
        InOutputDataCollection data = new InOutputDataCollection();
        addSample(data, "{items:[{id:'HAY-002'},{id:'HAY-002'}]}", "{id:'HAY-002'}");
        InOutputDataSchema<SimpleDataSchema> existsSchema = data.deriveSchema(new DefaultTypePromotionPolicy());
        InOutputDataSchema<SimpleDataSchema> forAllSchema = data.deriveSchema(new DefaultTypePromotionPolicy());
        DataObject emptyCart = new DataObject();
        emptyCart.putValue("items", new DataObject[0]);
        data.addDataEntry(emptyCart, DataObject.parseData("{id:'HAY-002'}"));

        assertResult(evaluate(data, membership(), existsSchema), 1, 1);
        assertResult(evaluate(data, new ForAll(new Variable("a"), comparison()), forAllSchema), 2, 0);
    }

    @Test
    void simpleSchemaUsesAnElementRelativePathForNestedObjectFields() {
        DataObject sample = DataObject.parseData(
                "{items:[{details:{productId:'OTHER'}},{details:{productId:'HAY-002'}}],selected:'HAY-002'}"
        );
        SimpleDataSchema schema = SimpleDataSchema.deriveFromData(sample);
        schema.fillSchemaWithConstraints(membership(), term -> Set.of(new Constraint(term)));

        Map<DataSchemaEntry<SimpleDataSchema>, Set<IConstraint>> constraints = new HashMap<>();
        Map<DataSchemaEntry<SimpleDataSchema>, Set<IConstraint>> potentialConstraints = new HashMap<>();
        schema.collectAllConstraints(constraints, potentialConstraints);
        List<ArrayQuantifier> generated = new ArrayList<>();
        for (Set<IConstraint> candidates : potentialConstraints.values()) {
            for (IConstraint candidate : candidates) {
                ArrayQuantifier quantifier = assertInstanceOf(ArrayQuantifier.class, ((Constraint) candidate).term());
                if (quantifier.getParameter(0) instanceof ForEach projection
                        && projection.getParameter(0) instanceof DataReference array
                        && array.getName().equals("items")) {
                    generated.add(quantifier);
                }
            }
        }

        assertEquals(1, generated.size());
        ArrayQuantifier quantifier = generated.getFirst();
        ForEach projection = assertInstanceOf(ForEach.class, quantifier.getParameter(0));
        assertEquals(new ArrayType(TypeEnum.STRING), projection.getReturnType());
        assertEquals(new ArrayType(TypeEnum.STRING), projection.cloneNode().getReturnType());
        StringEquals condition = assertInstanceOf(StringEquals.class, quantifier.getParameter(1));
        assertEquals(new Variable(ArrayOperation.ELEMENT_NAME), condition.getParameter(0));
        Reference reference = assertInstanceOf(Reference.class, projection.getParameter(1));
        assertEquals(TypeEnum.STRING, reference.getReturnType());
        assertEquals(TypeEnum.STRING, reference.cloneNode().getReturnType());
        assertEquals(new Variable(ArrayOperation.ELEMENT_NAME), reference.getParameter(0));
        assertEquals(new StringLiteral("details.productId"), reference.getParameter(1));
        Map<Variable, Node> values = new HashMap<>();
        for (Variable variable : quantifier.findInvolvedVariables()) {
            Node value = sample.getDataValues().get(variable.getName());
            if (value != null) {
                values.put(variable, value);
            }
        }
        Node result = quantifier.setVariableValues(values).evaluate();
        assertEquals(BooleanLiteral.TRUE, result);
    }

    private static Exists membership() {
        return new Exists(new Variable("a"), comparison());
    }

    private static StringEquals comparison() {
        return new StringEquals(new Variable(ArrayOperation.ELEMENT_NAME), new Variable("b"));
    }

    private static void addSample(InOutputDataCollection data, String source, String target) {
        data.addDataEntry(DataObject.parseData(source), DataObject.parseData(target));
    }

    private static List<ConstraintResults<Pair<DataObject, DataObject>>> evaluate(
            InOutputDataCollection data, Node template) {
        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
        return evaluate(data, template, schema);
    }

    private static List<ConstraintResults<Pair<DataObject, DataObject>>> evaluate(
            InOutputDataCollection data, Node template, InOutputDataSchema<SimpleDataSchema> schema) {
        schema.fillSchemaWithConstraints(template, term -> Set.of(new Constraint(term)));
        EvaluationResults<DataSchema, Pair<DataObject, DataObject>> evaluation = schema.evaluate(data);
        List<ConstraintResults<Pair<DataObject, DataObject>>> results = new ArrayList<>();
        for (Set<ConstraintResults<Pair<DataObject, DataObject>>> fieldResults : evaluation.getPotentialConstraintResults().values()) {
            results.addAll(fieldResults);
        }
        return results;
    }

    private static void assertResult(List<ConstraintResults<Pair<DataObject, DataObject>>> results,
                                     int valid, int invalid) {
        assertTrue(results.stream().anyMatch(result ->
                result.numberOfValidDataEntries() == valid && result.numberOfViolations() == invalid));
    }
}
