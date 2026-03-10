package at.sfischer.generator.random;

public enum RandomNumberGenerationStrategy {
    SMALL, MEDIUM, FULL, BOUNDARIES;

    public static RandomNumberGenerationStrategy getFromName(String name){
        for (RandomNumberGenerationStrategy value : RandomNumberGenerationStrategy.values()) {
            if(value.name().equalsIgnoreCase(name)){
                return value;
            }
        }

        return null;
    }
}
