package at.sfischer.constraints.model.operators.objects;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class Reference extends Function  {

    private static final String FUNCTION_NAME = "objects.reference";

    public Reference(Node object, StringLiteral reference) {
        super(FUNCTION_NAME, object, reference);
    }

    @Override
    public Node evaluate() {
        ComplexValue object = getComplexValue(0);
        String reference = getStringArgument(1);

        if(object != null && reference != null){
            DataObject dataObject = object.getValue();
            Value<?> value = getLiteralValue(dataObject, reference);
            if(value != null){
                return value;
            }
        }

        return this;
    }

    public static Value<?> getLiteralValue(DataObject object, String reference){
        return getLiteralValue(object, reference.split("\\."));
    }

    public static Value<?> getLiteralValue(DataObject object, String[] reference){
        DataObject curObject = object;
        for (int i = 0; i < reference.length; i++) {
            String fieldName = reference[i];
            DataValue<?> value = curObject.getDataValue(fieldName);
            if(value == null){
                break;
            }

            Value<?> literal = value.getLiteralValue();
            if(i == reference.length - 1){
                return literal;
            }

            if(literal instanceof ComplexValue){
                curObject = ((ComplexValue) literal).getValue();
                continue;
            }

            break;
        }

        return null;
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.COMPLEXTYPE, TypeEnum.STRING);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.ANY;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Reference(getParameter(0).setVariableValues(values), (StringLiteral) getParameter(1));
    }
}
