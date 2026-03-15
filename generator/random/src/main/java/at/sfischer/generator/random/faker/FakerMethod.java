package at.sfischer.generator.random.faker;

import at.sfischer.constraints.data.DataSchemaEntry;
import net.datafaker.Faker;

import java.util.Set;
import java.util.function.Function;

public class FakerMethod implements IFakerGeneratorNode {

    private final Set<String> identifiers;
    private final Function<Faker, Object> generator;

    protected FakerMethod(Set<String> identifiers, Function<Faker, Object> generator) {
        this.identifiers = identifiers;
        this.generator = generator;
    }

    @Override
    public Set<String> identifiers() {
        return identifiers;
    }

    @Override
    public Object generate(DataSchemaEntry<?> entry, Faker faker) {
        return generator.apply(faker);
    }
}
