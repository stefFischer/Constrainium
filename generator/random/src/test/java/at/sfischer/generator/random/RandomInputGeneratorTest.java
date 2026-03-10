package at.sfischer.generator.random;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.generator.InputGenerator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RandomInputGeneratorTest {

    @Test
    public void testGeneratorWithNestedSchema() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.numberEntry("size", true);
        schema.booleanEntry("isEmpty", true);

        DataSchemaEntry<SimpleDataSchema> objectEntry = schema.objectEntry("object", true);
        objectEntry.dataSchema.numberEntry("id", true);
        DataSchemaEntry<SimpleDataSchema> schemaEntry = objectEntry.dataSchema.stringEntry("value", true);

        Stream<DataObject> stream = inputGenerator.generate(schema);

        DataObject generatedObject = stream.limit(1).findFirst().orElseThrow();

        assertNotNull(generatedObject.getDataValue("size"));
        assertNotNull(generatedObject.getDataValue("isEmpty"));
        assertNotNull(generatedObject.getDataValue("object"));

        Object object = generatedObject.getDataValue("object").getValue();
        assertInstanceOf(DataObject.class, object);

        DataObject objectData = (DataObject) object;

        assertNotNull(objectData.getDataValue("id"));
        assertNotNull(objectData.getDataValue("value"));
    }

    @Test
    public void generateMultipleInputsTest() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();

        // Top-level objects
        DataSchemaEntry<SimpleDataSchema> person = schema.objectEntry("person", true);
        DataSchemaEntry<SimpleDataSchema> company = schema.objectEntry("company", true);
        DataSchemaEntry<SimpleDataSchema> book = schema.objectEntry("book", true);
        DataSchemaEntry<SimpleDataSchema> unknown = schema.objectEntry("unknownEntity", true);

        // --- Person object ---
        person.dataSchema.stringEntry("firstName", true);
        person.dataSchema.stringEntry("lastName", true);
        person.dataSchema.stringEntry("email", true);
        person.dataSchema.stringEntry("birthDate", true);
        person.dataSchema.stringEntry("nickname", true);

        // --- Company object ---
        company.dataSchema.stringEntry("name", true);
        company.dataSchema.stringEntry("country", true);
        company.dataSchema.stringEntry("registrationCode", true);
        company.dataSchema.stringEntry("phone", true);

        // --- Book object ---
        book.dataSchema.stringEntry("title", true);
        book.dataSchema.stringEntry("isbn", true);
        book.dataSchema.stringEntry("summary", true);
        book.dataSchema.stringEntry("publisher", true);

        // --- Unknown entity (no known faker mapping) ---
        unknown.dataSchema.stringEntry("foo", true);
        unknown.dataSchema.stringEntry("bar", true);

        // --- Nested structures inside person ---
        DataSchemaEntry<SimpleDataSchema> address = person.dataSchema.objectEntry("address", true);
        address.dataSchema.stringEntry("street", true);
        address.dataSchema.stringEntry("city", true);
        address.dataSchema.stringEntry("zipcode", true);
        address.dataSchema.stringEntry("metadata", true);

        int numberOfGeneratedInputs = 10;
        Stream<DataObject> stream = inputGenerator.generate(schema);

        SimpleDataCollection dataCollection = new SimpleDataCollection();
        stream.limit(numberOfGeneratedInputs).forEach(dataCollection::addDataEntry);

        assertEquals(numberOfGeneratedInputs, dataCollection.size());
        assertEquals(dataCollection.deriveSchema(), schema);
    }

    @Test
    public void testGeneratorWithNumberArray() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.numberEntry("size", true);
        schema.numberArrayEntry("numbers", true);

        Stream<DataObject> stream = inputGenerator.generate(schema);
        DataObject generated = stream.limit(1).findFirst().orElseThrow();

        assertNotNull(generated.getDataValue("size"));
        assertNotNull(generated.getDataValue("numbers"));

        Object numbersObj = generated.getDataValue("numbers").getValue();
        Number[] numbers = (Number[]) numbersObj;
        for (Number n : numbers) {
            assertInstanceOf(Number.class, n);
        }
    }

    @Test
    public void testGeneratorWithStringArray() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.stringArrayEntry("tags", true);

        Stream<DataObject> stream = inputGenerator.generate(schema);
        DataObject generated = stream.limit(1).findFirst().orElseThrow();

        Object tagsObj = generated.getDataValue("tags").getValue();
        String[] strings = (String[]) tagsObj;
        for (String n : strings) {
            assertInstanceOf(String.class, n);
        }
    }

    @Test
    public void testGeneratorWithBooleanArray() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.booleanArrayEntry("flags", true);

        Stream<DataObject> stream = inputGenerator.generate(schema);
        DataObject generated = stream.limit(1).findFirst().orElseThrow();

        Object flagsObj = generated.getDataValue("flags").getValue();
        Boolean[] booleans = (Boolean[]) flagsObj;
        for (boolean n : booleans) {
            assertInstanceOf(Boolean.class, n);
        }
    }
    @Test
    public void testGeneratorWithObjectArray() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> users = schema.objectArrayEntry("users", true);

        users.dataSchema.stringEntry("name", true);
        users.dataSchema.numberEntry("age", true);

        Stream<DataObject> stream = inputGenerator.generate(schema);
        DataObject generated = stream.limit(1).findFirst().orElseThrow();

        Object usersObj = generated.getDataValue("users").getValue();
        DataObject[] objects = (DataObject[]) usersObj;
        for (DataObject user : objects) {
            assertInstanceOf(DataObject.class, user);
            assertNotNull(user.getDataValue("name"));
            assertNotNull(user.getDataValue("age"));
        }
    }

    @Test
    public void testGeneratorWithNestedArrays() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of());

        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> matrix = schema.arrayEntryFor(
                new ArrayType(TypeEnum.NUMBER), "matrix", true
        );

        Stream<DataObject> stream = inputGenerator.generate(schema);
        DataObject generated = stream.limit(1).findFirst().orElseThrow();

        Object matrixObj = generated.getDataValue("matrix").getValue();
        DataValue<?>[] values = (DataValue<?>[]) matrixObj;
        for (DataValue<?> value : values) {
            Object innerValue = value.getValue();
            assertInstanceOf(Number[].class, innerValue);
        }
    }

    @Test
    public void testSmallNumberGeneration() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of(
                "numberGenerationStrategy", "SMALL"
        ));

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.numberEntry("number", true);

        int numberOfGeneratedInputs = 100;
        Stream<DataObject> stream = inputGenerator.generate(schema);

        stream.limit(numberOfGeneratedInputs).forEach(
                dataObject -> {
                    Object numberObject = dataObject.getDataValue("number").getValue();
                    assertInstanceOf(Number.class, numberObject);
                    Number number = (Number) numberObject;
                    assertTrue(number.doubleValue() < 100);
                }
        );
    }

    @Test
    public void testMediumIntegerGeneration() {
        RandomGeneratorProvider generatorProvider = new RandomGeneratorProvider();
        InputGenerator inputGenerator = generatorProvider.create(Map.of(
                "numberGenerationStrategy", "MEDIUM"
        ));

        SimpleDataSchema schema = new SimpleDataSchema();
        schema.integerEntry("number", true);

        int numberOfGeneratedInputs = 100;
        Stream<DataObject> stream = inputGenerator.generate(schema);

        stream.limit(numberOfGeneratedInputs).forEach(
                dataObject -> {
                    Object numberObject = dataObject.getDataValue("number").getValue();
                    assertInstanceOf(Integer.class, numberObject);
                    Integer number = (Integer) numberObject;
                    assertTrue(number < 10_000);
                }
        );
    }
}
