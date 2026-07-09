package at.sfischer.constraints.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConstraintConstructParserRegistryTest {

    @Test
    void metamorphicRelationParserIsDiscovered() {
        ConstraintConstructParser<?> providers = ConstraintConstructParserRegistry.forStartToken(MetamorphicTokenType.METAMORPHIC);
        assertNotNull(providers);
        assertInstanceOf(MetamorphicRelationParser.class, providers);
        assertTrue(ConstraintConstructParserRegistry.getKeywordExtensions().containsValue(MetamorphicTokenType.METAMORPHIC));
    }
}
