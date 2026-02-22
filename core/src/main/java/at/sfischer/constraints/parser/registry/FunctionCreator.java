package at.sfischer.constraints.parser.registry;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;

@FunctionalInterface
public interface FunctionCreator {
    Function create(List<Node> arguments) throws FunctionCreateException;
}
