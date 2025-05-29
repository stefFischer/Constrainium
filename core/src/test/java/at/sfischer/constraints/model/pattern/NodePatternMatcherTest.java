package at.sfischer.constraints.model.pattern;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.numbers.OneOfNumber;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static at.sfischer.constraints.model.pattern.NodePatternMatcher.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NodePatternMatcherTest {

    @Test
    public void testMatchWithOnePlaceholder() {
        Variable a = new Variable("a");
        Node actual = new OneOfNumber(a, new NumberLiteral(3));

        Placeholder p = new Placeholder("x");
        Node pattern = new OneOfNumber(p, new NumberLiteral(3));

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertThat(match)
                .containsEntry(p, a)
                .hasSize(1);
    }

    @Test
    public void testMatchWithOnePlaceholderPatterNode() {
        Variable a = new Variable("a");
        Node actual = new OneOfNumber(a, new NumberLiteral(3));

        Placeholder p = new Placeholder("x");
        Placeholder v = new Placeholder("y");
        Node pattern = new PatternNode("at.sfischer.constraints.model.operators.numbers.OneOfNumber", p, new NumberLiteral(3), v);

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertThat(match)
                .containsEntry(p, a)
                .containsEntry(v, new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[3]))
                .hasSize(2);
    }

    @Test
    public void testMatchStringEqualsWithPlaceholder() {
        Variable a = new Variable("a");
        Variable b = new Variable("b");
        Node actual = new StringEquals(a, b);

        Placeholder x = new Placeholder("X");
        Placeholder y = new Placeholder("Y");
        Node pattern = new StringEquals(x, y);

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertThat(match)
                .containsEntry(x, a)
                .containsEntry(y, b)
                .hasSize(2);
    }

    @Test
    public void testMatchNotAVariable() {
        Variable a = new Variable("a");
        Node actual = new OneOfNumber(new ArrayLength(a), new NumberLiteral(3));

        Placeholder p = new Placeholder("x");
        Node pattern = new OneOfNumber(p, new NumberLiteral(3));

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertThat(match)
                .containsEntry(p, new ArrayLength(a))
                .hasSize(1);
    }

    @Test
    public void testMatchDoesNotMatchDifferentRoot() {
        Variable a = new Variable("a");
        Variable b = new Variable("b");
        Node actual = new StringEquals(a, b);

        Placeholder p = new Placeholder("x");
        Node pattern = new OneOfNumber(p, new NumberLiteral(3));

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertNull(match);
    }

    @Test
    public void testMatchDoesNotMatchDifferentLeafNode() {
        Variable a = new Variable("a");
        Node actual = new OneOfNumber(a, new NumberLiteral(3));

        Placeholder p = new Placeholder("x");
        Node pattern = new OneOfNumber(p, new NumberLiteral(2));

        Map<Placeholder, Node> match = NodePatternMatcher.match(pattern, actual);

        assertNull(match);
    }
}
