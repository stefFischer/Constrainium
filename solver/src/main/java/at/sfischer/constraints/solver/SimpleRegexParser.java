package at.sfischer.constraints.solver;

import com.microsoft.z3.CharSort;
import com.microsoft.z3.Context;
import com.microsoft.z3.ReExpr;
import com.microsoft.z3.SeqSort;

public class SimpleRegexParser {

    private final Context ctx;

    public SimpleRegexParser(Context ctx) {
        this.ctx = ctx;
    }

    // Parses a simple regex pattern into a Z3 ReExpr
    public ReExpr<SeqSort<CharSort>> parse(String pattern) {
        if (pattern.isEmpty()) {
            // Empty pattern matches empty string
            return ctx.mkToRe(ctx.mkString(""));
        }

        ReExpr<SeqSort<CharSort>> result = null;
        int i = 0;

        while (i < pattern.length()) {
            char c = pattern.charAt(i);

            ReExpr<SeqSort<CharSort>> nextRegex;

            if (c == '*') {
                throw new UnsupportedOperationException("'*' cannot appear at the beginning or without preceding token");
            }

            // Literal character regex
            nextRegex = ctx.mkToRe(ctx.mkString(Character.toString(c)));

            // Check if next char is '*'
            if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                nextRegex = ctx.mkStar(nextRegex);
                i += 1; // skip the '*'
            } else if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '+') {
                nextRegex = ctx.mkPlus(nextRegex);
                i += 1; // skip the '+'
            }

            if (result == null) {
                result = nextRegex;
            } else {
                result = ctx.mkConcat(result, nextRegex);
            }
            i++;
        }

        return result;
    }
}

