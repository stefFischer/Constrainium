package at.sfischer.constraints.parser;

import at.sfischer.constraints.ConstraintConstruct;
import at.sfischer.constraints.miner.ConstraintPolicy;

import java.io.IOException;
import java.util.Map;

public interface ConstraintConstructParser<T extends ConstraintConstruct> {
    /** The keyword token that introduces this construct, e.g. MetamorphicTokenType.METAMORPHIC. */
    TokenKind startToken();

    /** Extra keyword -> token kind mappings this module contributes to the scanner. */
    Map<String, TokenKind> keywords();

    /** Parse the full construct, including consuming its own start token. */
    T parse(ExtensionParserContext context, ConstraintPolicy defaultPolicy, ConstraintPolicy groupPolicy, Map<String, ConstraintPolicy> policies) throws IOException, ParseException;
}
