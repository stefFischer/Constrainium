package at.sfischer.generator.random;

import at.sfischer.ConfigurationDescriptor;
import at.sfischer.ConfigurationField;
import at.sfischer.generator.InputGenerator;
import at.sfischer.generator.InputGeneratorProvider;

import java.util.List;
import java.util.Map;

public class RandomGeneratorProvider implements InputGeneratorProvider {

    public static final String GENERATOR_NAME = "random";

    @Override
    public String getIdentifier() {
        return GENERATOR_NAME;
    }

    @Override
    public ConfigurationDescriptor getConfigurationDescriptor() {
        return new ConfigurationDescriptor(List.of(
                new ConfigurationField(
                        "seed",
                        Long.class,
                        false,
                        "Seed for random generation"
                ),
                new ConfigurationField(
                        "numberGenerationStrategy",
                        String.class,
                        false,
                        "Strategy for random number generation (One of: SMALL, MEDIUM, FULL, BOUNDARIES)"
                )
        ));
    }

    @Override
    public InputGenerator create(Map<String, Object> configurationValues) {
        Long seed = (Long) configurationValues.get("seed");
        String numberGenerationStrategy = (String) configurationValues.get("numberGenerationStrategy");
        RandomNumberGenerationStrategy strategy = RandomNumberGenerationStrategy.getFromName(numberGenerationStrategy);
        return new RandomInputGenerator(seed, strategy);
    }
}
