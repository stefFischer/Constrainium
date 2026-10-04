package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintConstruct;
import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.GroupDefinition;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.numbers.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        ConstraintConstruct c = file.getConstraints().get(0);

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

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertNotNull(c.getRetentionPolicy());
    }

    @Test
    void parsesDefaultCodedPolicy() throws Exception {
        String input = """
                constraint C1: true
                """;

        ConstraintTemplateFile file = parse(input);

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertEquals(ConstraintDslParser.DEFAULT_POLICY, c.getRetentionPolicy());
    }

    @Test
    void parsePi() throws Exception {
        String input = """
            constraint C1:
                x > PI
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintConstruct c = file.getConstraints().getFirst();
        Node actualNode = c.getTerms().getFirst();
        Node expectedNode = new GreaterThanOperator(
                new Variable("x"),
                PiLiteral.INSTANCE
        );
        assertEquals(expectedNode, actualNode);
    }

    @Test
    void parseArithmeticOperations() throws Exception {
        String input = """
            constraint C1:
                a + b * c / d^2
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintConstruct c = file.getConstraints().getFirst();
        Node actualNode = c.getTerms().getFirst();
        Node expectedNode = new AdditionOperator(
                new Variable("a"),
                new DivisionOperator(
                        new MultiplicationOperator(
                                new Variable("b"),
                                new Variable("c")
                        ),
                        new PowerOperator(
                                new Variable("d"),
                                new IntegerLiteral(2)
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

        ConstraintConstruct c = file.getConstraints().getFirst();
        Node actualNode = c.getTerms().getFirst();
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
                forall x: ARRAY_ELEMENT > 5.0
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerms().getFirst();
        Node expectedNode = new ForAll(
                new Variable("x"),
                new GreaterThanOperator(
                        new Variable(ArrayOperation.ELEMENT_NAME),
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

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerms().getFirst();
        Node expectedNode = new ForAll(
                new Variable("x"),
                new GreaterThanOperator(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new IntegerLiteral(5)
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

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerms().getFirst();
        Node expectedNode = new OneOfNumber(new Variable("x"), new IntegerLiteral(3));
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

        ConstraintConstruct c = file.getConstraints().getFirst();
        assertEquals("C1", c.getName());

        Node actualNode = c.getTerms().getFirst();
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

    @Test
    void parsesFunctionDefinition() throws Exception {
        String input = """
            function difference(a, b){
                abs(a-b)
            }
            
            constraint C1: difference(5, 2) > 0
            constraint C2: difference(a, 2) > 0
            """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(2, file.getConstraints().size());

        ConstraintConstruct c1 = file.getConstraints().getFirst();
        assertEquals("C1", c1.getName());

        Node actualNode = c1.getTerms().getFirst();
        Node expectedNode = new GreaterThanOperator(
                new UserFunction("difference", List.of(new IntegerLiteral(5), new IntegerLiteral(2)), List.of("a", "b"), List.of(TypeEnum.NUMBER, TypeEnum.NUMBER),
                        new Abs(new SubtractionOperator(new IntegerLiteral(5), new IntegerLiteral(2)))
                        ),
                new IntegerLiteral(0)
        );
        assertEquals(expectedNode, actualNode);

        Node result = actualNode.evaluate();
        BooleanLiteral resultValue = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(resultValue.getValue());

        ConstraintConstruct c2 = file.getConstraints().getLast();
        assertEquals("C2", c2.getName());

        actualNode = c2.getTerms().getFirst();
        expectedNode = new GreaterThanOperator(
                new UserFunction("difference", List.of(new Variable("a"), new IntegerLiteral(2)), List.of("a", "b"), List.of(TypeEnum.NUMBER, TypeEnum.NUMBER),
                        new Abs(new SubtractionOperator(new Variable("a"), new IntegerLiteral(2)))
                ),
                new IntegerLiteral(0)
        );
        assertEquals(expectedNode, actualNode);

        result = actualNode.evaluate();
        assertThat(actualNode)
                .usingRecursiveComparison()
                .withComparatorForType(
                        Comparator.comparing(a -> new BigDecimal(String.valueOf(a.getValue()))),
                        NumberLiteral.class)
                .isEqualTo(result);
    }

    @Test
    void functionDefinedInGroupIsAvailableToGroupConstraints() throws Exception {
        String input = """
        group G {
            function difference(a, b) {
                abs(a - b)
            }

            constraint C1: difference(5, 2) > 0
        }
        """;

        ConstraintTemplateFile file = parse(input);

        assertEquals(1, file.getGroups().size());

        GroupDefinition group = file.getGroups().getFirst();
        assertEquals("G", group.getName());
        assertEquals(1, group.getConstraints().size());

        ConstraintConstruct constraint = group.getConstraints().getFirst();
        Node actualNode = constraint.getTerms().getFirst();

        assertInstanceOf(GreaterThanOperator.class, actualNode);

        Node result = actualNode.evaluate();
        BooleanLiteral resultValue = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(resultValue.getValue());
    }

    @Test
    void fileFunctionIsAvailableInsideGroup() throws Exception {
        String input = """
        function difference(a, b) {
            abs(a - b)
        }

        group G {
            constraint C1: difference(5, 2) > 0
        }
        """;

        ConstraintTemplateFile file = parse(input);

        ConstraintConstruct constraint = file.getGroups()
                .getFirst()
                .getConstraints()
                .getFirst();

        Node result = constraint.getTerms().getFirst().evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    void nestedFunctionCanUseFunctionDefinedInOuterFunction() throws Exception {
        String input = """
        function outer(x) {
            function difference(a, b) {
                abs(a - b)
            }

            difference(x, 2)
        }

        constraint C1: outer(5) > 0
        """;

        ConstraintTemplateFile file = parse(input);

        Node actualNode = file.getConstraints()
                .getFirst()
                .getTerms()
                .getFirst();

        Node result = actualNode.evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    void innerFunctionShadowsOuterFunction() throws Exception {
        String input = """
        function f(a, b) {
            a - b
        }

        group G {
            function f(a, b) {
                a + b
            }

            constraint C1: f(2, 5) > 0
        }
        """;

        ConstraintTemplateFile file = parse(input);

        ConstraintConstruct constraint = file.getGroups()
                .getFirst()
                .getConstraints()
                .getFirst();

        Node actualNode = constraint.getTerms().getFirst();

        Node result = actualNode.evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    void innerFunctionDoesNotReplaceOuterFunction() throws Exception {
        String input = """
        function f(a, b) {
            a - b
        }

        group G {
            function f(a, b) {
                a + b
            }

            constraint C1: f(5, 2) == 3
        }

        constraint C2: f(5, 2) == 3
        """;

        ConstraintTemplateFile file = parse(input);

        ConstraintConstruct outerConstraint = file.getConstraints().getFirst();

        Node result = outerConstraint.getTerms().getFirst().evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    void innermostFunctionShadowsAllOuterDefinitions() throws Exception {
        String input = """
        function calculate(x) {
            x + 1
        }

        function outer(x) {
            function calculate(x) {
                x + 2
            }

            function inner(x) {
                function calculate(x) {
                    x + 3
                }

                calculate(x)
            }

            inner(x)
        }

        constraint C1: outer(1) == 4
        """;

        ConstraintTemplateFile file = parse(input);

        Node actualNode = file.getConstraints()
                .getFirst()
                .getTerms()
                .getFirst();

        Node result = actualNode.evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    void functionDefinedInGroupIsNotAvailableOutsideGroup() throws Exception {
        String input = """
        group G {
            function difference(a, b) {
                abs(a - b)
            }

            constraint C1: difference(5, 2) > 0
        }

        constraint C2: difference(5, 2) > 0
        """;

        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    void functionDefinedInsideFunctionIsNotAvailableOutside() throws Exception {
        String input = """
        function outer(x) {
            function difference(a, b) {
                abs(a - b)
            }

            difference(x, 2)
        }

        constraint C1: difference(5, 2) > 0
        """;

        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    void functionIsNotVisibleInSiblingGroup() throws Exception {
        String input = """
        group G1 {
            function difference(a, b) {
                a - b
            }

            constraint C1: difference(5, 2) == 3
        }

        group G2 {
            constraint C2: difference(5, 2) == 3
        }
        """;

        assertThrows(ParseException.class, () -> parse(input));
    }

    @Test
    public void testFunctionEvaluation() throws Exception {
        String input = """
            function sum(a, b){
                a + b
            }

            constraint C1: sum(5, 2) == 5 + 2
        """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        Node actualNode = file.getConstraints()
                .getFirst()
                .getTerms()
                .getFirst();
        Node result = actualNode.evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }

    @Test
    public void testFunctionEvaluationInsideFor() throws Exception {
        String input = """
            function addOffset(x){
                arrays.forEach(x, ARRAY_ELEMENT + (0.5 * arrays.standardDeviation(x)))
            }

            function sineWave(amplitude, frequency, sampleRate, length){
                arrays.generate(amplitude * sin(2 * PI * frequency * ARRAY_INDEX / sampleRate), length)
            }

            constraint C1: arrays.length(addOffset(sineWave(0.5, 5, 100, 1000))) == 1000
        """;

        ConstraintTemplateFile file = parse(input);
        assertEquals(1, file.getConstraints().size());

        Node actualNode = file.getConstraints()
                .getFirst()
                .getTerms()
                .getFirst();

        Node result = actualNode.evaluate();

        BooleanLiteral value = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(value.getValue());
    }
}
