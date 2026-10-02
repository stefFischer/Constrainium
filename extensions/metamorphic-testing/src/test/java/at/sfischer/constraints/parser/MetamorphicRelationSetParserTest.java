package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MetamorphicRelationSetParserTest {

    private ConstraintTemplateFile parse(String source) throws IOException, ParseException {
        ConstraintDslScanner scanner = new ConstraintDslScanner(new StringReader(source));
        ConstraintDslParser parser = new ConstraintDslParser(scanner);
        return parser.parse();
    }

    @Test
    void parsesMetamorphicRelationSet() throws IOException, ParseException {
        String source = """
            MRS M1:
                transformations: {
                    input + 1
                    input * 2
                    input * 3
                }

                validations: {
                    sourceOutput <= followUpOutput
                    sourceOutput == followUpOutput
                }
            """;

        ConstraintTemplateFile file = parse(source);

        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);

        assertEquals(1, relations.size());

        MetamorphicRelationSetTemplate relation = relations.getFirst();

        assertEquals("M1", relation.getName());
        assertEquals(3, relation.getTransformations().size());
        assertEquals(2, relation.getValidations().size());
    }

    @Test
    void parsesTransformationExpressions() throws IOException, ParseException {
        String source = """
            MRS M1:
                transformations: {
                    input + 1
                    input * 2
                }

                validations: {
                    sourceOutput <= followUpOutput
                }
            """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation = file.getConstraints(MetamorphicRelationSetTemplate.class).getFirst();

        assertEquals(2, relation.getTransformations().size());
        assertEquals(1, relation.getValidations().size());

        assertNotNull(relation.getTransformations().get(0));
        assertNotNull(relation.getTransformations().get(1));
        assertInstanceOf(
                LessThanOrEqualOperator.class,
                relation.getValidations().getFirst().expression()
        );
    }

    @Test
    void appliesNamedPolicy() throws IOException, ParseException {
        String source = """
            policy STRICT: minApplications = 3

            MRS M1:
                transformations: {
                    input + 1
                    input * 2
                }

                validations: {
                    sourceOutput <= followUpOutput
                }

                policy = STRICT
            """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation = file.getConstraints(MetamorphicRelationSetTemplate.class).getFirst();

        assertEquals(
                file.getPolicies().get("STRICT"),
                relation.getRetentionPolicy()
        );
    }

    @Test
    void emptyTransformationSetThrowsParseException() {
        String source = """
            MRS M1:
                transformations: {
                }

                validations: {
                    sourceOutput <= followUpOutput
                }
            """;

        assertThrows(ParseException.class, () -> parse(source));
    }

    @Test
    void parsesMultipleMetamorphicRelationSets() throws IOException, ParseException {
        String source = """
            MRS M1:
                transformations: {
                    input + 1
                    input * 2
                }

                validations: {
                    sourceOutput <= followUpOutput
                    sourceOutput == followUpOutput
                }

            MRS M2:
                transformations: {
                    input - 1
                    input / 2
                }

                validations: {
                    sourceOutput >= followUpOutput
                }
            """;

        ConstraintTemplateFile file = parse(source);

        List<MetamorphicRelationSetTemplate> sets = file.getConstraints(MetamorphicRelationSetTemplate.class);

        assertEquals(2, sets.size());

        MetamorphicRelationSetTemplate first = sets.get(0);
        assertEquals("M1", first.getName());
        assertEquals(2, first.getTransformations().size());
        assertEquals(2, first.getValidations().size());

        MetamorphicRelationSetTemplate second = sets.get(1);
        assertEquals("M2", second.getName());
        assertEquals(2, second.getTransformations().size());
        assertEquals(1, second.getValidations().size());
    }

    @Test
    void parsesNamedTransformations() throws IOException, ParseException {
        String source = """
        MRS M1:
            transformations: {
                @increment: input + 1
                @double: input * 2
                @triple: input * 3
            }

            validations: {
                sourceOutput <= followUpOutput
            }
        """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation =
                file.getConstraints(MetamorphicRelationSetTemplate.class)
                        .getFirst();

        List<NamedExpression> transformations = relation.getTransformations();

        assertEquals(3, transformations.size());

        assertEquals("increment", transformations.get(0).name());
        assertEquals("double", transformations.get(1).name());
        assertEquals("triple", transformations.get(2).name());

        assertNotNull(transformations.get(0).expression());
        assertNotNull(transformations.get(1).expression());
        assertNotNull(transformations.get(2).expression());
    }

    @Test
    void parsesNamedValidations() throws IOException, ParseException {
        String source = """
        MRS M1:
            transformations: {
                input + 1
            }

            validations: {
                @monotonic: sourceOutput <= followUpOutput
                @invariant: sourceOutput == followUpOutput
            }
        """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation =
                file.getConstraints(MetamorphicRelationSetTemplate.class)
                        .getFirst();

        List<NamedExpression> validations = relation.getValidations();

        assertEquals(2, validations.size());

        assertEquals("monotonic", validations.get(0).name());
        assertEquals("invariant", validations.get(1).name());

        assertInstanceOf(
                LessThanOrEqualOperator.class,
                validations.get(0).expression()
        );

        assertNotNull(validations.get(1).expression());
    }

    @Test
    void parsesNamedAndUnnamedExpressions() throws IOException, ParseException {
        String source = """
        MRS M1:
            transformations: {
                @increment: input + 1
                input * 2
                @triple: input * 3
            }

            validations: {
                @monotonic: sourceOutput <= followUpOutput
                sourceOutput == followUpOutput
            }
        """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation =
                file.getConstraints(MetamorphicRelationSetTemplate.class)
                        .getFirst();

        List<NamedExpression> transformations = relation.getTransformations();
        List<NamedExpression> validations = relation.getValidations();

        assertEquals(3, transformations.size());
        assertEquals(2, validations.size());

        assertEquals("increment", transformations.get(0).name());
        assertNull(transformations.get(1).name());
        assertEquals("triple", transformations.get(2).name());

        assertEquals("monotonic", validations.get(0).name());
        assertNull(validations.get(1).name());

        assertNotNull(transformations.get(1).expression());
        assertNotNull(validations.get(1).expression());
    }

    @Test
    void identifierAtStartOfExpressionIsNotTreatedAsName()
            throws IOException, ParseException {

        String source = """
        MRS M1:
            transformations: {
                input + 1
                input * 2
            }

            validations: {
                sourceOutput <= followUpOutput
            }
        """;

        ConstraintTemplateFile file = parse(source);

        MetamorphicRelationSetTemplate relation =
                file.getConstraints(MetamorphicRelationSetTemplate.class)
                        .getFirst();

        assertNull(relation.getTransformations().get(0).name());
        assertNull(relation.getTransformations().get(1).name());

        assertNotNull(relation.getTransformations().get(0).expression());
        assertNotNull(relation.getTransformations().get(1).expression());
    }

    @Test
    void missingNameAfterAtThrowsParseException() {
        String source = """
        MRS M1:
            transformations: {
                @: input + 1
            }

            validations: {
                sourceOutput <= followUpOutput
            }
        """;

        assertThrows(ParseException.class, () -> parse(source));
    }
}
