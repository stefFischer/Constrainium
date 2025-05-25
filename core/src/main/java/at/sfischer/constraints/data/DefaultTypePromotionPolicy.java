package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

public class DefaultTypePromotionPolicy implements TypePromotionPolicy {

    /**
     * Promotes two types to a common compatible type based on simple casting rules.
     * <p>
     * This implementation defines a promotion strategy where:
     * <ul>
     *   <li>If both types are equal, that type is returned.</li>
     *   <li>If one type is {@code STRING} and the other is {@code NUMBER} or {@code BOOLEAN},
     *       the result is {@code STRING}.</li>
     *   <li>If one type is {@code NUMBER} and the other is {@code BOOLEAN},
     *       the result is {@code STRING}.</li>
     *   <li>If the types cannot be promoted according to the above rules,
     *       {@code null} is returned.</li>
     * </ul>
     * This is useful in schema inference scenarios where some types can be
     * safely cast to more general types (like {@code STRING}), while others may be incompatible.
     *
     * @param type1 the first type to be promoted
     * @param type2 the second type to be promoted
     * @return the promoted {@code Type}, or {@code null} if no promotion rule applies
     */
    @Override
    public Type promote(Type type1, Type type2) {
        if (type1.equals(type2)) return type1;
        if (
            (type1 == TypeEnum.STRING && type2 == TypeEnum.NUMBER) ||
            (type1 == TypeEnum.NUMBER && type2 == TypeEnum.STRING)
        )
            return TypeEnum.STRING;
        if (
                (type1 == TypeEnum.STRING && type2 == TypeEnum.BOOLEAN) ||
                        (type1 == TypeEnum.BOOLEAN && type2 == TypeEnum.STRING)
        )
            return TypeEnum.STRING;
        if (
                (type1 == TypeEnum.NUMBER && type2 == TypeEnum.BOOLEAN) ||
                        (type1 == TypeEnum.BOOLEAN && type2 == TypeEnum.NUMBER)
        )
            return TypeEnum.STRING;

        return null;
    }
}
