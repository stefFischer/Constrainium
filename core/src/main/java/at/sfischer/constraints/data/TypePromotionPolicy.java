package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Type;

public interface TypePromotionPolicy {

    /**
     * Determines the promoted type when merging two potentially conflicting types.
     * <p>
     * This method is used during schema inference to resolve type inconsistencies
     * in data examples. Implementations define the rules for how types should be
     * promoted (e.g., promoting both integers and strings to strings).
     * </p>
     *
     * @param type1 the first observed type
     * @param type2 the second observed type
     * @return the resulting promoted {@code Type}, or {@code null} if the two types
     *         cannot be promoted according to the implementation's rules
     */
    Type promote(Type type1, Type type2);
}
