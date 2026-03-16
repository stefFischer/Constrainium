package at.sfischer.testing;

import at.sfischer.constraints.ConstraintTemplate;
import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.constraints.model.validation.ValidationContext;
import at.sfischer.constraints.parser.ConstraintDslParser;
import at.sfischer.constraints.parser.ConstraintDslScanner;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.driver.SystemDriverProvider;
import at.sfischer.driver.rest.RestDriverProvider;
import at.sfischer.generator.InputGenerator;
import at.sfischer.generator.InputGeneratorProvider;
import at.sfischer.generator.random.RandomGeneratorProvider;
import at.sfischer.testing.systems.PayrollApplication;
import org.javatuples.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static at.sfischer.testing.SchemaAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PayrollApplicationIntegrationTest {

    private static PayrollApplication TEST_SYSTEM;

    @BeforeAll
    public static void setup(){
        TEST_SYSTEM = new PayrollApplication();
        TEST_SYSTEM.start();
    }

    @AfterAll
    public static void cleanup(){
        TEST_SYSTEM.stop();
    }

    @Test
    public void testRandomGenerationBlackboxExecution() throws Exception {
        int numberOfInputsToGenerate = 50;

        InputGeneratorProvider inputGeneratorProvider = InputGeneratorProvider.getProvider(RandomGeneratorProvider.GENERATOR_NAME);
        InputGenerator inputGenerator = inputGeneratorProvider.create(Map.of());
        assertNotNull(inputGenerator);

        SystemDriverProvider systemDriverProvider = SystemDriverProvider.getProvider(RestDriverProvider.DRIVER_NAME);
        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/employee",
                "operation", "POST",
                "openApiSpec", "http://localhost:8080/v3/api-docs"
        );
        SystemDriver systemDriver = systemDriverProvider.create(configValues);
        assertNotNull(systemDriver);

        // TODO This schema should be derived from the REST specification.
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> body =  schema.objectEntry("employee", true);
        body.dataSchema.stringEntry("name", true);
        body.dataSchema.stringEntry("role", true);

        InOutputDataCollection inout = new InOutputDataCollection();
        inputGenerator.generate(schema).limit(numberOfInputsToGenerate).forEach(in -> {
            try {
                DataObject out = systemDriver.execute(in);
                inout.addDataEntry(in, out);
            } catch (DriverException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(numberOfInputsToGenerate, inout.size());

        DataSchema resultSchema = inout.deriveSchema();
        assertInstanceOf(InOutputDataSchema.class, resultSchema);
        InOutputDataSchema<SimpleDataSchema> fullSchema = (InOutputDataSchema<SimpleDataSchema>) resultSchema;

        DataSchemaEntry<SimpleDataSchema> employeeEntry = fullSchema.getInputSchema().getSchemaEntry("employee");
        DataSchemaEntry<SimpleDataSchema> employeeRoleEntry = employeeEntry.dataSchema.getSchemaEntry("role");
        DataSchemaEntry<SimpleDataSchema> employeeNameEntry = employeeEntry.dataSchema.getSchemaEntry("name");

        DataSchemaEntry<SimpleDataSchema> idEntry = fullSchema.getOutputSchema().getSchemaEntry("id");
        DataSchemaEntry<SimpleDataSchema> roleEntry = fullSchema.getOutputSchema().getSchemaEntry("role");
        DataSchemaEntry<SimpleDataSchema> nameEntry = fullSchema.getOutputSchema().getSchemaEntry("name");

        assertNotNull(employeeEntry);
        assertNotNull(employeeRoleEntry);
        assertNotNull(employeeNameEntry);
        assertNotNull(idEntry);
        assertNotNull(roleEntry);
        assertNotNull(nameEntry);

        assertEquals(TypeEnum.COMPLEXTYPE, employeeEntry.type);
        assertEquals(TypeEnum.STRING, employeeRoleEntry.type);
        assertEquals(TypeEnum.STRING, employeeNameEntry.type);

        assertEquals(TypeEnum.STRING, roleEntry.type);
        assertEquals(TypeEnum.STRING, nameEntry.type);
        assertEquals(TypeEnum.INTEGER, idEntry.type);

        String input = """
            constraint C1:
                string.equals(a, b)

            constraint C2:
                a > 0

            constraint C3:
                a == 42
            """;

        ConstraintTemplateFile file = parse(input);

        // TODO We need to create a trimming feature to remove equivalent constraints (e.g., a == b, b == a).
        for (ConstraintTemplate constraint : file.getConstraints()) {
            ValidationContext context = new ValidationContext();
            constraint.getTerm().validate(context);
            assertTrue(context.isValid());

            fullSchema.fillSchemaWithConstraints(constraint.getTerm());

            fullSchema.getInputSchema().fillSchemaWithConstraints(constraint.getTerm());
            fullSchema.getOutputSchema().fillSchemaWithConstraints(constraint.getTerm());
        }

        assertConstraintNumber(employeeEntry, 0);
        assertConstraintNumber(employeeRoleEntry, 0);
        assertConstraintNumber(employeeNameEntry, 0);
        assertConstraintNumber(idEntry, 0);
        assertConstraintNumber(roleEntry, 0);
        assertConstraintNumber(nameEntry, 0);

        assertPotentialConstraintNumber(idEntry, 2);
        assertPotentialConstraintNumber(roleEntry, 5);
        assertPotentialConstraintNumber(nameEntry, 5);


        EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = fullSchema.evaluate(inout);
        fullSchema.applyConstraintRetentionPolicy(results, new NoViolationsPolicy());


        assertPotentialConstraintNumber(employeeEntry, 0);
        assertPotentialConstraintNumber(employeeRoleEntry, 0);
        assertPotentialConstraintNumber(employeeNameEntry, 0);
        assertPotentialConstraintNumber(idEntry, 0);
        assertPotentialConstraintNumber(roleEntry, 0);
        assertPotentialConstraintNumber(nameEntry, 0);

        assertConstraintNumber(idEntry, 1);
        assertConstraintNumber(roleEntry, 2);
        assertConstraintNumber(nameEntry, 2);

        assertConstraintType(idEntry, GreaterThanOperator.class, "Expected invariant id > 0");
        assertNoConstraintType(idEntry, EqualOperator.class);

        // TODO We need to create a trimming feature to remove equivalent constraints (e.g., a == b, b == a).
        //  Then one of these should be removed.
        assertConstraint(roleEntry, new StringEquals(new DataReference(roleEntry), new DataReference(employeeRoleEntry)));
        assertConstraint(roleEntry, new StringEquals(new DataReference(employeeRoleEntry), new DataReference(roleEntry)));

        // TODO We need to create a trimming feature to remove equivalent constraints (e.g., a == b, b == a).
        //  Then one of these should be removed.
        assertConstraint(nameEntry, new StringEquals(new DataReference(nameEntry), new DataReference(employeeNameEntry)));
        assertConstraint(nameEntry, new StringEquals(new DataReference(employeeNameEntry), new DataReference(nameEntry)));
    }

    private ConstraintTemplateFile parse(String input) throws Exception {
        ConstraintDslScanner scanner =
                new ConstraintDslScanner(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
                );

        ConstraintDslParser parser = new ConstraintDslParser(scanner);
        return parser.parse();
    }
}
