package at.sfischer.traces.otel.testing;

public class ObjectEqualsMatch<T> implements Match<T> {

    private final T value;

    public ObjectEqualsMatch(T value) {
        this.value = value;
    }

    @Override
    public MatchResult matches(T toCheck) {
        if(toCheck == this.value){
            return MatchResult.SUCCESS;
        }

        if(toCheck == null){
            return new FailExpectedValueResult(this.value, null);
        }

        if(!toCheck.equals(value)){
            return new FailExpectedValueResult(value, toCheck);
        }

        return MatchResult.SUCCESS;
    }
}
