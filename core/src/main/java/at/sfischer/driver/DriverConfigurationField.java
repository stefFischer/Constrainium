package at.sfischer.driver;

public class DriverConfigurationField {

    private final String name;
    private final Class<?> type;
    private final boolean required;
    private final String description;

    public DriverConfigurationField(
            String name,
            Class<?> type,
            boolean required,
            String description) {

        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }
}
