package at.sfischer.constraints;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MetamorphicRelationHandlerTest {

    @Test
    public void instantiateMetamorphicRelation() {
        Node transformation = new AdditionOperator(
                new Variable("a"),
                new NumberLiteral(1)
        );
        Node validation = new LessThanOrEqualOperator(
                new Variable("a"),
                new Variable("n")
        );
        MetamorphicRelationTemplate template =
                new MetamorphicRelationTemplate(
                        "MR1",
                        transformation,
                        validation,
                        new NoViolationsPolicy()
                );

        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:1, object:{number:0}}")
        );

        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(null);

        MetamorphicRelationHandler handler = new MetamorphicRelationHandler(null);
        handler.instantiate(template, schema);

        DataSchemaEntry<SimpleDataSchema> size = schema.findDataSchemaEntry("output.size");
        DataSchemaEntry<SimpleDataSchema> number = schema.findDataSchemaEntry("output.object.number");

        assertEquals(4, size.potentialConstraints.size());
        assertEquals(4, number.potentialConstraints.size());

        for (IConstraint potentialConstraint : size.potentialConstraints) {
            assertInstanceOf(MetamorphicRelation.class, potentialConstraint);
        }
        for (IConstraint potentialConstraint : number.potentialConstraints) {
            assertInstanceOf(MetamorphicRelation.class, potentialConstraint);
        }

        for (IConstraint mr1 : size.potentialConstraints) {
            for (IConstraint mr2 : number.potentialConstraints) {
                assertEquals(((MetamorphicRelation)mr1).getTransformation(), ((MetamorphicRelation)mr2).getTransformation());
            }
        }
    }

    @Test
    public void evaluateMetamorphicRelation() throws DriverException {
        Node transformation = new AdditionOperator(
                new Variable("a"),
                new NumberLiteral(1));
        Node validation = new LessThanOrEqualOperator(
                new Variable("old"),
                new Variable("new"));
        MetamorphicRelationTemplate template =
                new MetamorphicRelationTemplate(
                        "MR1",
                        transformation,
                        validation,
                        new NoViolationsPolicy());

        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}")
        );
        InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(null);

        SystemDriver driver = Mockito.mock(SystemDriver.class);
        when(driver.execute(DataObject.parseData("{input:{value:2.0}}")))
                .thenReturn(DataObject.parseData("{result:3}"));
        when(driver.execute(DataObject.parseData("{input:{value:3.0}}")))
                .thenReturn(DataObject.parseData("{result:4}"));

        MetamorphicRelationHandler handler = new MetamorphicRelationHandler(driver);
        handler.instantiate(template, schema);

        EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = handler.evaluate(template, schema, data);
        verify(driver, times(2)).execute((DataObject)any());
        assertNotNull(results);

        verify(driver).execute(DataObject.parseData("{input:{value:2.0}}"));
        verify(driver).execute(DataObject.parseData("{input:{value:3.0}}"));

        assertNotNull(results);
        assertEquals(0, results.getEvaluationResults().size());

        DataSchemaEntry<SimpleDataSchema> result = schema.findDataSchemaEntry("output.result");
        assertEquals(0, result.constraints.size());
        assertEquals(2, result.potentialConstraints.size());

        Map<DataSchemaEntry<SimpleDataSchema>, Set<ConstraintResults<Pair<DataObject, DataObject>>>> mrResults = results.getPotentialConstraintResults();
        Set<ConstraintResults<Pair<DataObject, DataObject>>> constraintResults = mrResults.get(result);

        assertTrue(constraintResults.stream().anyMatch(res -> res.applicationRate() == 1.0 && !res.foundCounterExample()));
        assertTrue(constraintResults.stream().anyMatch(res -> res.applicationRate() == 0.0 && res.foundCounterExample()));

        handler.retain(template, schema, results);

        assertEquals(1, result.constraints.size());
        assertEquals(0, result.potentialConstraints.size());
    }
}
