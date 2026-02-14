package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintTemplate;
import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.GroupDefinition;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.numbers.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintDslParserTest {

    private ConstraintTemplateFile parse(String input) throws Exception {
        ConstraintDslScanner scanner =
                new ConstraintDslScanner(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
                );

        ConstraintDslParser parser = new ConstraintDslParser(scanner);
        return parser.parse();
    }

    @Test
    void parsesEmptyFile() throws Exception {
        ConstraintTemplateFile file = parse("");

        assertTrue(file.getPolicies().isEmpty());
        assertTrue(file.getConstraints().isEmpty());
        assertTrue(file.getGroups().isEmpty());
    }

    @Test
    void parsesSimplePolicy() throws Exception {
        String input = """
                policy P1: noViolations
                """;

        ConstraintTemplateFile file = parse(input);

        assertEquals(1, file.getPolicies().size());
        assertTrue(file.getPolicies().containsKey("P1"));
    }

    @Test
    void parsesAndPolicy() throws Exception {
        String input = """
                policy P1: AND { minApplications = 2 }
                """;

        ConstraintTemplateFile file = parse(input);

        assertEquals(1, file.getPolicies().size());
        assertTrue(file.getPolicies().containsKey("P1"));
    }

    @Test
    void parsesConstraintWithExplicitPolicy() throws Exception {
        String input = """
                policy P1: noViolations
                constraint C1: true policy = P1
                """;

        ConstraintTemplateFile file = parse(input);

        ConstraintTemplate c = file.getConstraints().get(0);

        assertEquals("C1", c.getName());
        assertNotNull(c.getRetentionPolicy());
    }

    @Test
    void parsesGroupWithConstraint() throws Exception {
        String input = """
                group G1 {
                    constraint C1: true
                }
                """;

        ConstraintTemplateFile file = parse(input);

        assertEquals(1, file.getGroups().size());

        GroupDefinition group = file.getGroups().getFirst();

        assertEquals("G1", group.getName());
        assertEquals(1, group.getConstraints().size());
        assertEquals("C1", group.getConstraints().getFirst().getName());
    }

    @Test
    void parsesGroupAndTopLevelConstraint() throws Exception {
        String input = """
                constraint C1: true

                group G1 {
                    constraint C2: false
                }
                """;

        ConstraintTemplateFile file = parse(input);

        assertEquals(1, file.getConstraints().size());
        assertEquals(1, file.getGroups().size());
    }

    @Test
    void parsesDefaultPolicy() throws Exception {
        String input = """
                policy DEFAULT: minApplications = 3
                constraint C1: true
                """;

        ConstraintTemplateFile file = parse(input);

        assertTrue(file.getPolicies().containsKey("DEFAULT"));

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertNotNull(c.getRetentionPolicy());
    }

    @Test
    void parsesDefaultCodedPolicy() throws Exception {
        String input = """
                constraint C1: true
                """;

        ConstraintTemplateFile file = parse(input);

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertEquals(ConstraintDslParser.DEFAULT_POLICY, c.getRetentionPolicy());
    }

    @Test
    void parseArithmeticOperations() throws Exception {
        String input = """
            constraint C1:
                a + b * c / d^2
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        Node actualNode = c.getTerm();
        Node expectedNode = new AdditionOperator(
                new Variable("a"),
                new DivisionOperator(
                        new MultiplicationOperator(
                                new Variable("b"),
                                new Variable("c")
                        ),
                        new PowerOperator(
                                new Variable("d"),
                                new NumberLiteral(2.0)
                        )
                )
        );
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void parseArithmeticParenthesisOperations() throws Exception {
        String input = """
            constraint C1:
                (a + b) * c % d
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        Node actualNode = c.getTerm();
        Node expectedNode = new ModuloOperator(
                new MultiplicationOperator(
                        new AdditionOperator(
                                new Variable("a"),
                                new Variable("b")
                        ),
                        new Variable("c")
                ),
                new Variable("d")
        );
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void throwsOnInvalidPolicySyntax() {
        String input = "policy P1 noViolations";

        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    void throwsOnInvalidConstraint() {
        String input = "constraint : true";

        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    void parsesQuantifierWithArrayElement() throws Exception {
        String input = """
            constraint C1:
                forall x: ARRAY_ELEMENT > 5
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerm();
        Node expectedNode = new ForAll(
                new Variable("x"),
                new GreaterThanOperator(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new NumberLiteral(5.0)
                )
        );
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void parsesQuantifierAsFunction() throws Exception {
        String input = """
            constraint C1:
                arrays.forAll(x, ARRAY_ELEMENT > 5)
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerm();
        Node expectedNode = new ForAll(
                new Variable("x"),
                new GreaterThanOperator(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new NumberLiteral(5.0)
                )
        );
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void parsesFunctionWithSpecificTypeConstructor() throws Exception {
        String input = """
            constraint C1:
                number.OneOf(x, 3)
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerm();
        Node expectedNode = new OneOfNumber(new Variable("x"), new NumberLiteral(3));
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void parsesFunctionWithArrayArgument() throws Exception {
        String input = """
            constraint C1:
                number.OneOf(x, [1, 2, 3])
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintTemplate c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerm();
        Node expectedNode = new OneOfNumber(
                new Variable("x"),
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1),
                        new NumberLiteral(2),
                        new NumberLiteral(3),
                })
        );
        assertEquals(expectedNode, actualNode);
    }
}
