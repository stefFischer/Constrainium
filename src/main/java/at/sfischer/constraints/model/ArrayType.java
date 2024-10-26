package at.sfischer.constraints.model;

import java.util.Objects;

public record ArrayType(Type elementType) implements Type {


    @Override
    public boolean canAssignTo(Type target) {
        if(!(target instanceof ArrayType targetArrayType)){
            return false;
        }

        return targetArrayType.elementType().canAssignTo(this.elementType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(elementType, arrayType.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }
}
