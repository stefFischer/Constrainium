package at.sfischer.constraints.parser;

import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.miner.ConstraintPolicy;

import java.io.IOException;
import java.util.*;

public class MetamorphicRelationSetParser implements ConstraintConstructParser<MetamorphicRelationSetTemplate> {

    @Override
    public TokenKind startToken() {
        return MetamorphicTokenType.METAMORPHIC_SET;
    }

    @Override
    public Map<String, TokenKind> keywords() {
        return Map.of(
                "MRS", MetamorphicTokenType.METAMORPHIC_SET,
                "transformations", MetamorphicTokenType.TRANSFORMATIONS,
                "validations", MetamorphicTokenType.VALIDATIONS
        );
    }

    @Override
    public MetamorphicRelationSetTemplate parse(ExtensionParserContext context, ConstraintPolicy defaultPolicy,
            ConstraintPolicy groupPolicy, Map<String, ConstraintPolicy> policies) throws IOException, ParseException {

        context.consume(MetamorphicTokenType.METAMORPHIC_SET,"Expected 'MRS'");
        String name = context.consume(TokenType.IDENTIFIER, "Expected metamorphic relation set name").getLexeme();
        context.consume(TokenType.COLON, "Expected ':'");

        context.consume(MetamorphicTokenType.TRANSFORMATIONS, "Expected 'transformations'");
        context.consume(TokenType.COLON, "Expected ':'");
        List<NamedExpression> transformations = parseExpressionSet(context);

        context.consume(MetamorphicTokenType.VALIDATIONS, "Expected 'validations'");
        context.consume(TokenType.COLON, "Expected ':'");
        List<NamedExpression> validations = parseExpressionSet(context);

        ConstraintPolicy policy;
        if (context.match(TokenType.POLICY)) {
            context.consume(TokenType.ASSIGN, "Expected '='" );

            String policyRef = context.consume(TokenType.IDENTIFIER, "Expected policy name").getLexeme();
            policy = policies.get(policyRef);
            if (policy == null) {
                throw new ParseException("Unknown policy: " + policyRef, context.current());
            }
        } else if (groupPolicy != null) {
            policy = groupPolicy;
        } else {
            policy = defaultPolicy;
        }

        return new MetamorphicRelationSetTemplate(name, transformations, validations, policy);
    }

    private List<NamedExpression> parseExpressionSet(ExtensionParserContext context) throws IOException, ParseException {
        context.consume(TokenType.LEFT_BRACE,"Expected '{'");
        List<NamedExpression> expressions = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        while (!context.check(TokenType.RIGHT_BRACE)) {
            String identifier = null;
            if (context.check(TokenType.AT)) {
                context.consume(TokenType.AT, "Expected '@'");
                identifier = context.consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();
                context.consume(TokenType.COLON, "Expected ':' after identifier");

                if (!identifiers.add(identifier)) {
                    throw new ParseException("Duplicate identifier '" + identifier + "'", context.current());
                }
            }

            expressions.add(new NamedExpression(identifier, context.parseExpression()));
        }

        context.consume(TokenType.RIGHT_BRACE,"Expected '}'");

        if (expressions.isEmpty()) {
            throw new ParseException("Expected at least one expression", context.current());
        }

        return expressions;
    }
}
