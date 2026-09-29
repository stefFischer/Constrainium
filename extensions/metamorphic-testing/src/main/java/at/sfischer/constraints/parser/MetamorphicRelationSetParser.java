package at.sfischer.constraints.parser;

import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<Node> transformations = parseExpressionSet(context);

        context.consume(MetamorphicTokenType.VALIDATIONS, "Expected 'validations'");
        context.consume(TokenType.COLON, "Expected ':'");
        List<Node> validations = parseExpressionSet(context);

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

    private List<Node> parseExpressionSet(ExtensionParserContext context) throws IOException, ParseException {
        context.consume(TokenType.LEFT_BRACE,"Expected '{'");
        List<Node> expressions = new ArrayList<>();
        while (!context.check(TokenType.RIGHT_BRACE)) {
            expressions.add(context.parseExpression());
        }

        context.consume(TokenType.RIGHT_BRACE,"Expected '}'"
        );

        if (expressions.isEmpty()) {
            throw new ParseException("Expected at least one expression", context.current());
        }

        return expressions;
    }
}
