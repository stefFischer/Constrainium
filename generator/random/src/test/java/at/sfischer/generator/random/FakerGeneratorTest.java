package at.sfischer.generator.random;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.generator.random.faker.FakerGenerator;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FakerGeneratorTest {

    public static void assertFakerMethodMatches(
            DataSchemaEntry<?> entry,
            Function<Faker, Object> expectedFakerMethod
    ) {
        long seed = new Random().nextLong();
        Faker fakerGenerator = new Faker(new Random(seed));
        Faker fakerExpected = new Faker(new Random(seed));

        Object actual = FakerGenerator.generate(entry, fakerGenerator);
        if(expectedFakerMethod == null){
            assertNull(actual);
            return;
        }

        Object expected = expectedFakerMethod.apply(fakerExpected);

        assertNotNull(actual, "Generated value should not be null for entry: " + entry.getQualifiedName());
        assertEquals(expected, actual, "Generated value does not match expected Faker output for entry: " + entry.getQualifiedName());
    }

    @Test
    void shouldGenerateFirstNameForPersonFirstNameEntry() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> personEntry =
                schema.objectEntry("person", true);
        DataSchemaEntry<SimpleDataSchema> firstNameEntry =
                personEntry.dataSchema.stringEntry("first-name", true);

        assertFakerMethodMatches(
                firstNameEntry,
                f -> f.name().firstName()
        );
    }

    @Test
    void shouldGenerateLastNameForPersonLastNameEntry() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> personEntry =
                schema.objectEntry("person", true);
        DataSchemaEntry<SimpleDataSchema> lastNameEntry =
                personEntry.dataSchema.stringEntry("last-name", true);

        assertFakerMethodMatches(
                lastNameEntry,
                f -> f.name().lastName()
        );
    }

    @Test
    void shouldResolveCompanyNameFromContext() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> companyEntry =
                schema.objectEntry("company", true);
        DataSchemaEntry<SimpleDataSchema> nameEntry =
                companyEntry.dataSchema.stringEntry("name", true);

        assertFakerMethodMatches(
                nameEntry,
                f -> f.company().name()
        );
    }

    @Test
    void shouldGenerateBookTitleFromContext() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> bookEntry =
                schema.objectEntry("book", true);
        DataSchemaEntry<SimpleDataSchema> titleEntry =
                bookEntry.dataSchema.stringEntry("title", true);

        assertFakerMethodMatches(
                titleEntry,
                f -> f.book().title()
        );
    }

    @Test
    void jobTitleShouldGenerateJobTitle() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> job =
                schema.objectEntry("job", true);
        DataSchemaEntry<SimpleDataSchema> titleEntry =
                job.dataSchema.stringEntry("title", true);

        assertFakerMethodMatches(
                titleEntry,
                f -> f.job().title()
        );
    }

    @Test
    void shouldGenerateCityForAddressContext() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> addressEntry =
                schema.objectEntry("address", true);
        DataSchemaEntry<SimpleDataSchema> cityEntry =
                addressEntry.dataSchema.stringEntry("city", true);

        assertFakerMethodMatches(
                cityEntry,
                f -> f.address().city()
        );
    }

    @Test
    void shouldFallbackForUnknownAttribute() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> rootEntry =
                schema.objectEntry("object", true);
        DataSchemaEntry<SimpleDataSchema> unknownEntry =
                rootEntry.dataSchema.stringEntry("completely-unknown-field", true);

        assertFakerMethodMatches(
                unknownEntry,
                null
        );
    }

    @Test
    void productNameShouldNotResolveToPersonName() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> product =
                schema.objectEntry("product", true);
        DataSchemaEntry<SimpleDataSchema> name =
                product.dataSchema.stringEntry("name", true);

        assertFakerMethodMatches(
                name,
                f -> f.commerce().productName()
        );
    }

    @Test
    void shouldHandleCompositeFieldPersonName() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> personName =
                schema.stringEntry("personName", true);

        assertFakerMethodMatches(
                personName,
                f -> f.name().fullName()
        );
    }

    @Test
    void countryNameShouldGenerateCountryNameNested() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> country =
                schema.objectEntry("country", true);
        DataSchemaEntry<SimpleDataSchema> name =
                country.dataSchema.stringEntry("name", true);

        assertFakerMethodMatches(
                name,
                f -> f.country().name()
        );
    }

    @Test
    void countryNameShouldGenerateCountryName() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> entry =
                schema.stringEntry("countryName", true);

        assertFakerMethodMatches(
                entry,
                f -> f.address().country()
        );
    }

    @Test
    void customerNameShouldResolveToPersonName() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> customer =
                schema.objectEntry("customer", true);
        DataSchemaEntry<SimpleDataSchema> name =
                customer.dataSchema.stringEntry("name", true);

        assertFakerMethodMatches(
                name,
                f -> f.name().fullName()
        );
    }

    @Test
    void nestedCountryShouldUseAddressCountry() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> customer =
                schema.objectEntry("customer", true);
        DataSchemaEntry<SimpleDataSchema> address =
                customer.dataSchema.objectEntry("address", true);
        DataSchemaEntry<SimpleDataSchema> country =
                address.dataSchema.stringEntry("country", true);

        assertFakerMethodMatches(
                country,
                f -> f.address().country()
        );
    }

    @Test
    void postalCodeShouldMatchZipCode() {
        SimpleDataSchema schema = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> entry =
                schema.stringEntry("postalCode", true);

        assertFakerMethodMatches(
                entry,
                f -> f.address().postcode()
        );
    }
}