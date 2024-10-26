package at.sfischer.constraints.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArrayValues<T extends Literal<?>> extends Literal<T[]> {

    private final Type elementType;

    public ArrayValues(Type elementType, T[] values) {
        super(values);
        this.elementType = elementType;
    }

    public void setValue(int i, T value){
        this.value[i] = value;
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(getElementType());
    }

    public Type getElementType(){
        return elementType;
    }

    @Override
    public boolean validate() {
        for (T val : getValue()) {
            if(!val.getReturnType().equals(elementType)){
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayValues<?> that = (ArrayValues<?>) o;
        if(!Objects.equals(elementType, that.elementType)) return false;
        return Arrays.equals(this.getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elementType);
    }

    public static ArrayValues<?> createArrayValuesFromList(List<Node> literals){
        if(literals.isEmpty()){
            return new ArrayValues<>(TypeEnum.ANY, new BooleanLiteral[0]);
        }

        Type elementType = null;
        for (Node literal : literals) {
            if(elementType == null){
                elementType = literal.getReturnType();
            } else if(!elementType.equals(literal.getReturnType())) {
                throw new IllegalStateException("Element types are inconsistent: " + elementType + " and " + literal.getReturnType());
            }
        }

        if(elementType == TypeEnum.NUMBER){
            NumberLiteral[] literalArray = new NumberLiteral[literals.size()];
            for (int i = 0; i < literals.size(); i++) {
                literalArray[i] = new NumberLiteral(((NumberLiteral)literals.get(i)).getValue());
            }
            return new ArrayValues<>(TypeEnum.NUMBER, literalArray);
        } else if(elementType == TypeEnum.BOOLEAN){
            BooleanLiteral[] literalArray = new BooleanLiteral[literals.size()];
            for (int i = 0; i < literals.size(); i++) {
                literalArray[i] = new BooleanLiteral(((BooleanLiteral)literals.get(i)).getValue());
            }
            return new ArrayValues<>(TypeEnum.BOOLEAN, literalArray);
        } else if(elementType == TypeEnum.STRING){
            StringLiteral[] literalArray = new StringLiteral[literals.size()];
            for (int i = 0; i < literals.size(); i++) {
                literalArray[i] = new StringLiteral(((StringLiteral)literals.get(i)).getValue());
            }
            return new ArrayValues<>(TypeEnum.STRING, literalArray);
        }

        throw new UnsupportedOperationException("Unsupported element type: " + elementType);
    }
}
