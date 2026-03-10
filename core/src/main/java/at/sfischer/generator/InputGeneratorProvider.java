package at.sfischer.generator;

import at.sfischer.ConfigurationDescriptor;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public interface InputGeneratorProvider {

    String getIdentifier();

    ConfigurationDescriptor getConfigurationDescriptor();

    InputGenerator create(Map<String, Object> configurationValues);

    static List<InputGeneratorProvider> providers() {
        return ServiceLoader.load(InputGeneratorProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    static InputGeneratorProvider getProvider(String identifier) {
        return providers().stream()
                .filter(p -> p.getIdentifier().equals(identifier))
                .findFirst()
                .orElseThrow();
    }
}
