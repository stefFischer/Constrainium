package at.sfischer.constraints.data;

import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

public class DefaultArrayTypePromotionPolicy extends DefaultTypePromotionPolicy {

    @Override
    public Type promote(Type type1, Type type2) {
        Type superResults = super.promote(type1, type2);
        if(superResults != null){
            return superResults;
        }

        if(type1 instanceof ArrayType && ((ArrayType) type1).elementType().equals(type2)){
            return type1;
        }

        if(type2 instanceof ArrayType && ((ArrayType) type2).elementType().equals(type1)){
            return type2;
        }

        return null;
    }
}
