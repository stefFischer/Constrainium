package at.sfischer.constraints.solver;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import at.sfischer.constraints.model.operators.strings.*;
import com.microsoft.z3.*;

import java.math.BigInteger;
import java.util.*;

public class SolverZ3 {

    public Map<Variable, Value<?>> solveConstraint(Constraint... constraints) {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        try (Context ctx = new Context(cfg)) {
            Map<Variable, Expr<?>> variables = new HashMap<>();
            Solver solver = ctx.mkSolver();
            for (Constraint constraint : constraints) {
                Expr<?> z3Constraint = addTerm(ctx, constraint.term(), variables);
                solver.add((BoolExpr) z3Constraint);
            }

            Status status = solver.check();
//            System.out.println(status);
            if(status != Status.SATISFIABLE){
                return null;
            }

            Model model = solver.getModel();

            Map<Variable, Value<?>> values = new HashMap<>();
            variables.forEach((variable, variableConst) -> {
                Expr<?> val = model.evaluate(variableConst, true);
                Value<?>  value = convertToValue(ctx, val, model);
                values.put(variable, value);
            });

            return values;
        }
    }

    private Expr<?> addTerm(Context ctx, Node term, Map<Variable, Expr<?>> variables){
        Map<Variable, Type> variableTypes = term.inferVariableTypes();
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
                    variables.put(variable, ctx.mkConst(variable.getName(), ctx.mkArraySort(ctx.getIntSort(), ctx.mkFPSortDouble())));
//                    throw new IllegalStateException("Unsupported type: " + arrayType + " for variable: " + variable);
                }
                default -> throw new IllegalStateException("Unsupported type: " + type + " for variable: " + variable);
            }
        });

        return processTerm(ctx, term, variables);
    }

    private static Value<?> convertToValue(Context ctx, Expr<?> val, Model model){
        switch(val){
            case BoolExpr boolExpr -> {return BooleanLiteral.getBooleanLiteral(boolExprToBoolean(boolExpr));}
            case IntNum intNum -> {return new NumberLiteral(intNum.getBigInteger());}
            case FPNum fpNum -> {
                if (fpNum.isNaN()) {
                    return null;
                }
                return new NumberLiteral(fpNumToDouble(fpNum, model));
            }
            case SeqExpr<?> seqExpr -> {return new StringLiteral(seqExpr.getString());}
            case ArrayExpr<?, ?> arrayExpr -> {
                List<Node> values = new LinkedList<>();
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    IntNum index = ctx.mkInt(i);
                    @SuppressWarnings("unchecked")
                    Expr<?> element = model.eval(ctx.mkSelect((ArrayExpr<IntSort, ?>) arrayExpr, index), true);
                    Value<?> value = convertToValue(ctx, element, model);

                    if (value != null) {
                        values.add(value);
                    } else {
                        break;
                    }
                }
                return ArrayValues.createArrayValuesFromList(values);
            }

            default -> throw new UnsupportedOperationException("Unsupported value: " + val.getClass());
        }

    }

    public static boolean boolExprToBoolean(BoolExpr expr) {
        return expr.isTrue();
    }

    public static double fpNumToDouble(FPNum fpNum, Model model) {
        BitVecExpr signExpr = fpNum.getSignBV();
        BitVecExpr exponentExpr = fpNum.getExponentBV(true);
        BitVecExpr significandExpr = fpNum.getSignificandBV();

        BitVecNum sign = (BitVecNum) model.evaluate(signExpr, true);
        BitVecNum exponent = (BitVecNum) model.evaluate(exponentExpr, true);
        BitVecNum significand = (BitVecNum) model.evaluate(significandExpr, true);

        // Compose the IEEE 754 binary representation
        long signBit = sign.getLong();
        long exponentBits = exponent.getLong();
        long significandBits = significand.getLong();

        // Combine into 64-bit representation
        long ieeeBits = (signBit << 63) | (exponentBits << 52) | (significandBits & 0xFFFFFFFFFFFFFL);

        // Convert to Java double
        return Double.longBitsToDouble(ieeeBits);
    }

    private RealExpr toReal(Context ctx, Expr<?> expr) {
        if (expr instanceof RealExpr real) {
            return real;
        } else if (expr instanceof IntExpr intExpr) {
            return ctx.mkInt2Real(intExpr);
        } else {
            throw new IllegalArgumentException("Cannot convert to RealExpr: " + expr);
        }
    }

    private FPExpr toFP(Context ctx, Expr<?> expr) {
        if (expr instanceof FPExpr fp) {
            return fp;
        } else if (expr instanceof RealExpr real) {
            // Use 64-bit FP sort and rounding mode
            return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), real, ctx.mkFPSortDouble());
        } else if (expr instanceof IntExpr intExpr) {
            RealExpr real = ctx.mkInt2Real(intExpr);
            return ctx.mkFPToFP(ctx.mkFPRoundNearestTiesToEven(), real, ctx.mkFPSortDouble());
        } else {
            throw new IllegalArgumentException("Cannot convert to FPExpr: " + expr);
        }
    }

    private Expr<?> processTerm(Context ctx, Node term, Map<Variable, Expr<?>> variables){
        switch(term){
            case Variable var -> {return variables.get(var);}
            // Literal values.
            // TODO May need to introduce a distinction between int and double literals, so we can generate inputs of the correct type.
            case NumberLiteral number -> {
                if(number.getValue() instanceof Integer || number.getValue() instanceof Long || number.getValue() instanceof BigInteger){
                    return ctx.mkInt(number.getValue().longValue());
                } else {
                    return ctx.mkFP(number.getValue().doubleValue(), ctx.mkFPSortDouble());
                }
            }
            case StringLiteral string -> {
                return ctx.mkString(string.getValue());
            }
            case ArrayValues<?> array -> {
                switch(array.getElementType()){
                    case TypeEnum typeEnum -> {
                        switch (typeEnum){
                            case NUMBER:
                                List<Expr<?>> numbers = new ArrayList<>();
                                boolean isFP = false;
                                NumberLiteral[] numberLiterals = (NumberLiteral[]) array.getValue();
                                for (NumberLiteral numberLiteral : numberLiterals) {
                                    Expr<?> val = processTerm(ctx, numberLiteral, variables);
                                    if(val instanceof FPExpr){
                                        isFP = true;
                                    }
                                    numbers.add(val);
                                }

                                if(isFP){
                                    ArrayExpr<IntSort, FPSort> arr = ctx.mkConstArray(ctx.getIntSort(), ctx.mkFP(0.0, ctx.mkFPSortDouble()));
                                    for (int i = 0; i < numbers.size(); i++) {
                                        //noinspection unchecked
                                        arr = ctx.mkStore(arr, ctx.mkInt(i), (Expr<FPSort>)numbers.get(i));
                                    }
                                    return arr;
                                } else {
                                    ArrayExpr<IntSort, IntSort> arr = ctx.mkConstArray(ctx.getIntSort(), ctx.mkInt(0));
                                    for (int i = 0; i < numbers.size(); i++) {
                                        //noinspection unchecked
                                        arr = ctx.mkStore(arr, ctx.mkInt(i), (Expr<IntSort>)numbers.get(i));
                                    }
                                    return arr;
                                }

                            case BOOLEAN:
                            case STRING:
                            case ANY:
                            case COMPLEXTYPE:
                                throw new UnsupportedOperationException("Unsupported array element type: " + array.getElementType());
                        }
                    }
                    case ArrayType arrayType -> {throw new UnsupportedOperationException("Unsupported array element type: " + array.getElementType());}
                    default -> throw new UnsupportedOperationException("Unsupported array element type: " + array.getElementType());
                }
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
            case GreaterThanOperator a -> {return greaterNumbers(ctx, a, variables);}
            case GreaterThanOrEqualOperator a -> {return greaterEqualsNumbers(ctx, a, variables);}
            case LessThanOperator a -> {return lessNumbers(ctx, a, variables);}
            case LessThanOrEqualOperator a -> {return lessEqualsNumbers(ctx, a, variables);}

            // Other number operators.
            case OneOfNumber a -> {return oneOfNumber(ctx, a, variables);}

            // String operators.
            case StringEquals a -> {return equalStrings(ctx, a, variables);}
            case SubString a -> {return isSubString(ctx, a, variables);}
            case StringLength a -> {return stringLength(ctx, a, variables);}
            case IsNumeric a -> {return isNumeric(ctx, a, variables);}
            case OneOfString a -> {return oneOfString(ctx, a, variables);}

            case IsEmail a -> {return isEmail(ctx, a, variables);}
            case IsUrl a -> {return isUrl(ctx, a, variables);}
            case IsHour a -> {return isHour(ctx, a, variables);}
            case IsDate a -> {return isDate(ctx, a, variables);}
            case IsDateTime a -> {return isDateTime(ctx, a, variables);}

            case StringMatches a -> {return stringMatches(ctx, a, variables);}
            case MatchesRegex a -> {return matchesRegex(ctx, a, variables);}

            // Array operators.
            case ArrayLength a -> {return arrayLength(ctx, a, variables);}

            default -> throw new UnsupportedOperationException("Unsupported term: " + term);
        }

       throw new UnsupportedOperationException("Unsupported term: " + term);
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

    private Expr<?> add(Context ctx, AdditionOperator add, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, add.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, add.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPAdd(
                    ctx.mkFPRoundNearestTiesToEven(),
                    leftFP,
                    rightFP
            );

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkAdd(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkAdd((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private Expr<?> sub(Context ctx, SubtractionOperator sub, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, sub.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, sub.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPSub(
                    ctx.mkFPRoundNearestTiesToEven(),
                    leftFP,
                    rightFP
            );

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkSub(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkSub((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private Expr<?> mul(Context ctx, MultiplicationOperator mul, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, mul.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, mul.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPMul(
                    ctx.mkFPRoundNearestTiesToEven(),
                    leftFP,
                    rightFP
            );

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkMul(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkMul((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private Expr<?> div(Context ctx, DivisionOperator div, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, div.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, div.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPDiv(
                    ctx.mkFPRoundNearestTiesToEven(),
                    leftFP,
                    rightFP
            );

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkDiv(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkDiv((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private IntExpr mod(Context ctx, ModuloOperator mod, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, mod.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, mod.getOperand2(), variables);

        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkMod((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be integer: " + left + ", " + right);
    }

    private BoolExpr equalNumbers(Context ctx, EqualOperator equalOperator, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, equalOperator.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, equalOperator.getOperand2(), variables);
        return mkEqualNumbers(ctx, left, right);
    }

    private BoolExpr mkEqualNumbers(Context ctx, Expr<?> left, Expr<?> right){
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkEq(leftFP, rightFP);

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkEq(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkEq(left, right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private BoolExpr greaterNumbers(Context ctx, GreaterThanOperator greaterThanOperator, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, greaterThanOperator.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, greaterThanOperator.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPGt(leftFP, rightFP);

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkGt(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkGt((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private BoolExpr greaterEqualsNumbers(Context ctx, GreaterThanOrEqualOperator greaterThanOrEqualOperator, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, greaterThanOrEqualOperator.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, greaterThanOrEqualOperator.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPGEq(leftFP, rightFP);

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkGe(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkGe((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private BoolExpr lessNumbers(Context ctx, LessThanOperator lessThanOperator, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, lessThanOperator.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, lessThanOperator.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPLt(leftFP, rightFP);

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkLt(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkLt((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private BoolExpr lessEqualsNumbers(Context ctx, LessThanOrEqualOperator lessThanOrEqualOperator, Map<Variable, Expr<?>> variables){
        Expr<?> left = processTerm(ctx, lessThanOrEqualOperator.getOperand1(), variables);
        Expr<?> right = processTerm(ctx, lessThanOrEqualOperator.getOperand2(), variables);
        if (left instanceof FPExpr || right instanceof FPExpr) {
            FPExpr leftFP = toFP(ctx, left);
            FPExpr rightFP = toFP(ctx, right);
            return ctx.mkFPLEq(leftFP, rightFP);

        } else if (left instanceof RealExpr || right instanceof RealExpr) {
            RealExpr leftReal = toReal(ctx, left);
            RealExpr rightReal = toReal(ctx, right);
            return ctx.mkLe(leftReal, rightReal);

        } else if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkLe((IntExpr) left, (IntExpr) right);
        }

        throw new IllegalArgumentException("Operands must be numeric: " + left + ", " + right);
    }

    private BoolExpr oneOfNumber(Context ctx, OneOfNumber oneOfNumber, Map<Variable, Expr<?>> variables){
        Expr<?> varialbeExpr = processTerm(ctx, oneOfNumber.getParameter(0), variables);
        List<BoolExpr> oneOf = new LinkedList<>();
        ArrayValues<?> options = oneOfNumber.getArrayArgument(2);
        for (Object option : options.getValue()) {
            if(!(option instanceof NumberLiteral)){
                continue;
            }

            oneOf.add(
                    mkEqualNumbers(ctx, varialbeExpr, processTerm(ctx, (NumberLiteral)option, variables))
            );
        }

        return ctx.mkOr(oneOf.toArray(new BoolExpr[0]));
    }

    private BoolExpr equalStrings(Context ctx, StringEquals stringEquals, Map<Variable, Expr<?>> variables){
        return ctx.mkEq(
                processTerm(ctx, stringEquals.getParameter(0), variables),
                processTerm(ctx, stringEquals.getParameter(1), variables)
        );
    }

    private BoolExpr isSubString(Context ctx, SubString subString, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str1 = (SeqExpr<CharSort>) processTerm(ctx, subString.getParameter(0), variables);
        //noinspection unchecked
        SeqExpr<CharSort> str2 = (SeqExpr<CharSort>) processTerm(ctx, subString.getParameter(1), variables);

        BoolExpr contains = ctx.mkContains(str1, str2);
        BoolExpr notEquals = ctx.mkNot(ctx.mkEq(str1, str2));
        BoolExpr firstGTZero = ctx.mkGt(ctx.mkLength(str1), ctx.mkInt(0));
        BoolExpr secondGTZero = ctx.mkGt(ctx.mkLength(str2), ctx.mkInt(0));

        return ctx.mkAnd(contains, notEquals, firstGTZero, secondGTZero);
    }

    private IntExpr stringLength(Context ctx, StringLength stringLength, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, stringLength.getParameter(0), variables);
        return ctx.mkLength(str);
    }

    private BoolExpr stringMatches(Context ctx, StringMatches stringMatches, Map<Variable, Expr<?>> variables) {
        // TODO Regex are not fully supported yet.
        SimpleRegexParser parser = new SimpleRegexParser(ctx);
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, stringMatches.getParameter(0), variables);
        Node regexPara = stringMatches.getParameter(1);
        if(!(regexPara instanceof StringLiteral regexLiteral)){
            throw new UnsupportedOperationException("Regex has to be a StringLiteral.");
        }

        ReExpr<SeqSort<CharSort>> regexAst = parser.parse(regexLiteral.getValue());
        return ctx.mkInRe(str, regexAst);
    }

    private BoolExpr matchesRegex(Context ctx, MatchesRegex matchesRegex, Map<Variable, Expr<?>> variables) {
        // TODO Regex are not fully supported yet.
        SimpleRegexParser parser = new SimpleRegexParser(ctx);
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, matchesRegex.getParameter(0), variables);
        Node regexPara = matchesRegex.getParameter(1);
        if(!(regexPara instanceof StringLiteral regexLiteral)){
            throw new UnsupportedOperationException("Regex has to be a StringLiteral.");
        }

        ReExpr<SeqSort<CharSort>> regexAst = parser.parse(regexLiteral.getValue());
        return ctx.mkInRe(str, regexAst);
    }

    private BoolExpr isNumeric(Context ctx, IsNumeric isNumeric, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isNumeric.getParameter(0), variables);
        ReExpr<SeqSort<CharSort>> regexAst = ctx.mkPlus(RegexHelper.digit(ctx));
        return ctx.mkInRe(str, regexAst);
    }

    private BoolExpr isEmail(Context ctx, IsEmail isEmail, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isEmail.getParameter(0), variables);
        ReExpr<SeqSort<CharSort>> regexAst = RegexHelper.getEmailRegex(ctx);
        return ctx.mkInRe(str, regexAst);
    }

    private BoolExpr isUrl(Context ctx, IsUrl isUrl, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isUrl.getParameter(0), variables);
        ReExpr<SeqSort<CharSort>> regexAst = RegexHelper.getUrlRegex(ctx);
        return ctx.mkInRe(str, regexAst);
    }

    private BoolExpr isHour(Context ctx, IsHour isHour, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isHour.getParameter(0), variables);
        ArrayValues<?> patterns = isHour.getArrayArgument(1);

        List<BoolExpr> oneOf = new LinkedList<>();
        for (Value<?> value : patterns.getValue()) {
            if(value instanceof StringLiteral){
                ReExpr<SeqSort<CharSort>> regexAst = RegexHelper.regexFromDateTimePattern(ctx, ((StringLiteral) value).getValue());
                oneOf.add(ctx.mkInRe(str, regexAst));
            }
        }

        return ctx.mkOr(oneOf.toArray(new BoolExpr[0]));
    }

    private BoolExpr isDate(Context ctx, IsDate isDate, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isDate.getParameter(0), variables);
        ArrayValues<?> patterns = isDate.getArrayArgument(1);

        List<BoolExpr> oneOf = new LinkedList<>();
        for (Value<?> value : patterns.getValue()) {
            if(value instanceof StringLiteral){
                ReExpr<SeqSort<CharSort>> regexAst = RegexHelper.regexFromDateTimeFormatterPattern(ctx, ((StringLiteral) value).getValue());
                oneOf.add(ctx.mkInRe(str, regexAst));
            }
        }

        return ctx.mkOr(oneOf.toArray(new BoolExpr[0]));
    }

    private BoolExpr isDateTime(Context ctx, IsDateTime isDateTime, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        SeqExpr<CharSort> str = (SeqExpr<CharSort>) processTerm(ctx, isDateTime.getParameter(0), variables);
        ArrayValues<?> patterns = isDateTime.getArrayArgument(1);

        List<BoolExpr> oneOf = new LinkedList<>();
        for (Value<?> value : patterns.getValue()) {
            if(value instanceof StringLiteral){
                ReExpr<SeqSort<CharSort>> regexAst = RegexHelper.regexFromDateTimeFormatterPattern(ctx, ((StringLiteral) value).getValue());
                oneOf.add(ctx.mkInRe(str, regexAst));
            }
        }

        return ctx.mkOr(oneOf.toArray(new BoolExpr[0]));
    }

    private BoolExpr oneOfString(Context ctx, OneOfString oneOfString, Map<Variable, Expr<?>> variables){
        Expr<?> varialbeExpr = processTerm(ctx, oneOfString.getParameter(0), variables);
        List<BoolExpr> oneOf = new LinkedList<>();
        ArrayValues<?> options = oneOfString.getArrayArgument(2);
        for (Object option : options.getValue()) {
            if(!(option instanceof StringLiteral)){
                continue;
            }

            oneOf.add(
                    ctx.mkEq(varialbeExpr, processTerm(ctx, (StringLiteral)option, variables))
            );
        }

        return ctx.mkOr(oneOf.toArray(new BoolExpr[0]));
    }

    private IntExpr arrayLength(Context ctx, ArrayLength arrayLength, Map<Variable, Expr<?>> variables) {
        //noinspection unchecked
        Expr<?> arr = processTerm(ctx, arrayLength.getParameter(0), variables);

        // Assume variables map has a separate variable for the length,
        // e.g. the array length variable is named something like "arr_len"
        String lengthVarName = arrayLength.getParameter(0).toString() + "_len";

        IntExpr lengthVar = (IntExpr) variables.get(new Variable(lengthVarName));
        if (lengthVar == null) {
            // Create length variable if not existing
            lengthVar = ctx.mkIntConst(lengthVarName);
            variables.put(new Variable(lengthVarName), lengthVar);

            // You should add constraints elsewhere to limit lengthVar >= 0, etc.
        }

        // TODO Need to link lengthVar to the actual array content. Make a map to link array to lengthVar and add the link constraints in the end.
//        for (int i = 0; i < 1000; i++) {
//            BoolExpr inBounds = ctx.mkLt(ctx.mkInt(i), lengthVar);
//            BoolExpr isSet = ctx.mkNot(ctx.mkFPEq(
//                    ctx.mkSelect((ArrayExpr<IntSort, FPSort>)arr, ctx.mkInt(i)),
//                    ctx.mkFPNaN(ctx.mkFPSortDouble())
//            ));
//            solver.add(ctx.mkImplies(inBounds, isSet));
//        }

        return lengthVar;
    }

}
