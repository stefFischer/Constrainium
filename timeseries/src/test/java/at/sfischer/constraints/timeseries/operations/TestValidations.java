package at.sfischer.constraints.timeseries.operations;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.timeseries.Constants;
import at.sfischer.constraints.timeseries.MainComputeProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestValidations {

    @Test
    public void testAmplitude() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                arrays.max(x) - arrays.min(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,3,4]}");
        DataObject expected = DataObject.parseData("{property:3.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testAbsoluteDerivative() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.averageAbsoluteDerivative(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,0,1,0]}");
        DataObject expected = DataObject.parseData("{property:1.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testLinearTrend() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.linearTrend(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,3,4]}");
        DataObject expected = DataObject.parseData("{property:1.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testAutoCorrelation() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.autoCorrelation(x, 1)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,3,4]}");
        DataObject expected = DataObject.parseData("{property:0.4}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testSkewness() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.skewness(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,1]}");
        DataObject expected = DataObject.parseData("{property:0.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testKurtosis() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.pearsonKurtosis(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,1]}");
        DataObject expected = DataObject.parseData("{property:2.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testDominantFrequency() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.dominantFrequency(x, 5, 10)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,1,0,-1,-2,-1,0]}");
        DataObject expected = DataObject.parseData("{property:2.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testSpectralCentroid() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.spectralCentroid(x, 4, 10)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[0,1,2,1,0,-1,-2,-1,0]}");
        DataObject expected = DataObject.parseData("{property:2.75}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testZeroCrossingRate() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.levelCrossingRate(x, 0)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,-1,1,-1]}");
        DataObject expected = DataObject.parseData("{property:1.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testMeanCrossingRate() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.levelCrossingRate(x, arrays.average(x))
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,1,2]}");
        DataObject expected = DataObject.parseData("{property:1.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
    }

    @Test
    public void testTurningPointRate() throws IOException, ParseException {
        String source = """
            MRS M1:
            transformations: {x + 1}
            validations: {
                timeseries.turningPointRate(x)
            }
        """;

        SimpleDataCollection data = SimpleDataCollection.parseData("{x:[1,2,1,2]}");
        DataObject expected = DataObject.parseData("{property:1.0}");

        DataObject transformed = performValidation(source, data);

        assertEqualDataObjects(expected, transformed);
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

    private static DataObject performValidation(String source, SimpleDataCollection data) throws IOException, ParseException {
        SimpleDataSchema inputSchema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> x = inputSchema.numberArrayEntry("x", true);
        EvaluationResults<SimpleDataSchema, DataObject> evaluationResults = inputSchema.evaluate(data);
        assertTrue(evaluationResults.getEvaluationResults().isEmpty());
        ConstraintTemplateFile file = Constants.parse(source);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);
        MetamorphicRelationSetTemplate relation = relations.getFirst();
        assertNotNull(relation);
        NamedExpression expression = relation.getValidations().getFirst();
        Node validation = expression.expression();
        validation = validation.setVariableNameValue("x", new DataReference(x));

        DataObject original = data.getDataCollection().getFirst();
        DataObject propertyHolder = new DataObject();
        MainComputeProperties.transform(validation, original, "property", propertyHolder);
        return propertyHolder;
    }
}
