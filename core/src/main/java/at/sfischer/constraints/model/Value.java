package at.sfischer.constraints.model;

import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class Value<T> implements Node {

    protected T value;

    public Value(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public Node evaluate() {
        return this;
    }

    @Override
    public void validate(ValidationContext context) {
        if(getValue() == null)
            context.error(this, "Value cannot be null.");
    }

    @Override
    public List<Node> getChildren() {
        return null;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value<?> value = (Value<?>) o;
        return Objects.equals(this.value, value.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if(value.getClass().isArray()){
            return getClass().getSimpleName() + "{value: " + Arrays.deepToString((Object[]) value) + '}';
        }

        return getClass().getSimpleName() + "{value: " + value + '}';
    }
}
