package at.sfischer.traces.otel.testing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringRegexMatch implements Match<String> {

    private final String valueRegex;

    public StringRegexMatch(String valueRegex) {
        this.valueRegex = valueRegex;
    }

    @Override
    public MatchResult matches(String toCheck) {
        if(toCheck == null){
            return new FailExpectedValueResult(this.valueRegex, null);
        }

        Pattern pattern = Pattern.compile(valueRegex);
        Matcher matcher = pattern.matcher(toCheck);
        if(!matcher.find()){
            return new FailExpectedValueResult(valueRegex, toCheck);
        }

        return MatchResult.SUCCESS;
    }
}
