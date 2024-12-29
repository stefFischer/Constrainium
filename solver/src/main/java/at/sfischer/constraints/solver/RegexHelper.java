package at.sfischer.constraints.solver;

import com.microsoft.z3.*;

import java.util.ArrayList;
import java.util.List;

public class RegexHelper {

    public static ReExpr<SeqSort<CharSort>> reStr(Context ctx, String s) {
        return ctx.mkToRe(ctx.mkString(s));
    }

    public static ReExpr<SeqSort<CharSort>> charRange(Context ctx, char lo, char hi){
        return ctx.mkRange(ctx.mkString(lo + ""), ctx.mkString(hi + ""));
    }

    public static ReExpr<SeqSort<CharSort>> upper(Context ctx){
        return charRange(ctx, 'A', 'Z');
    }

    public static ReExpr<SeqSort<CharSort>> lower (Context ctx){
        return charRange(ctx, 'a', 'z');
    }

    public static ReExpr<SeqSort<CharSort>> alpha (Context ctx){
        return ctx.mkUnion(upper(ctx), lower(ctx));
    }

    public static ReExpr<SeqSort<CharSort>> digit (Context ctx){
        return charRange(ctx, '0', '9');
    }

    public static ReExpr<SeqSort<CharSort>> alphaNum (Context ctx){
        return ctx.mkUnion(alpha(ctx), digit(ctx));
    }

    public static ReExpr<SeqSort<CharSort>> getEmailRegex(Context ctx){
        ReExpr<SeqSort<CharSort>> dot = reStr(ctx, ".");
        ReExpr<SeqSort<CharSort>> at = reStr(ctx, "@");

        ReExpr<SeqSort<CharSort>> localChar = ctx.mkUnion(
                alphaNum(ctx),
                ctx.mkUnion(dot, reStr(ctx, "_"), reStr(ctx, "-"))
        );

        ReExpr<SeqSort<CharSort>> localPart = ctx.mkConcat(
                ctx.mkLoop(alphaNum(ctx), 1, 20),
                ctx.mkLoop(localChar, 1, 20)
        );

        ReExpr<SeqSort<CharSort>> domainLabel = ctx.mkLoop(alpha(ctx), 2, 10);

        ReExpr<SeqSort<CharSort>> domain = ctx.mkConcat(
                domainLabel,
                ctx.mkLoop(ctx.mkConcat(dot, domainLabel), 1, 3)
        );

        return ctx.mkConcat(localPart, at, domain);
    }

    public static ReExpr<SeqSort<CharSort>> getUrlRegex(Context ctx) {
        // Basic components
        ReExpr<SeqSort<CharSort>> colonSlashSlash = reStr(ctx, "://");
        ReExpr<SeqSort<CharSort>> dot = reStr(ctx, ".");
        ReExpr<SeqSort<CharSort>> slash = reStr(ctx, "/");

        // Scheme: http or https
        ReExpr<SeqSort<CharSort>> scheme = ctx.mkUnion(reStr(ctx, "http"), reStr(ctx, "https"));

        // Domain labels (e.g. "example", "sub1", "my-site")
        ReExpr<SeqSort<CharSort>> domainChar = ctx.mkUnion(
                alphaNum(ctx),
                reStr(ctx, "-")
        );
        ReExpr<SeqSort<CharSort>> domainLabel = ctx.mkLoop(domainChar, 1, 20); // allow hyphenated names

        // Subdomains (e.g. sub.domain)
        ReExpr<SeqSort<CharSort>> subdomains = ctx.mkLoop(ctx.mkConcat(domainLabel, dot), 0, 3);

        // TLD (e.g. com, org, co.uk)
        ReExpr<SeqSort<CharSort>> tld = ctx.mkLoop(alpha(ctx), 2, 10);

        // Host part: (sub.)*domain.tld
        ReExpr<SeqSort<CharSort>> host = ctx.mkConcat(subdomains, domainLabel, dot, tld);

        // Path segments (optional)
        ReExpr<SeqSort<CharSort>> pathChar = ctx.mkUnion(
                alphaNum(ctx),
                ctx.mkUnion(reStr(ctx, "-"), reStr(ctx, "_"))
        );
        ReExpr<SeqSort<CharSort>> pathSegment = ctx.mkLoop(pathChar, 1, 20);
        ReExpr<SeqSort<CharSort>> path = ctx.mkLoop(ctx.mkConcat(slash, pathSegment), 0, 10);

        // Optional final slash.
        ReExpr<SeqSort<CharSort>> finalSlash = ctx.mkLoop(slash, 0, 1);

        // Full URL: scheme + "://" + host + path
        return ctx.mkConcat(scheme, colonSlashSlash, host, path, finalSlash);
    }

