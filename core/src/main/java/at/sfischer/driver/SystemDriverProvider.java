package at.sfischer.driver;

import at.sfischer.ConfigurationDescriptor;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public interface SystemDriverProvider {

    String getIdentifier();

    ConfigurationDescriptor getConfigurationDescriptor();

    SystemDriver create(Map<String, Object> configurationValues);


    static List<SystemDriverProvider> providers() {
        return ServiceLoader.load(SystemDriverProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    static SystemDriverProvider getProvider(String identifier) {
        return providers().stream()
                .filter(p -> p.getIdentifier().equals(identifier))
                .findFirst()
                .orElseThrow();
    }
}
