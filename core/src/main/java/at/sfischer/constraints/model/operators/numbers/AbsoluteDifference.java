package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.operators.DslFunctionCreator;

public final class AbsoluteDifference extends DslFunctionCreator {

    private AbsoluteDifference() {
        super("""
            function absoluteDifference(a, b) {
                abs(a - b)
            }
            """
        );
    }
}
