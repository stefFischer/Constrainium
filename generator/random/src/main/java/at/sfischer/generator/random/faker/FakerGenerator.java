package at.sfischer.generator.random.faker;

import at.sfischer.constraints.data.DataSchemaEntry;
import net.datafaker.Faker;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class FakerGenerator {

    private static final FakerGeneratorNode root = new FakerGeneratorNode();

    static {
        root.nodes(
                method(f -> f.name().firstName(), "firstname", "givenname"),
                method(f -> f.name().lastName(), "lastname", "surname"),
                method(f -> f.name().fullName(), "fullname", "personname"),
                method(f -> f.gender().binaryTypes(),"gender", "sex"),
                method(f -> f.credentials().username(),"username", "login"),
                node("name").nodes(
                        method(f -> f.name().fullName(), "person", "customer", "employee"),
                        method(f -> f.commerce().productName(),"product", "productname", "item"),
                        method(f -> f.credentials().username(), "user"),
                        method(f -> f.company().name(), "company", "organisation", "organization"),
                        method(f -> f.country().name(), "country")
                ),
                method(f -> f.credentials().password(), "password", "passwd"),
                method(f -> f.phoneNumber().phoneNumber(),"phone", "phonenumber", "telephone", "mobile", "cell"),
                node("title").nodes(
                        method(f -> f.book().title(), "book"),
                        method(f -> f.job().title(), "job", "position")
                ),
                method(f -> f.job().title(),"jobtitle", "employeerole"),
                method(f -> f.code().isbn10(),"isbn", "isbn10"),
                method(f -> f.code().isbn13(),"isbn13"),
                method(f -> f.internet().emailAddress(), "emailAddress", "email"),
                method(f -> f.address().city(), "city", "town"),
                method(f -> f.address().zipCode(), "zipcode", "postalcode", "postcode"),
                method(f -> f.address().country(), "country", "nation"),
                method(f -> f.address().streetAddress(),"street", "streetaddress", "address"),
                method(f -> f.address().state(),"state", "province", "region"),
                method(f -> f.finance().creditCard(), "creditcard"),
                method(f -> f.finance().iban(),"iban", "bankaccount", "accountnumber"),
                method(f -> f.commerce().productName(),"product", "productname", "item"),
                method(f -> UUID.randomUUID().toString(),"uuid", "guid"),
                method(f -> f.internet().url(), "url", "link", "website"),
                method(f -> f.lorem().sentence(),"description", "comment", "message", "text", "summary"),
                method(f -> f.timeAndDate().birthday().toString(), "birthdate", "birthday"),
                method(f -> Instant.now().toString(),"timestamp")
        );
    }

    public static FakerGeneratorNode node(String... identifiers){
        return new FakerGeneratorNode(identifiers);
    }

    public static FakerMethod method(Function<Faker, Object> generator, String... identifiers){
        return new FakerMethod(Set.of(identifiers), generator);
    }

    public static Object generate(DataSchemaEntry<?> entry, Faker faker) {
        return root.generate(entry, faker);
    }
}
