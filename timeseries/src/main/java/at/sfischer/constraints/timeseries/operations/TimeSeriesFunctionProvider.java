package at.sfischer.constraints.timeseries.operations;

import at.sfischer.constraints.parser.registry.FunctionProvider;
import at.sfischer.constraints.parser.registry.FunctionRegistry;

public class TimeSeriesFunctionProvider implements FunctionProvider {
    @Override
    public void registerFunctions() {
        FunctionRegistry.autoRegister(TimeSeriesFunctionProvider.class.getPackageName());
    }
}
