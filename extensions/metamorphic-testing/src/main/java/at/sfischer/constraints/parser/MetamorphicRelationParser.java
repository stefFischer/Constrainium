package at.sfischer.constraints.parser;

import at.sfischer.constraints.MetamorphicRelationTemplate;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.io.IOException;
import java.util.Map;

public class MetamorphicRelationParser implements ConstraintConstructParser<MetamorphicRelationTemplate>{

    @Override
    public TokenKind startToken() {
        return MetamorphicTokenType.METAMORPHIC;
    }

    @Override
    public Map<String, TokenKind> keywords() {
        return Map.of(
        "metamorphic", MetamorphicTokenType.METAMORPHIC,
        "transformation", MetamorphicTokenType.TRANSFORMATION,
        "validation", MetamorphicTokenType.VALIDATION
        );
    }

    @Override
    public MetamorphicRelationTemplate parse(ExtensionParserContext context, ConstraintPolicy defaultPolicy, ConstraintPolicy groupPolicy, Map<String, ConstraintPolicy> policies) throws IOException, ParseException {
        context.consume(MetamorphicTokenType.METAMORPHIC, "Expected 'metamorphic'");
        String name = context.consume(TokenType.IDENTIFIER, "Expected metamorphic relation name").getLexeme();

        context.consume(TokenType.COLON, "Expected ':'");

        context.consume(MetamorphicTokenType.TRANSFORMATION, "Expected 'transformation'");
        context.consume(TokenType.COLON, "Expected ':' after 'transformation'");
        Node transformation = context.parseExpression();

        context.consume(MetamorphicTokenType.VALIDATION, "Expected 'validation'");
        context.consume(TokenType.COLON, "Expected ':' after 'validation'");
        Node validation = context.parseExpression();

        ConstraintPolicy policy;
        if (context.match(TokenType.POLICY)) {
            context.consume(TokenType.ASSIGN, "Expected '='");
            String policyRef = context.consume(TokenType.IDENTIFIER, "Expected policy name").getLexeme();
            policy = policies.get(policyRef);
        } else if(groupPolicy != null) {
            policy = groupPolicy;
        } else {
            policy = defaultPolicy;
        }

        return new MetamorphicRelationTemplate(name, transformation, validation, policy);
    }
}
