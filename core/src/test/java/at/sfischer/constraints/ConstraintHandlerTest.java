package at.sfischer.constraints;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.miner.MinApplicationsPolicy;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConstraintHandlerTest {

    @Test
    public void fillSimpleSchemaAndEvaluate() {
        Node term = new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0));
        ConstraintPolicy retentionPolicy = new NoViolationsPolicy();
        ConstraintTemplate constraintTemplate = new ConstraintTemplate("c1", term, retentionPolicy);
        SimpleDataCollection data = SimpleDataCollection.parseData(
                "{size:0, isEmpty:true, object:{number:2}}",
                "{size:1, isEmpty:false, object:{number:3}}",
                "{size:3, isEmpty:false, object:{number:7}}"
        );
        SimpleDataSchema schema = data.deriveSchema(null);

        ConstraintHandler handler = new ConstraintHandler();
        handler.instantiate(constraintTemplate, schema);
        EvaluationResults<SimpleDataSchema, DataObject> results = handler.evaluate(constraintTemplate, schema, data);
        // TODO This is going to be a problem if we have multiple constraints with different policies.
        handler.retain(constraintTemplate, schema, results);

        DataSchemaEntry<SimpleDataSchema> size = schema.getSchemaEntry("size");
        DataSchemaEntry<SimpleDataSchema> isEmpty = schema.getSchemaEntry("isEmpty");
        DataSchemaEntry<SimpleDataSchema> objectNumber = schema.findDataSchemaEntry("object.number");

        assertEquals(1, size.constraints.size());
        assertEquals(0,  isEmpty.constraints.size());
        assertEquals(1, objectNumber.constraints.size());

        Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new NumberLiteral(0)));
        Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(objectNumber), new NumberLiteral(0)));

        assertEquals(0, results.getEvaluationResults().size());

        ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
        assertEquals(1.0, constraintResults1.applicationRate());
        assertFalse(constraintResults1.foundCounterExample());

        ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(objectNumber, constraint2, data);
        assertEquals(1.0, constraintResults2.applicationRate());
        assertFalse(constraintResults2.foundCounterExample());
    }

    @Test
    public void fillInoutSchemaAndEvaluate() {
        Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("n"));
        ConstraintPolicy retentionPolicy = new NoViolationsPolicy();
        ConstraintTemplate constraintTemplate = new ConstraintTemplate("c1", term, retentionPolicy);
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
                new Pair<>("{add:1}", "{size:1, object:{number:1}}"),
                new Pair<>("{add:2}", "{size:1, object:{number:2}}"));

        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(null);

        ConstraintHandler handler = new ConstraintHandler();
        handler.instantiate(constraintTemplate, schema);
        EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = handler.evaluate(constraintTemplate, schema, data);
        handler.retain(constraintTemplate, schema, results);

        DataSchemaEntry<SimpleDataSchema> add = schema.findDataSchemaEntry("input.add");
        DataSchemaEntry<SimpleDataSchema> size = schema.findDataSchemaEntry("output.size");
        DataSchemaEntry<SimpleDataSchema> objectNumber = schema.findDataSchemaEntry("output.object.number");

        assertEquals(0, add.constraints.size());
        assertEquals(0,  size.constraints.size());
        assertEquals(2, objectNumber.constraints.size());

        Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(size)));
        Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(add)));

        Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(objectNumber)));
        Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(objectNumber), new DataReference(add)));

        Constraint constraint5 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(objectNumber)));
        Constraint constraint6 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(objectNumber), new DataReference(size)));

        assertEquals(0, results.getEvaluationResults().size());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
        assertEquals(2.0/3, constraintResults1.applicationRate());
        assertTrue(constraintResults1.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults2 = results.getPotentialConstraintResults(size, constraint2, data);
        assertEquals(2.0/3, constraintResults2.applicationRate());
        assertTrue(constraintResults2.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults3 = results.getPotentialConstraintResults(objectNumber, constraint3, data);
        assertEquals(1.0, constraintResults3.applicationRate());
        assertFalse(constraintResults3.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults4 = results.getPotentialConstraintResults(objectNumber, constraint4, data);
        assertEquals(1.0, constraintResults4.applicationRate());
        assertFalse(constraintResults4.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults5 = results.getPotentialConstraintResults(size, constraint5, data);
        assertEquals(0.0, constraintResults5.applicationRate());
        assertFalse(constraintResults5.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults6 = results.getPotentialConstraintResults(size, constraint6, data);
        assertEquals(0.0, constraintResults6.applicationRate());
        assertFalse(constraintResults6.foundCounterExample());
    }

    @Test
    public void fillInoutSchemaWithSubSchemasAndEvaluate() {
        Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("n"));
        ConstraintPolicy retentionPolicy = new NoViolationsPolicy();
        ConstraintTemplate constraintTemplate = new ConstraintTemplate("c1", term, retentionPolicy);
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
                new Pair<>("{add:1}", "{size:1, object:{number:1}}"),
                new Pair<>("{add:2}", "{size:1, object:{number:2}}"));

        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(null);

        ConstraintHandler handler = new ConstraintHandler(true);
        handler.instantiate(constraintTemplate, schema);
        EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = handler.evaluate(constraintTemplate, schema, data);
        handler.retain(constraintTemplate, schema, results);

        DataSchemaEntry<SimpleDataSchema> add = schema.findDataSchemaEntry("input.add");
        DataSchemaEntry<SimpleDataSchema> size = schema.findDataSchemaEntry("output.size");
        DataSchemaEntry<SimpleDataSchema> objectNumber = schema.findDataSchemaEntry("output.object.number");

        assertEquals(0, add.constraints.size());
        assertEquals(0,  size.constraints.size());
        assertEquals(2, objectNumber.constraints.size());

        Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(size)));
        Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(add)));

        Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(objectNumber)));
        Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(objectNumber), new DataReference(add)));

        Constraint constraint5 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(objectNumber)));
        Constraint constraint6 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(objectNumber), new DataReference(size)));

        assertEquals(0, results.getEvaluationResults().size());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
        assertEquals(2.0/3, constraintResults1.applicationRate());
        assertTrue(constraintResults1.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults2 = results.getPotentialConstraintResults(size, constraint2, data);
        assertEquals(2.0/3, constraintResults2.applicationRate());
        assertTrue(constraintResults2.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults3 = results.getPotentialConstraintResults(objectNumber, constraint3, data);
        assertEquals(1.0, constraintResults3.applicationRate());
        assertFalse(constraintResults3.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults4 = results.getPotentialConstraintResults(objectNumber, constraint4, data);
        assertEquals(1.0, constraintResults4.applicationRate());
        assertFalse(constraintResults4.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults5 = results.getPotentialConstraintResults(size, constraint5, data);
        assertEquals(2.0/3, constraintResults5.applicationRate());
        assertTrue(constraintResults5.foundCounterExample());

        ConstraintResults<Pair<DataObject, DataObject>> constraintResults6 = results.getPotentialConstraintResults(size, constraint6, data);
        assertEquals(2.0/3, constraintResults6.applicationRate());
        assertTrue(constraintResults6.foundCounterExample());
    }

    @Test
    public void differentRetentionPoliciesShouldNotAffectEachOther() {
        SimpleDataCollection data = SimpleDataCollection.parseData(
                "{value:1}",
                "{value:2}",
                "{value:3}"
        );

        SimpleDataSchema schema = data.deriveSchema(null);

        ConstraintTemplate validConstraint = new ConstraintTemplate(
                "valid",
                new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)),
                new NoViolationsPolicy()
        );

        ConstraintTemplate invalidConstraint = new ConstraintTemplate(
                "invalid",
                new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(3)),
                new MinApplicationsPolicy(1)
        );

        ConstraintHandler handler = new ConstraintHandler();

        handler.instantiate(validConstraint, schema);
        handler.instantiate(invalidConstraint, schema);
        EvaluationResults<SimpleDataSchema, DataObject> results = handler.evaluate(validConstraint, schema, data);

        handler.retain(validConstraint, schema, results);

        DataSchemaEntry<SimpleDataSchema> value = schema.getSchemaEntry("value");

        Constraint valid = new Constraint(new GreaterThanOrEqualOperator(
                                new DataReference(value),
                                new NumberLiteral(0)));

        Constraint invalid = new Constraint(new GreaterThanOrEqualOperator(
                                new DataReference(value),
                                new NumberLiteral(3)));

        assertTrue(value.constraints.contains(valid));
        assertFalse(value.constraints.contains(invalid));
        assertTrue(value.potentialConstraints.contains(invalid));

        handler.retain(invalidConstraint, schema, results);
        assertTrue(value.constraints.contains(invalid));
    }
}
