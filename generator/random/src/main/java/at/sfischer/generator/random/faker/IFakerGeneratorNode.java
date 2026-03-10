package at.sfischer.generator.random.faker;

import at.sfischer.constraints.data.DataSchemaEntry;
import net.datafaker.Faker;

import java.util.Set;

public interface IFakerGeneratorNode {

    Set<String> identifiers();

    Object generate(DataSchemaEntry<?> entry, Faker faker);
}
