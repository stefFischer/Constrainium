package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.operators.Function;
import at.sfischer.constraints.parser.registry.FunctionCreateException;
import at.sfischer.constraints.parser.registry.FunctionCreator;
import at.sfischer.constraints.parser.registry.FunctionRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {

    private final Scope parent;

    private final Map<String, FunctionCreator> functions = new HashMap<>();

    public Scope() {
        this(null);
    }

    private Scope(Scope parent) {
        this.parent = parent;
    }

    public void register(String name, FunctionCreator creator) {
        functions.put(name.toLowerCase(), creator);
    }

    public Collection<FunctionCreator> getFunctions() {
        return functions.values();
    }

    public FunctionCreator getCreator(String name) {
        FunctionCreator creator = this.functions.get(name.toLowerCase());
        if (creator != null) {
            return creator;
        }

        if (this.parent == null) {
            return FunctionRegistry.getCreator(name);
        } else {
            return this.parent.getCreator(name);
        }
    }

    public Function create(String name, List<Node> args) throws FunctionCreateException {
        FunctionCreator creator = getCreator(name);
        try {
            return creator.create(args);
        } catch (Exception e) {
            throw new FunctionCreateException("Exception happened while trying to create function: " + name, e);
        }
    }

    public Scope push(){
        return new Scope(this);
    }

    public Scope pop(){
        return this.parent;
    }
}
