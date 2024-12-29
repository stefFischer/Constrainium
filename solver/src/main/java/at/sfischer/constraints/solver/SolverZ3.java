package at.sfischer.constraints.solver;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import com.microsoft.z3.*;

import java.util.HashMap;
import java.util.Map;

public class SolverZ3 {

    public void solveConstraint(Constraint constraint) {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        try (Context ctx = new Context(cfg)) {
            Node term = constraint.term();
            Map<Variable, Type> variableTypes = term.inferVariableTypes();
            Map<Variable, Expr<?>> variables = new HashMap<>();
            variableTypes.forEach((variable, type) -> {
                switch(type){
                    case TypeEnum typeEnum -> {
                        switch (typeEnum){
                            case ANY, COMPLEXTYPE -> {
                                throw new IllegalStateException("Unsupported type: " + typeEnum + " for variable: " + variable);
                            }
                            case NUMBER -> {
                                variables.put(variable, ctx.mkConst(variable.getName(), ctx.mkFPSortDouble()));
                            }
                            case BOOLEAN -> {
                                variables.put(variable, ctx.mkBoolConst(variable.getName()));
                            }
                            case STRING -> {
                                variables.put(variable, ctx.mkConst(variable.getName(), ctx.getStringSort()));
                            }
                        }
                    }
                    case ArrayType arrayType -> {
                        throw new IllegalStateException("Unsupported type: " + arrayType + " for variable: " + variable);
                    }
                    default -> throw new IllegalStateException("Unsupported type: " + type + " for variable: " + variable);
                }
            });

            Expr<?> z3Constraint = processTerm(ctx, term, variables);

            Solver solver = ctx.mkSolver();
            solver.add((BoolExpr) z3Constraint);

            Status status = solver.check();

            System.out.println(status);

            Model model = solver.getModel();

            System.out.println(model);

            variables.forEach((variable, variableConst) -> {
                Expr<?> val = model.evaluate(variableConst, true);
                System.out.println(variable.getName() + " : " + val);
                System.out.println(val.getClass());

                if(val instanceof FPNum){
                    System.out.println(((FPNum) val).getSignificand());
                    System.out.println(((FPNum) val).getSignificandUInt64());
                    System.out.println(((FPNum) val).getSign());
                }
            });
        }
    }

    private Expr<?> processTerm(Context ctx, Node term, Map<Variable, Expr<?>> variables){
        switch(term){
            case Variable var -> {return variables.get(var);}
            // Literal values.
            // TODO May need to introduce a distinction between int and double literals, so we can generate inputs of the correct type.
            case NumberLiteral number -> {
                return ctx.mkFP(number.getValue().doubleValue(), ctx.mkFPSortDouble());
//                return ctx.mkReal(number.getValue().longValue());
            }
            // Logical operators.
            case AndOperator a -> {return and(ctx, a, variables);}
            case OrOperator a -> {return or(ctx, a, variables);}
            case NotOperator a -> {return not(ctx, a, variables);}

            // Arithmetic operators.
            case AdditionOperator a -> {return add(ctx, a, variables);}
            case SubtractionOperator a -> {return sub(ctx, a, variables);}
            case MultiplicationOperator a -> {return mul(ctx, a, variables);}
            case DivisionOperator a -> {return div(ctx, a, variables);}
            case ModuloOperator a -> {return mod(ctx, a, variables);}

            // Number comparison operators.
            case EqualOperator a -> {return equalNumbers(ctx, a, variables);}

            default -> throw new IllegalStateException("Unsupported term: " + term);
        }
    }

    private BoolExpr and(Context ctx, AndOperator and, Map<Variable, Expr<?>> variables){
        return ctx.mkAnd(
                (BoolExpr) processTerm(ctx, and.getOperand1(), variables),
                (BoolExpr) processTerm(ctx, and.getOperand2(), variables)
        );
    }

    private BoolExpr or(Context ctx, OrOperator or, Map<Variable, Expr<?>> variables){
        return ctx.mkOr(
                (BoolExpr) processTerm(ctx, or.getOperand1(), variables),
                (BoolExpr) processTerm(ctx, or.getOperand2(), variables)
        );
    }

    private BoolExpr not(Context ctx, NotOperator not, Map<Variable, Expr<?>> variables){
        return ctx.mkNot(
                (BoolExpr) processTerm(ctx, not.getOperand(), variables)
        );
    }

    private FPExpr add(Context ctx, AdditionOperator add, Map<Variable, Expr<?>> variables){
        return ctx.mkFPAdd(
                ctx.mkFPRoundNearestTiesToEven(),
                (FPExpr) processTerm(ctx, add.getOperand1(), variables),
                (FPExpr) processTerm(ctx, add.getOperand2(), variables)
        );
//        return (RealExpr) ctx.mkAdd(
//                (RealExpr) processTerm(ctx, add.getOperand1(), variables),
//                (RealExpr) processTerm(ctx, add.getOperand2(), variables)
//        );
    }

    private FPExpr sub(Context ctx, SubtractionOperator sub, Map<Variable, Expr<?>> variables){
        return ctx.mkFPSub(
                ctx.mkFPRoundNearestTiesToEven(),
                (FPExpr) processTerm(ctx, sub.getOperand1(), variables),
                (FPExpr) processTerm(ctx, sub.getOperand2(), variables)
        );
//        return (RealExpr) ctx.mkSub(
//                (RealExpr) processTerm(ctx, sub.getOperand1(), variables),
//                (RealExpr) processTerm(ctx, sub.getOperand2(), variables)
//        );
    }

    private FPExpr mul(Context ctx, MultiplicationOperator sub, Map<Variable, Expr<?>> variables){
        return ctx.mkFPMul(
                ctx.mkFPRoundNearestTiesToEven(),
                (FPExpr) processTerm(ctx, sub.getOperand1(), variables),
                (FPExpr) processTerm(ctx, sub.getOperand2(), variables)
        );
    }

    private FPExpr div(Context ctx, DivisionOperator sub, Map<Variable, Expr<?>> variables){
        return ctx.mkFPDiv(
                ctx.mkFPRoundNearestTiesToEven(),
                (FPExpr) processTerm(ctx, sub.getOperand1(), variables),
                (FPExpr) processTerm(ctx, sub.getOperand2(), variables)
        );
    }

    private IntExpr mod(Context ctx, ModuloOperator sub, Map<Variable, Expr<?>> variables){
        return ctx.mkMod(
                (IntExpr) processTerm(ctx, sub.getOperand1(), variables),
                (IntExpr) processTerm(ctx, sub.getOperand2(), variables)
        );
    }

    private BoolExpr equalNumbers(Context ctx, EqualOperator equalOperator, Map<Variable, Expr<?>> variables){
        return ctx.mkEq(
                processTerm(ctx, equalOperator.getOperand1(), variables),
                processTerm(ctx, equalOperator.getOperand2(), variables)
        );
    }
}
