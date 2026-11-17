package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DefaultArrayTypePromotionPolicy;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

public class TraceonTypePromotionPolicy extends DefaultArrayTypePromotionPolicy {

    @Override
    public Type promote(Type type1, Type type2) {
        Type superResults = super.promote(type1, type2);
        if(superResults != null){
            return superResults;
        }

        return TypeEnum.ANY;
    }
}
