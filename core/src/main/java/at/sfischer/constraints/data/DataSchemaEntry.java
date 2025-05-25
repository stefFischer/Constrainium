package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.Type;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataSchemaEntry<T extends DataSchema> {
    public final String name;

    public Type type;

    public final boolean mandatory;

    public final T dataSchema;

    // Constraints that hae been confirmed.
    public final Set<Constraint> constraints;

    // Constraints that could be possible but still need confirmation.
    public final Set<Constraint> potentialConstraints;

    protected T parentSchema;

    public DataSchemaEntry(T parentSchema, String name, Type type, boolean mandatory, T dataSchema) {
        this.parentSchema = parentSchema;
        this.name = name;
        this.type = type;
        this.mandatory = mandatory;
        this.dataSchema = dataSchema;
        if(this.dataSchema != null){
            this.dataSchema.setParentEntry(this);
        }
        this.constraints = new HashSet<>();
        this.potentialConstraints = new HashSet<>();
    }

    public String getQualifiedName(){
        StringBuilder sb = new StringBuilder(this.name);
        DataSchemaEntry<T> parent = this.getParentSchemaEntry();
        while (parent != null) {
            sb.insert(0, ".");
            sb.insert(0, parent.name);
            parent = parent.getParentSchemaEntry();
        }

        return sb.toString();
    }

    public DataSchemaEntry<T> getParentSchemaEntry() {
        if(this.parentSchema == null){
            return null;
        }

        return this.parentSchema.getParentEntry();
    }

    public Constraint getPotentionConstraint(Constraint constraint){
        for (Constraint potentialConstraint : potentialConstraints) {
            if(potentialConstraint.equals(constraint)){
                return potentialConstraint;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSchemaEntry<?> that = (DataSchemaEntry<?>) o;
        return mandatory == that.mandatory && Objects.equals(name, that.name) && Objects.equals(getQualifiedName(), that.getQualifiedName()) && Objects.equals(type, that.type) && Objects.equals(dataSchema, that.dataSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, getQualifiedName(), type, mandatory, dataSchema);
    }

    @Override
    public String toString() {
        return toString("", "\t");
    }

    public String toString(String indent, String indentIncrease) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type).append(" (").append(mandatory ? "mandatory" : "optional").append(")");
        sb.append(" [").append(getQualifiedName()).append("]");
        sb.append(System.identityHashCode(this));
        if(!constraints.isEmpty()) {
            sb.append("\n");
            sb.append(indent);
            sb.append(indentIncrease);
            sb.append("- constraints:");
            sb.append("\n");
            AtomicBoolean first = new AtomicBoolean(true);
            constraints.forEach(c -> {
                if (!first.get()) {
                    sb.append("\n");
                }
                first.set(false);
                sb.append(indent);
                sb.append(indentIncrease);
                sb.append(indentIncrease);
                sb.append(c);
            });
        }
        if(!potentialConstraints.isEmpty()) {
            sb.append("\n");
            sb.append(indent);
            sb.append(indentIncrease);
            sb.append("- potentialConstraints:");
            sb.append("\n");
            AtomicBoolean first = new AtomicBoolean(true);
            potentialConstraints.forEach(c -> {
                if (!first.get()) {
                    sb.append("\n");
                }
                first.set(false);
                sb.append(indent);
                sb.append(indentIncrease);
                sb.append(indentIncrease);
                sb.append(c);
            });
        }
        return sb.toString();
    }
}
