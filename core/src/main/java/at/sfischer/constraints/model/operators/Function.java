package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class Function implements Operator {

    private final String name;

    protected List<Node> parameters;

    public Function(String name, List<Node> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public Function(String name, Node... parameters) {
        this(name, Arrays.asList(parameters));
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean validate() {
        List<Type> parameterTypes = parameterTypes();
        if(this.parameters == null || this.parameters.size() != parameterTypes.size()){
            return false;
        }

        for (int i = 0; i < this.parameters.size(); i++) {
            Node parameter = this.parameters.get(i);
            if(!parameter.validate()){
                return false;
            }

            Type parameterType = parameterTypes.get(i);
            if(parameterType instanceof ArrayType){
                if(!(parameter.getReturnType() instanceof ArrayType)){
                    return false;
                }

                boolean isValidType = ((ArrayType) parameterType).elementType() == TypeEnum.ANY || ((ArrayType) parameter.getReturnType()).elementType() == ((ArrayType) parameterType).elementType() || ((ArrayType) parameter.getReturnType()).elementType() == TypeEnum.ANY;
                if(!isValidType){
                    return false;
                }

                continue;
            }

            boolean isValidType = parameterType == TypeEnum.ANY || parameter.getReturnType() == parameterType || parameter.getReturnType() == TypeEnum.ANY;
            if(!isValidType){
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return Objects.equals(name, function.name) && Objects.equals(parameters, function.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    public abstract List<Type> parameterTypes();

    @Override
    public List<Type> operandTypes() {
        return parameterTypes();
    }

    public Node getParameter(int i){
        if(i < 0 || i >= this.parameters.size()){
            return null;
        }

        return this.parameters.get(i);
    }

    protected void setParameter(int i, Node value){
        parameters.set(i, value);
    }

    protected String getStringArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof StringLiteral){
            return ((StringLiteral) arg).getValue();
        }

        return null;
    }

    protected Number getNumberArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof NumberLiteral){
            return ((NumberLiteral) arg).getValue();
        }

        return null;
    }

    protected ArrayValues<?> getArrayArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof ArrayValues<?>){
            return (ArrayValues<?>)arg;
        }

        return null;
    }

    protected ComplexValue getComplexValue(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof ComplexValue){
            return (ComplexValue)arg;
        }

        return null;
    }

    protected String[] getStringArrayArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof ArrayValues<?>){
            if(((ArrayValues<?>) arg).getElementType() == TypeEnum.STRING){
                //noinspection unchecked
                StringLiteral[] value = ((ArrayValues<StringLiteral>) arg).getValue();
                String[] val = new String[value.length];
                for (int j = 0; j < value.length; j++) {
                    if(value[j] != null){
                        val[j] = value[j].getValue();
                    } else {
                        val[j] = null;
                    }
                }

                return val;
            }
        }

        return null;
    }

    protected Number[] getNumberArrayArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof ArrayValues<?>){
            if(((ArrayValues<?>) arg).getElementType() == TypeEnum.NUMBER){
                //noinspection unchecked
                NumberLiteral[] value = ((ArrayValues<NumberLiteral>) arg).getValue();
                Number[] val = new Number[value.length];
                for (int j = 0; j < value.length; j++) {
                    if(value[j] != null){
                        val[j] = value[j].getValue();
                    } else {
                        val[j] = null;
                    }
                }

                return val;
            }
        }

        return null;
    }

    protected ArrayValues<?>[] getNestedArrayArgument(int i){
        Node arg = getParameter(i).evaluate();
        setParameter(i, arg);
        if(arg instanceof ArrayValues<?>){
            if(((ArrayValues<?>) arg).getElementType() instanceof ArrayType){
                //noinspection unchecked
                ArrayValues<?>[] value = ((ArrayValues<ArrayValues<?>>) arg).getValue();
                ArrayValues<?>[] val = new ArrayValues[value.length];
                for (int j = 0; j < value.length; j++) {
                    if(value[j] != null){
                        val[j] = value[j];
                    } else {
                        val[j] = null;
                    }
                }

                return val;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return name +"(parameters=" + parameters + ')';
    }
}
