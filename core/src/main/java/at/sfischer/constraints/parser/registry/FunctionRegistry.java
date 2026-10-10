package at.sfischer.constraints.parser.registry;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.operators.DslFunctionCreator;
import at.sfischer.constraints.model.operators.Function;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

public class FunctionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionRegistry.class);

    private static final Map<String, FunctionCreator> FUNCTIONS = new HashMap<>();

    static {
        autoRegister(Function.class.getPackageName());
        registerProviders();
    }

    private FunctionRegistry() {}

    private static void registerProviders() {
        ServiceLoader<FunctionProvider> loader = ServiceLoader.load(FunctionProvider.class);
        for (FunctionProvider provider : loader) {
            try {
                LOGGER.debug("Registering functions from provider {}", provider.getClass().getName());
                provider.registerFunctions();
            } catch (Exception e) {
                LOGGER.error("Failed to register functions from provider {}", provider.getClass().getName(), e);
            }
        }
    }

    public static void register(String name, FunctionCreator creator) {
        FUNCTIONS.put(name.toLowerCase(), creator);
    }

    public static FunctionCreator getCreator(String name) {
        return FUNCTIONS.get(name.toLowerCase());
    }

    public static Function create(String name, List<Node> args) throws FunctionCreateException {
        FunctionCreator creator = FUNCTIONS.get(name.toLowerCase());

        if (creator == null) {
            throw new FunctionCreateException("Unknown function: " + name);
        }

        try {
            return creator.create(args);
        } catch (Exception e) {
            throw new FunctionCreateException("Exception happened while trying to create function: " + name, e);
        }
    }

    public static void autoRegister(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends Function>> classes = reflections.getSubTypesOf(Function.class);
        for (Class<? extends Function> clazz : classes) {
            if(Modifier.isAbstract(clazz.getModifiers())){
                continue;
            }

            registerFunctionClass(clazz);
        }

        Set<Class<? extends DslFunctionCreator>> creatorClasses = reflections.getSubTypesOf(DslFunctionCreator.class);
        for (Class<? extends DslFunctionCreator> clazz : creatorClasses) {
            if(Modifier.isAbstract(clazz.getModifiers())){
                continue;
            }

            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                DslFunctionCreator creator = (DslFunctionCreator) constructor.newInstance();
                register(creator.getFunctionName(), creator);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("Could not register function creator class: {}", clazz.getCanonicalName(), e);
            }
        }
    }

    private static void registerFunctionClass(final Class<? extends Function> clazz) {
        try {
            Field nameField = clazz.getDeclaredField("FUNCTION_NAME");
            nameField.setAccessible(true);
            final String functionName = (String) nameField.get(null);

            final List<FunctionCreator> functionCreators = new ArrayList<>();
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (final Constructor<?> constructor : constructors) {
                if(!allNodeArguments(constructor)){
                    continue;
                }

                functionCreators.add(args -> {
                    // TODO Need to adjust this when we allow List<Node> constructors.
                    if(args.size() != constructor.getParameterCount()){
                        return null;
                    }

                    Parameter[] parameters = constructor.getParameters();
                    Object[] initargs = new Object[parameters.length];
                    for (int i = 0; i < parameters.length; i++) {
                        if(!parameters[i].getType().isAssignableFrom(args.get(i).getClass())){
                            return null;
                        }

                        initargs[i] = args.get(i);
                    }

                    try {
                        return (Function) constructor.newInstance(initargs);
                    } catch (Exception e) {
                        return null;
                    }
                });
            }

            if(functionCreators.isEmpty()){
                throw new IllegalStateException("Could not find a usable constructor for function: " + functionName + ". Please use `FunctionRegistry.register` to register a constructor for this function.");
            }

            register(functionName, args -> {
                Function function = null;
                for (FunctionCreator functionCreator : functionCreators) {
                    Function nextFunction = null;
                    try {
                        nextFunction = functionCreator.create(args);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to instantiate function: {}", functionName, e);
                    }

                    if(function != null && nextFunction != null){
                        throw new FunctionCreateException("Ambiguous function initialization. " + function + " vs. " + nextFunction);
                    }

                    if(nextFunction != null) {
                        function = nextFunction;
                    }
                }

                return function;
            });

        } catch (Exception e) {
            LOGGER.warn("Could not register function class: {}", clazz.getCanonicalName(), e);
        }
    }

    private static boolean allNodeArguments(Constructor<?> constructor){
        for (Parameter parameter : constructor.getParameters()) {
            // TODO We should also support constructors with List<Node> parameters.
            if(!Node.class.isAssignableFrom(parameter.getType())){
                return false;
            }
        }

        return true;
    }
}
