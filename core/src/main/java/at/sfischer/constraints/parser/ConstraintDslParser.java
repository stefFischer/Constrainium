package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintTemplate;
import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.GroupDefinition;
import at.sfischer.constraints.miner.*;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import at.sfischer.constraints.parser.registry.FunctionCreateException;
import at.sfischer.constraints.parser.registry.FunctionRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintDslParser {

    public static final String DEFAULT_POLICY_NAME = "DEFAULT";
    public static final ConstraintPolicy DEFAULT_POLICY = new AndConstraintPolicy(new NoViolationsPolicy(), new MinApplicationsPolicy(1));

    private final ConstraintDslScanner scanner;

    private Token current;
    private Token previous;

    public ConstraintDslParser(ConstraintDslScanner scanner) throws IOException {
        this.scanner = scanner;
        advance(); // load first token
    }

    private void advance() throws IOException {
        previous = current;
        current = scanner.nextToken();
    }

    private boolean check(TokenType type) {
        return current.getType() == type;
    }

    private boolean match(TokenType... types) throws IOException {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) throws IOException, ParseException {
        if (check(type)) {
            Token token = current;
            advance();
            return token;
        }
        throw new ParseException(message, current);
    }

    private boolean isAtEnd() {
        return current.getType() == TokenType.EOF;
    }

    public ConstraintTemplateFile parse() throws IOException, ParseException {

        ConstraintPolicy defaultPolicy;
        Map<String, ConstraintPolicy> policies = new HashMap<>();
        List<GroupDefinition> groups = new ArrayList<>();
        List<ConstraintTemplate> constraints = new ArrayList<>();

        while (check(TokenType.POLICY)) {
            parsePolicyDecl(policies);
        }

        defaultPolicy = policies.getOrDefault("DEFAULT_POLICY_NAME", DEFAULT_POLICY);

        while (!isAtEnd()) {
            if (check(TokenType.GROUP)) {
                groups.add(parseGroupDecl(defaultPolicy, policies));
            } else if (check(TokenType.CONSTRAINT)) {
                constraints.add(parseConstraintDecl(defaultPolicy, null, policies));
            } else {
                throw new ParseException(
                        "Expected 'policy' or 'group' at top level",
                        current
                );
            }
        }

        return new ConstraintTemplateFile(policies, constraints, groups);
    }

    private void parsePolicyDecl(Map<String, ConstraintPolicy> policies) throws IOException, ParseException {

        consume(TokenType.POLICY, "Expected 'policy'");
        String name = consume(TokenType.IDENTIFIER, "Expected policy name")
                .getLexeme();

        consume(TokenType.COLON, "Expected ':' after policy name");

        ConstraintPolicy policy = parsePolicyStatement();

        policies.put(name, policy);
    }

    private ConstraintPolicy parsePolicyStatement() throws IOException, ParseException {

        if (match(TokenType.AND)) {
            return parseLogicalPolicy(true);
        }

        if (match(TokenType.OR)) {
            return parseLogicalPolicy(false);
        }

        return parsePolicyTerm();
    }

    private ConstraintPolicy parseLogicalPolicy(boolean isAnd) throws IOException, ParseException {

        consume(TokenType.LEFT_BRACE, "Expected '{' after AND/OR");

        List<ConstraintPolicy> children = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            children.add(parsePolicyStatement());
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}'");

        return isAnd
                ? new AndConstraintPolicy(children)
                : new OrConstraintPolicy(children);
    }

    private ConstraintPolicy parsePolicyTerm() throws IOException, ParseException {

        if (match(TokenType.NO_VIOLATIONS)) {
            return new NoViolationsPolicy();
        }

        if (match(TokenType.MIN_APPLICATIONS)) {
            consume(TokenType.ASSIGN, "Expected '='");
            int value = Integer.parseInt(
                    consume(TokenType.NUMBER, "Expected integer").getLexeme()
            );
            return new MinApplicationsPolicy(value);
        }

//        if (match(TokenType.MAX_VIOLATIONS)) {
//            consume(TokenType.ASSIGN, "Expected '='");
//            double value = Double.parseDouble(
//                    consume(TokenType.NUMBER, "Expected integer").getLexeme()
//            );
//            return new MaxViolationsPolicy(value);
//        }

        throw new ParseException("Invalid policy expression", current);
    }


    private GroupDefinition parseGroupDecl(ConstraintPolicy defaultPolicy, Map<String, ConstraintPolicy> policies) throws IOException, ParseException {

        consume(TokenType.GROUP, "Expected 'group'");
        String name = consume(TokenType.IDENTIFIER, "Expected group name")
                .getLexeme();

        consume(TokenType.LEFT_BRACE, "Expected '{'");

        ConstraintPolicy groupPolicy = null;
        if (match(TokenType.POLICY)) {
            consume(TokenType.ASSIGN, "Expected '='");
            String groupPolicyRef = consume(TokenType.IDENTIFIER, "Expected policy name").getLexeme();
            groupPolicy = policies.get(groupPolicyRef);
        }

        List<ConstraintTemplate> constraints = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            constraints.add(parseConstraintDecl(defaultPolicy, groupPolicy, policies));
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}'");

        return new GroupDefinition(name, constraints);
    }

    private ConstraintTemplate parseConstraintDecl(ConstraintPolicy defaultPolicy, ConstraintPolicy groupPolicy, Map<String, ConstraintPolicy> policies) throws IOException, ParseException {
        consume(TokenType.CONSTRAINT, "Expected 'constraint'");
        String name = consume(TokenType.IDENTIFIER,
                "Expected constraint name").getLexeme();

        consume(TokenType.COLON, "Expected ':'");

        Node expression = parseExpression();
        ConstraintPolicy policy;
        if (match(TokenType.POLICY)) {
            consume(TokenType.ASSIGN, "Expected '='");
            String policyRef = consume(TokenType.IDENTIFIER, "Expected policy name").getLexeme();
            policy = policies.get(policyRef);
        } else if(groupPolicy != null) {
            policy = groupPolicy;
        } else {
            policy = defaultPolicy;
        }

        return new ConstraintTemplate(name, expression, policy);
    }

    private Node parseExpression() throws IOException, ParseException {
        return parseOr();
    }

    private Node parseOr() throws IOException, ParseException {
        Node expr = parseAnd();

        while (match(TokenType.OR_OR)) {
            Node right = parseAnd();
            expr = new OrOperator(expr, right);
        }

        return expr;
    }

    private Node parseAnd() throws IOException, ParseException {
        Node expr = parseEquality();

        while (match(TokenType.AND_AND)) {
            Node right = parseEquality();
            expr = new AndOperator(expr, right);
        }

        return expr;
    }

    private Node parseEquality() throws IOException, ParseException {
        Node expr = parseRelational();

        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous;
            Node right = parseRelational();

            expr = operator.getType() == TokenType.EQUAL_EQUAL
                    ? new EqualOperator(expr, right)
                    : new NotOperator(new EqualOperator(expr, right));
        }

        return expr;
    }

    private Node parseRelational() throws IOException, ParseException {
        Node expr = parseAdditive();

        while (match(TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL)) {

            Token operator = previous;
            Node right = parseAdditive();

            switch (operator.getType()) {
                case LESS -> expr = new LessThanOperator(expr, right);
                case LESS_EQUAL -> expr = new LessThanOrEqualOperator(expr, right);
                case GREATER -> expr = new GreaterThanOperator(expr, right);
                case GREATER_EQUAL -> expr = new GreaterThanOrEqualOperator(expr, right);
            }
        }

        return expr;
    }

    private Node parseAdditive() throws IOException, ParseException {
        Node expr = parseMultiplicative();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous;
            Node right = parseMultiplicative();

            expr = operator.getType() == TokenType.PLUS
                    ? new AdditionOperator(expr, right)
                    : new SubtractionOperator(expr, right);
        }

        return expr;
    }

    private Node parseMultiplicative() throws IOException, ParseException {
        Node expr = parsePower();

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MODULO)) {
            Token operator = previous;
            Node right = parsePower();

            expr = switch (operator.getType()) {
                case TokenType.STAR -> new MultiplicationOperator(expr, right);
                case TokenType.SLASH -> new DivisionOperator(expr, right);
                case TokenType.MODULO -> new ModuloOperator(expr, right);
                default -> throw new ParseException("Unsupported multiplicative operator", operator);
            };
        }

        return expr;
    }

    private Node parsePower() throws IOException, ParseException {
        Node expr = parseUnary();

        if (match(TokenType.POWER)) {
            Node right = parsePower();
            expr = new PowerOperator(expr, right);
        }

        return expr;
    }

    private Node parseUnary() throws IOException, ParseException {
        if (match(TokenType.BANG)) {
            return new NotOperator(parseUnary());
        }

        return parsePrimary();
    }

    private Node parsePrimary() throws IOException, ParseException {
        if (check(TokenType.NUMBER)
            || check(TokenType.STRING)
            || check(TokenType.TRUE)
            || check(TokenType.FALSE)
            || check(TokenType.LEFT_BRACKET)) {
            return parseLiteral();
        }

        if (match(TokenType.ARRAY_ELEMENT)) {
            return new Variable(ArrayQuantifier.ELEMENT_NAME);
        }

        if (match(TokenType.IDENTIFIER)) {
            String name = previous.getLexeme();

            if (match(TokenType.LEFT_PAREN)) {
                return parseFunctionCall(name);
            }

            return new Variable(name);
        }

        if (match(TokenType.FORALL)) {
            return parseQuantifier(true);
        }

        if (match(TokenType.EXISTS)) {
            return parseQuantifier(false);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Node expr = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Expected ')'");
            return expr;
        }

        throw new ParseException("Expected expression", current);
    }

    private Value<?> parseLiteral() throws IOException, ParseException {
        if (match(TokenType.NUMBER)) {
            return new NumberLiteral(Double.parseDouble(previous.getLexeme()));
        }

        if (match(TokenType.STRING)) {
            return new StringLiteral(previous.getLexeme());
        }

        if (match(TokenType.TRUE)) {
            return BooleanLiteral.TRUE;
        }

        if (match(TokenType.FALSE)) {
            return BooleanLiteral.FALSE;
        }

        if (match(TokenType.LEFT_BRACKET)) {
            return parseArrayLiteral();
        }

        throw new ParseException("Expected literal value", current);
    }

    private ArrayValues<?> parseArrayLiteral() throws IOException, ParseException {
        List<Node> elements = new ArrayList<>();

        if (!check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(parseLiteral());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_BRACKET,
                "Expected ']' after array literal");

        return ArrayValues.createArrayValuesFromList(elements);
    }

    private Node parseFunctionCall(String name) throws IOException, ParseException {

        List<Node> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expected ')'");

        try {
            Function function = FunctionRegistry.create(name, arguments);
            if(function == null){
                throw new ParseException("Could not create function: " + name, current);
            }

            return function;
        } catch (FunctionCreateException e) {
            throw new ParseException("Could not create function: " + name + ", error: " + e.getMessage(), current);
        }
    }

    private Node parseQuantifier(boolean forall) throws IOException, ParseException {

        Token variable = consume(TokenType.IDENTIFIER, "Expected variable name");
        consume(TokenType.COLON, "Expected ':' after quantifier variable");

        Node body = parseExpression();

        return forall
                ? new ForAll(new Variable(variable.getLexeme()), body)
                : new Exists(new Variable(variable.getLexeme()), body);
    }
}
