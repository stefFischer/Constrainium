package at.sfischer;

import java.util.List;

public class ConfigurationDescriptor {

    private final List<ConfigurationField> fields;

    public ConfigurationDescriptor(
            List<ConfigurationField> fields) {
        this.fields = fields;
    }

    public List<ConfigurationField> getFields() {
        return fields;
    }
}