package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.parser.ConstraintDslParser;
import at.sfischer.constraints.parser.ConstraintDslScanner;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.parser.UserFunctionCreator;
import at.sfischer.constraints.parser.registry.FunctionCreateException;
import at.sfischer.constraints.parser.registry.FunctionCreator;
import at.sfischer.constraints.parser.registry.FunctionRegistry;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public abstract class DslFunctionCreator implements FunctionCreator {

    private final UserFunctionCreator creator;

    private final String functionName;

    protected DslFunctionCreator(String source) {
        this.creator = parseDefinition(source);
        this.functionName = this.creator.getName();
        FunctionRegistry.register(functionName, this);
    }

    private static UserFunctionCreator parseDefinition(String source) {
        try {
            ConstraintDslParser parser = new ConstraintDslParser(new ConstraintDslScanner(new StringReader(source)));
            FunctionCreator creator = parser.parseFunctionDefinitionOnly();
            if(creator instanceof UserFunctionCreator userFunctionCreator){
                return userFunctionCreator;
            }

            throw new IllegalArgumentException("FunctionCreator is not instance of UserFunctionCreator, but: " + creator.getClass().getCanonicalName());
        } catch (IOException | ParseException e) {
            throw new IllegalArgumentException("Invalid DSL function definition: " + source, e);
        }
    }

    @Override
    public Function create(List<Node> arguments) throws FunctionCreateException {
        return this.creator.create(arguments);
    }

    public String getFunctionName() {
        return functionName;
    }
}
