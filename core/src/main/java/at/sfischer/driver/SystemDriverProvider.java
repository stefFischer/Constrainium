package at.sfischer.driver;

import java.util.Map;

public interface SystemDriverProvider {

    String getIdentifier();

    DriverConfigurationDescriptor getConfigurationDescriptor();

    SystemDriver create(Map<String, Object> configurationValues);
}
