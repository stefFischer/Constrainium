package at.sfischer.driver;

import java.util.List;

public class DriverConfigurationDescriptor {

    private final List<DriverConfigurationField> fields;

    public DriverConfigurationDescriptor(
            List<DriverConfigurationField> fields) {
        this.fields = fields;
    }

    public List<DriverConfigurationField> getFields() {
        return fields;
    }
}