    public static ReExpr<SeqSort<CharSort>> twoDigitRangeRegex(Context ctx, int min, int max) {
        List<ReExpr<SeqSort<CharSort>>> options = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            String s = String.format("%02d", i); // zero-padded
            options.add(reStr(ctx, s));
        }
        //noinspection unchecked
        return ctx.mkUnion(options.toArray(new ReExpr[0]));
    }

    public static ReExpr<SeqSort<CharSort>> oneOrTwoDigitRangeRegex(Context ctx, int min, int max) {
        List<ReExpr<SeqSort<CharSort>>> options = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            options.add(reStr(ctx, String.valueOf(i)));
        }
        //noinspection unchecked
        return ctx.mkUnion(options.toArray(new ReExpr[0]));
    }

    public static ReExpr<SeqSort<CharSort>> fixedDigitRegex(Context ctx, int digits) {
        return ctx.mkLoop(digit(ctx), digits, digits);
    }

    public static ReExpr<SeqSort<CharSort>> fixedDigitNonZeroRegex(Context ctx, int digits) {
        ReExpr<SeqSort<CharSort>> allDigits = ctx.mkLoop(digit(ctx), digits, digits); // any digits
        ReExpr<SeqSort<CharSort>> allZeros = ctx.mkLoop(reStr(ctx, "0"), digits, digits); // all zeros
        return ctx.mkDiff(allDigits, allZeros);
    }

    public static ReExpr<SeqSort<CharSort>> regexFromDateTimePattern(Context ctx, String pattern) {
        List<ReExpr<SeqSort<CharSort>>> parts = new ArrayList<>();

        for (int i = 0; i < pattern.length(); ) {
            if (pattern.startsWith("HH", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 23));
                i += 2;
            } else if (pattern.startsWith("hh", i)) {
                parts.add(twoDigitRangeRegex(ctx, 1, 12));
                i += 2;
            } else if (pattern.startsWith("mm", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 59));
                i += 2;
            } else if (pattern.startsWith("ss", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 59));
                i += 2;
            } else if (pattern.startsWith("a", i)) {
                parts.add(ctx.mkUnion(reStr(ctx, "AM"), reStr(ctx, "PM")));
                i += 1;
            } else {
                char ch = pattern.charAt(i);
                parts.add(reStr(ctx, Character.toString(ch)));
                i += 1;
            }
        }

        //noinspection unchecked
        return ctx.mkConcat((ReExpr<SeqSort<CharSort>>[]) parts.toArray(new ReExpr[0]));
    }

    public static ReExpr<SeqSort<CharSort>> regexFromDateTimeFormatterPattern(Context ctx, String pattern) {
        List<ReExpr<SeqSort<CharSort>>> parts = new ArrayList<>();

        for (int i = 0; i < pattern.length(); ) {
            if (pattern.startsWith("yyyy", i)) {
                parts.add(fixedDigitNonZeroRegex(ctx, 4));
                i += 4;
            } else if (pattern.startsWith("yy", i)) {
                parts.add(fixedDigitNonZeroRegex(ctx, 2));
                i += 2;
            } else if (pattern.startsWith("u", i)) {
                parts.add(
                        ctx.mkConcat(
                                ctx.mkUnion(
                                        reStr(ctx, "-"),
                                        reStr(ctx, "")
                                ),
                                fixedDigitNonZeroRegex(ctx, 4)
                        )
                );
                i += 1;
            } else if (pattern.startsWith("MMMM", i)) {
                parts.add(ctx.mkUnion(
                        reStr(ctx,"January"),
                        reStr(ctx,"February"),
                        reStr(ctx,"March"),
                        reStr(ctx,"April"),
                        reStr(ctx,"May"),
                        reStr(ctx,"June"),
                        reStr(ctx,"July"),
                        reStr(ctx,"August"),
                        reStr(ctx,"September"),
                        reStr(ctx,"October"),
                        reStr(ctx,"November"),
                        reStr(ctx,"December")
                ));
                i += 4;
            } else if (pattern.startsWith("EEE", i)) {
                parts.add(ctx.mkUnion(
                        reStr(ctx, "Mon"),
                        reStr(ctx, "Tue"),
                        reStr(ctx, "Wed"),
                        reStr(ctx, "Thu"),
                        reStr(ctx, "Fri"),
                        reStr(ctx, "Sat"),
                        reStr(ctx, "Sun")
                ));
                i += 3;
            } else if (pattern.startsWith("Z", i)) {
                parts.add(timeZoneZ(ctx));
                i += 1;
            } else if (pattern.startsWith("XXX", i)) {
                parts.add(timeZoneXXX(ctx));
                i += 3;
            } else if (pattern.startsWith("MM", i)) {
                parts.add(twoDigitRangeRegex(ctx, 1, 12));
                i += 2;
            } else if (pattern.startsWith("M", i)) {
                parts.add(oneOrTwoDigitRangeRegex(ctx, 1, 12));
                i += 1;
            } else if (pattern.startsWith("dd", i)) {
                parts.add(twoDigitRangeRegex(ctx, 1, 31));
                i += 2;
            } else if (pattern.startsWith("d", i)) {
                parts.add(oneOrTwoDigitRangeRegex(ctx, 1, 31));
                i += 1;
            } else if (pattern.startsWith("HH", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 23));
                i += 2;
            } else if (pattern.startsWith("hh", i)) {
                parts.add(twoDigitRangeRegex(ctx, 1, 12));
                i += 2;
            } else if (pattern.startsWith("mm", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 59));
                i += 2;
            } else if (pattern.startsWith("ss", i)) {
                parts.add(twoDigitRangeRegex(ctx, 0, 59));
                i += 2;
            } else if (pattern.startsWith("SSS", i)) {
                parts.add(fixedDigitRegex(ctx, 3));
                i += 3;
            } else if (pattern.startsWith("SS", i)) {
                parts.add(fixedDigitRegex(ctx, 2));
                i += 2;
            } else if (pattern.startsWith("S", i)) {
                parts.add(fixedDigitRegex(ctx, 1));
                i += 1;
            } else if (pattern.charAt(i) == '\'') {
                // Handle quoted literal
                int end = pattern.indexOf('\'', i + 1);
                if (end > i) {
                    String literal = pattern.substring(i + 1, end);
                    parts.add(reStr(ctx, literal));
                    i = end + 1;
                } else {
                    throw new IllegalArgumentException("Unclosed quote in pattern");
                }
            } else {
                char ch = pattern.charAt(i);
                parts.add(reStr(ctx, Character.toString(ch)));
                i += 1;
            }
        }

        //noinspection unchecked
        return ctx.mkConcat((ReExpr<SeqSort<CharSort>>[]) parts.toArray(new ReExpr[0]));
    }

    // Matches "Z" or +hhmm
    public static ReExpr<SeqSort<CharSort>> timeZoneZ(Context ctx) {
        ReExpr<SeqSort<CharSort>> z = reStr(ctx, "Z");
        ReExpr<SeqSort<CharSort>> offset = ctx.mkConcat(
                ctx.mkUnion(reStr(ctx, "+"), reStr(ctx, "-")),
                twoDigitRangeRegex(ctx, 0, 18),
                ctx.mkUnion(reStr(ctx, "00"), reStr(ctx, "15"), reStr(ctx, "30"), reStr(ctx, "45"))
        );
        return ctx.mkUnion(z, offset);
    }

    // Matches "Z" or +hh:mm
    public static ReExpr<SeqSort<CharSort>> timeZoneXXX(Context ctx) {
        ReExpr<SeqSort<CharSort>> z = reStr(ctx, "Z");
        ReExpr<SeqSort<CharSort>> offset = ctx.mkConcat(
                ctx.mkUnion(reStr(ctx, "+"), reStr(ctx, "-")),
                twoDigitRangeRegex(ctx, 0, 18),
                reStr(ctx, ":"),
                ctx.mkUnion(reStr(ctx, "00"), reStr(ctx, "15"), reStr(ctx, "30"), reStr(ctx, "45"))
        );
        return ctx.mkUnion(z, offset);
    }
}
