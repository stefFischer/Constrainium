package at.sfischer.constraints.timeseries.operations;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.timeseries.Constants;
import at.sfischer.constraints.timeseries.MainTransformForecast;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TestTransformations {

    @Test
    public void testAddOffset() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                arrays.forEach(x, ARRAY_ELEMENT + 1)
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4]}");
        DataObject expected = DataObject.parseData("{x:[2.0,3.0,4.0,5.0]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testScale() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                arrays.forEach(x, ARRAY_ELEMENT * 2.0)
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4]}");
        DataObject expected = DataObject.parseData("{x:[2.0,4.0,6.0,8.0]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testSmoothing() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                timeseries.movingAverageSmoothing(x, round(arrays.length(x) - 2))
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4,5]}");
        DataObject expected = DataObject.parseData("{x:[1.0,1.5,2.0,3.0,4.0]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testAddLinearTrend() throws IOException, ParseException {
        String source = """
            function addLinearTrend(x, slope){
                arrays.combine(
                    x,
                    arrays.generate(ARRAY_INDEX * slope, arrays.length(x)),
                    ARRAY_LEFT + ARRAY_RIGHT
                )
            }
        
            MRS M1:
            transformations: {
                addLinearTrend(x, 1)
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4,5]}");
        DataObject expected = DataObject.parseData("{x:[1.0,3.0,5.0,7.0,9.0]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testClipOutliers() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                arrays.forEach(x, min(max(ARRAY_ELEMENT, 1), 15))
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[-1,1,2,20,4,5]}");
        DataObject expected = DataObject.parseData("{x:[1.0,1.0,2.0,15.0,4.0,5.0]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testReverse() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                arrays.reverse(x)
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4,5]}");
        DataObject expected = DataObject.parseData("{x:[5,4,3,2,1]}");

        DataObject transformed = performTransformation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testAddNoise() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                timeseries.addGaussianNoise(x, 0.1 * arrays.standardDeviation(x))
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,-1,1,-1]}");

        DataObject transformed = performTransformation(source, data);

        Number[] result = assertInstanceOf(Number[].class, transformed.getDataValue("x").getValue());
        assertEquals(4, result.length);
    }

    @Test
    public void testAddOutliers() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {
                timeseries.addOutliers(x, arrays.length(x) / 2, 1)
            }
            validations: {a <= b}
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4,5]}");

        DataObject transformed = performTransformation(source, data);
        Number[] result = assertInstanceOf(Number[].class, transformed.getDataValue("x").getValue());
        assertEquals(5, result.length);
    }

    private void assertEqualDataObjects(DataObject expected, DataObject actual) {
        Comparator<Number> numberComparator = Comparator.comparing(a -> new BigDecimal(a.toString()));
        assertThat(actual)
                .usingRecursiveComparison()
                .withComparatorForType(numberComparator, Number.class)
                .withComparatorForType(numberComparator, Double.class)
                .withComparatorForType(numberComparator, Float.class)
                .withComparatorForType(numberComparator, BigDecimal.class)
                .withComparatorForType(numberComparator, Integer.class)
                .withComparatorForType(numberComparator, Long.class)
                .isEqualTo(expected);
    }

    private static DataObject performTransformation(String source, SimpleDataCollection data) throws IOException, ParseException {
        SimpleDataSchema inputSchema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> x = inputSchema.numberArrayEntry("x", true);
        EvaluationResults<SimpleDataSchema, DataObject> evaluationResults = inputSchema.evaluate(data);
        assertTrue(evaluationResults.getEvaluationResults().isEmpty());
        ConstraintTemplateFile file = Constants.parse(source);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);
        MetamorphicRelationSetTemplate relation = relations.getFirst();
        assertNotNull(relation);
        NamedExpression expression = relation.getTransformations().getFirst();
        Node transformation = expression.expression();
        transformation = transformation.setVariableNameValue("x", new DataReference(x));

        DataObject original = data.getDataCollection().getFirst();
        return MainTransformForecast.transform(transformation, original, "x");
    }
}
