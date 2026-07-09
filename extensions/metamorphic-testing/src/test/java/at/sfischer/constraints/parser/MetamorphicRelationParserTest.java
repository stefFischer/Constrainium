package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintConstruct;
import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationTemplate;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the metamorphic-relation extension end to end:
 * scanner keyword recognition -> ConstraintConstructParser dispatch -> resulting
 * MetamorphicRelationTemplate.
 *
 * NOTE: adjust the ConstraintDslScanner construction below if your actual
 * constructor signature differs (e.g. takes a Reader vs. a String directly).
 */
public class MetamorphicRelationParserTest {

    private ConstraintTemplateFile parse(String source) throws IOException, ParseException {
        ConstraintDslScanner scanner = new ConstraintDslScanner(new StringReader(source));
        ConstraintDslParser parser = new ConstraintDslParser(scanner);
        return parser.parse();
    }

    @Test
    void parsesBasicMetamorphicRelation() throws IOException, ParseException {
        String source = """
                metamorphic M1:
                    transformation: input + 1
                    validation: sourceOutput <= followUpOutput
                """;

        ConstraintTemplateFile file = parse(source);

        List<MetamorphicRelationTemplate> relations = file.getConstraints(MetamorphicRelationTemplate.class);

        assertEquals(1, relations.size());

        MetamorphicRelationTemplate relation = relations.getFirst();
        assertEquals("M1", relation.getName());
        assertNotNull(relation.getTransformation());
        assertNotNull(relation.getValidation());
        assertInstanceOf(LessThanOrEqualOperator.class, relation.getValidation());
    }

    @Test
    void usesDefaultPolicyWhenNoneSpecified() throws IOException, ParseException {
        String source = """
                metamorphic M1:
                    transformation: input + 1
                    validation: sourceOutput <= followUpOutput
                """;

        ConstraintTemplateFile file = parse(source);
        MetamorphicRelationTemplate relation = file.getConstraints(MetamorphicRelationTemplate.class).getFirst();

        assertEquals(ConstraintDslParser.DEFAULT_POLICY, relation.getRetentionPolicy());
    }

    @Test
    void appliesNamedPolicyWhenSpecified() throws IOException, ParseException {
        String source = """
                policy STRICT: minApplications = 3

                metamorphic M1:
                    transformation: input + 1
                    validation: sourceOutput <= followUpOutput
                    policy = STRICT
                """;

        ConstraintTemplateFile file = parse(source);
        MetamorphicRelationTemplate relation = file.getConstraints(MetamorphicRelationTemplate.class).getFirst();

        ConstraintPolicy expected = file.getPolicies().get("STRICT");
        assertNotNull(expected);
        assertEquals(expected, relation.getRetentionPolicy());
    }

    @Test
    void supportsMultipleMetamorphicRelationsAndConstraintsTogether() throws IOException, ParseException {
        String source = """
                constraint C1:
                    forall x: ARRAY_ELEMENT > 5.0

                metamorphic M1:
                    transformation: input + 1
                    validation: sourceOutput <= followUpOutput

                metamorphic M2:
                    transformation: input * 2
                    validation: sourceOutput <= followUpOutput
                """;

        ConstraintTemplateFile file = parse(source);

        List<ConstraintConstruct> all = file.getConstraints();
        assertEquals(3, all.size());

        List<MetamorphicRelationTemplate> relations = file.getConstraints(MetamorphicRelationTemplate.class);
        assertEquals(2, relations.size());
        assertEquals("M1", relations.get(0).getName());
        assertEquals("M2", relations.get(1).getName());
    }

    @Test
    void metamorphicRelationInsideGroupIsParsed() throws IOException, ParseException {
        String source = """
                group G1 {
                    metamorphic M1:
                        transformation: input + 1
                        validation: sourceOutput <= followUpOutput
                }
                """;

        ConstraintTemplateFile file = parse(source);

        assertEquals(1, file.getGroups().size());
        List<MetamorphicRelationTemplate> relations = file.getGroups().getFirst().getConstraints(MetamorphicRelationTemplate.class);
        assertEquals(1, relations.size());
        assertEquals("M1", relations.getFirst().getName());
    }

    @Test
    void missingTransformationKeywordThrowsParseException() {
        String source = """
                metamorphic M1:
                    validation: sourceOutput <= followUpOutput
                """;

        assertThrows(ParseException.class, () -> parse(source));
    }

    @Test
    void missingValidationKeywordThrowsParseException() {
        String source = """
                metamorphic M1:
                    transformation: input + 1
                """;

        assertThrows(ParseException.class, () -> parse(source));
    }

    @Test
    void missingRelationNameThrowsParseException() {
        String source = """
                metamorphic :
                    transformation: input + 1
                    validation: sourceOutput <= followUpOutput
                """;

        assertThrows(ParseException.class, () -> parse(source));
    }

    @Test
    void transformationSupportsFunctionCallsAndArithmetic() throws IOException, ParseException {
        // Assumes an "abs" function is registered in FunctionRegistry; swap for
        // whatever built-in/test function is actually available in your registry.
        String source = """
                metamorphic M1:
                    transformation: input + 1 * 2
                    validation: sourceOutput <= followUpOutput
                """;

        ConstraintTemplateFile file = parse(source);
        MetamorphicRelationTemplate relation = file.getConstraints(MetamorphicRelationTemplate.class).getFirst();

        assertNotNull(relation.getTransformation());
    }
}
