package at.sfischer.constraints.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class ConstraintConstructParserRegistry {
    private static volatile Map<TokenKind, ConstraintConstructParser<?>> parsers;
    private static volatile Map<String, TokenKind> keywords;

    private static synchronized Map<TokenKind, ConstraintConstructParser<?>> load() {
        if (parsers == null) {
            Map<TokenKind, ConstraintConstructParser<?>> map = new HashMap<>();
            Map<String, TokenKind> kwMap = new HashMap<>();
            for (ConstraintConstructParser<?> p : ServiceLoader.load(ConstraintConstructParser.class)) {
                if (map.putIfAbsent(p.startToken(), p) != null) {
                    throw new IllegalStateException("Duplicate extension start token: " + p.startToken());
                }

                for (Map.Entry<String, TokenKind> e : p.keywords().entrySet()) {
                    if (kwMap.putIfAbsent(e.getKey(), e.getValue()) != null) {
                        throw new IllegalStateException("Duplicate keyword registered by extensions: " + e.getKey());
                    }
                    ConstraintDslScanner.registerKeyword(e.getKey(), e.getValue());
                }
            }
            parsers = map;
            keywords = kwMap;
        }
        return parsers;
    }

    public static ConstraintConstructParser<?> forStartToken(TokenKind kind) {
        return load().get(kind);
    }

    public static Map<String, TokenKind> getKeywordExtensions() {
        return keywords;
    }

    private ConstraintConstructParserRegistry() {}
}
