package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayIndex;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import at.sfischer.constraints.model.operators.strings.*;

import java.lang.reflect.Array;
import java.util.*;

import static at.sfischer.constraints.model.pattern.NodePatternMatcher.*;

public class DaikonInvariantToStringUtil {

    public static Set<String> termToDaikonString(final String invariantType, Node term, DataSchemaEntry<?> schemaEntry){
        Set<String> strings = new HashSet<>();
        switch (invariantType){
            case DaikonInvariantTypes.DAIKON_ONE_OF_STRING: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("values");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.strings.OneOfString", refPlaceholder, new NumberLiteral(3), valuesPlaceholder);

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);
                if (values instanceof ArrayValues) {
                    String dataRef;
                    if (ref instanceof Variable) {
                        dataRef = ((Variable) ref).getName();
                    } else {
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    List<StringLiteral> literalValues = getListOfValues((ArrayValues<StringLiteral>) values);
                    if (literalValues.size() == 1) {
                        strings.add("\"" + dataRef + " == \"\"" + literalValues.get(0).getValue() + "\"\"\"");
                        return strings;
                    } else if (literalValues.size() > 1) {
                        String start = "\"" + dataRef + " one of { ";
                        String end = " }" + "\"";
                        strings.addAll(generatePermutationStrings(start, "\"\"", literalValues, "\"\"", end));

                        return strings;
                    }
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_ONE_OF_STRING_SEQUENCE: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("values");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.strings.OneOfStringArray", refPlaceholder, new NumberLiteral(3), valuesPlaceholder);

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);
                if (values instanceof ArrayValues) {
                    String dataRef;
                    if (ref instanceof Variable) {
                        dataRef = ((Variable) ref).getName();
                    } else {
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    List<ArrayValues<StringLiteral>> literalValues = getListOfValues((ArrayValues<ArrayValues<StringLiteral>>) values);
                    if (literalValues.size() == 1) {
                        strings.add(dataRef + "[] == " + formatValue(literalValues.get(0)));
                        return strings;
                    } else if (literalValues.size() > 1) {
                        String start = dataRef + "[] one of { ";
                        String end = " }";
                        strings.addAll(generatePermutationStrings(start, literalValues, end));
                        return strings;
                    }
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_ONE_OF_STRING_IN_SEQUENCE: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("values");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.array.ForAll", refPlaceholder,
                        new PatternNode("at.sfischer.constraints.model.operators.strings.OneOfString", new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(3), valuesPlaceholder)
                );

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);

                String array = getNodeString(ref);

                @SuppressWarnings("unchecked")
                List<StringLiteral> literalValues = getListOfValues((ArrayValues<StringLiteral>) values);
                if (literalValues.size() == 1) {
                    strings.add("\"" + array + "[] elements == \"\"" + literalValues.get(0).getValue() + "\"\"\"");
                    return strings;
                } else if (literalValues.size() > 1) {
                    String start = "\"" + array + "[] elements one of { ";
                    String end = " }" + "\"";
                    strings.addAll(generatePermutationStrings(start, "\"\"", literalValues, "\"\"", end));

                    return strings;
                }


                break;
            }
            case DaikonInvariantTypes.DAIKON_STRING_IN_SEQUENCE: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuePlaceholder = new Placeholder("value");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.array.Exists", refPlaceholder,
                        new PatternNode("at.sfischer.constraints.model.operators.strings.StringEquals", new Variable(ArrayQuantifier.ELEMENT_NAME), valuePlaceholder)
                );

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node value = match.get(valuePlaceholder);

                String array = getNodeString(ref);
                String string = getNodeString(value);

                strings.add(string + " in " + array + "[]");
                return strings;
            }
            case DaikonInvariantTypes.DAIKON_INT_IN_SEQUENCE: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuePlaceholder = new Placeholder("value");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.array.Exists", refPlaceholder,
                        new PatternNode("at.sfischer.constraints.model.operators.numbers.EqualOperator", new Variable(ArrayQuantifier.ELEMENT_NAME), valuePlaceholder)
                );

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node value = match.get(valuePlaceholder);

                String array = getNodeString(ref);
                String num = getNodeString(value);

                strings.add(num + " in " + array + "[]");
                return strings;
            }
            case DaikonInvariantTypes.DAIKON_STRING_EQUAL: {
                if (term instanceof StringEquals) {
                    String operand1 = getNodeString(((StringEquals) term).getParameter(0));
                    String operand2 = getNodeString(((StringEquals) term).getParameter(1));

                    strings.add(operand1 + " == " + operand2);
                    strings.add(operand2 + " == " + operand1);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_STRING_IS_DATE: {
                if (term instanceof IsDate) {
                    String operand1 = getNodeString(((IsDate) term).getParameter(0));
                    strings.add(operand1 + " is a Date. Format: YYYY/MM/DD");
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_STRING_IS_NUMBER: {
                if (term instanceof IsNumeric) {
                    String operand1 = getNodeString(((IsNumeric) term).getParameter(0));
                    strings.add(operand1 + " is Numeric");
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_SUB_STRING: {
                if (term instanceof SubString) {
                    String operand1 = getNodeString(((SubString) term).getParameter(0));
                    String operand2 = getNodeString(((SubString) term).getParameter(1));

                    strings.add(operand2 + " is a substring of " + operand1);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_STRING_FIXED_LENGTH: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("value");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.numbers.OneOfNumber", new StringLength(refPlaceholder), new NumberLiteral(1), valuesPlaceholder);

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);

                String str = getNodeString(ref);
                if (values instanceof ArrayValues) {
                    @SuppressWarnings("unchecked")
                    List<NumberLiteral> literalValues = getListOfValues((ArrayValues<NumberLiteral>) values);
                    String len = getNodeString(literalValues.get(0));

                    strings.add("LENGTH(" + str + ")==" + len);
                    return strings;
                }

                break;
            }
            case DaikonInvariantTypes.DAIKON_STRING_SEQUENCE_FIXED_LENGTH_ELEMENTS: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("value");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.array.ForAll", refPlaceholder,
                        new PatternNode("at.sfischer.constraints.model.operators.numbers.OneOfNumber", new StringLength(new Variable(ArrayQuantifier.ELEMENT_NAME)), new NumberLiteral(1), valuesPlaceholder)
                );

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);

                String array = getNodeString(ref);
                if (values instanceof ArrayValues) {
                    @SuppressWarnings("unchecked")
                    List<NumberLiteral> literalValues = getListOfValues((ArrayValues<NumberLiteral>) values);
                    String len = getNodeString(literalValues.get(0));

                    strings.add("All the elements of " + array + "[] have LENGTH=" + len);
                    return strings;
                }

                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_NOT_EQUAL: {
                if (term instanceof NotOperator) {
                    Node operand = ((NotOperator) term).getOperand();
                    if(operand instanceof EqualOperator){
                        String operand1 = getNodeString(((EqualOperator) operand).getOperand1());
                        String operand2 = getNodeString(((EqualOperator) operand).getOperand2());

                        strings.add(operand1 + " != " + operand2);
                        strings.add(operand2 + " != " + operand1);
                        return strings;
                    }
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_EQUAL: {
                if(term instanceof EqualOperator){
                    String operand1 = getNodeString(((EqualOperator) term).getOperand1());
                    String operand2 = getNodeString(((EqualOperator) term).getOperand2());

                    strings.add(operand1 + " == " + operand2);
                    strings.add(operand2 + " == " + operand1);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_GREATER_EQUAL: {
                if(term instanceof GreaterThanOrEqualOperator){
                    String operand1 = getNodeString(((GreaterThanOrEqualOperator) term).getOperand1());
                    String operand2 = getNodeString(((GreaterThanOrEqualOperator) term).getOperand2());

                    strings.add(operand1 + " >= " + operand2);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_GREATER: {
                if(term instanceof GreaterThanOperator){
                    String operand1 = getNodeString(((GreaterThanOperator) term).getOperand1());
                    String operand2 = getNodeString(((GreaterThanOperator) term).getOperand2());

                    strings.add(operand1 + " > " + operand2);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_LESS_EQUAL: {
                if(term instanceof LessThanOrEqualOperator){
                    String operand1 = getNodeString(((LessThanOrEqualOperator) term).getOperand1());
                    String operand2 = getNodeString(((LessThanOrEqualOperator) term).getOperand2());

                    strings.add(operand1 + " <= " + operand2);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_INT_LESS: {
                if(term instanceof LessThanOperator){
                    String operand1 = getNodeString(((LessThanOperator) term).getOperand1());
                    String operand2 = getNodeString(((LessThanOperator) term).getOperand2());

                    strings.add(operand1 + " < " + operand2);
                    return strings;
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_ONE_OF_SCALAR: {
                Placeholder refPlaceholder = new Placeholder("ref");
                Placeholder valuesPlaceholder = new Placeholder("values");
                Node pattern = new PatternNode("at.sfischer.constraints.model.operators.numbers.OneOfNumber", refPlaceholder, new NumberLiteral(3), valuesPlaceholder);

                Map<Placeholder, Node> match = match(pattern, term);
                if (match == null) {
                    break;
                }

                Node ref = match.get(refPlaceholder);
                Node values = match.get(valuesPlaceholder);
                if (values instanceof ArrayValues) {
                    String dataRef;
                    if (ref instanceof Variable) {
                        dataRef = ((Variable) ref).getName();
                    } else if (ref instanceof ArrayLength) {
                        Node arrayRef = ((ArrayLength) ref).getParameter(0);
                        if (arrayRef instanceof Variable) {
                            dataRef = "size(" + ((Variable) arrayRef).getName() + "[])";
                        } else {
                            break;
                        }
                    } else if (ref instanceof ArrayIndex) {
                        Node arrayRef = ((ArrayIndex) ref).getParameter(0);
                        Node indexRef = ((ArrayIndex) ref).getParameter(1);
                        if (arrayRef instanceof Variable) {
                            dataRef = ((Variable) arrayRef).getName() + "[";
                            if (indexRef instanceof Variable) {
                                dataRef += ((Variable) indexRef).getName();
                            } else if (indexRef instanceof SubtractionOperator) {
                                Node op1 = ((SubtractionOperator) indexRef).getOperand1();
                                dataRef += ((Variable) op1).getName() + "-1";
                            } else {
                                break;
                            }
                            dataRef += "]";
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    List<NumberLiteral> literalValues = getListOfValues((ArrayValues<NumberLiteral>) values);
                    if (literalValues.size() == 1) {
                        strings.add(dataRef + " == " + literalValues.get(0).getValue());
                        return strings;
                    } else if (literalValues.size() > 1) {
                        String start = dataRef + " one of { ";
                        String end = " }";
                        strings.addAll(generatePermutationStrings(start, literalValues, end));

                        return strings;
                    }
                }
                break;
            }
            case DaikonInvariantTypes.DAIKON_LOWER_BOUND: {
                if (term instanceof LowerBoundOperator) {
                    String ref = getNodeString(((LowerBoundOperator) term).getParameter(0));
                    String bound = ((LowerBoundOperator) term).getBound() + "";
                    strings.add(ref + " >= " + bound);
                    return strings;
                }
                break;
            }
        }

        return null;
    }

    public static String formatValue(Value<?> value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof ArrayValues<?>) {
            StringBuilder sb = new StringBuilder("[");
            int length = Array.getLength(value.getValue());
            for (int i = 0; i < length; i++) {
                Value<?> element = (Value<?>) Array.get(value.getValue(), i);
                sb.append(formatValue(element)); // Recursive call for nested arrays
                if (i < length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        // If it's not an array, call getValue() and return its string representation
        return value.getValue().toString();
    }

    public static <VAL extends Value<?>> List<VAL> getListOfValues(ArrayValues<VAL> values) {
        List<VAL> literalValues = new LinkedList<>();
        for (VAL val : values.getValue()) {
            if(val != null){
                literalValues.add(val);
            }
        }
        return literalValues;
    }

    public static <VAL extends Value<?>> List<String> generatePermutationStrings(String start, List<VAL> list, String end) {
        return generatePermutationStrings(start, "", list, "", end);
    }

    public static <VAL extends Value<?>> List<String> generatePermutationStrings(String start, String valPrefix, List<VAL> list, String valPostfix, String end) {
        List<String> strings = new LinkedList<>();
        List<String> permutationStrings = generateValuePermutationStrings(list, valPrefix, valPostfix);
        for (String permutationString : permutationStrings) {
            strings.add(start + permutationString + end);
        }

        return strings;
    }

    public static <VAL extends Value<?>> List<String> generateValuePermutationStrings(List<VAL> list, String valPrefix, String valPostfix) {
        List<String> permutationStrings = new LinkedList<>();
        List<List<VAL>> permutations = generatePermutations(list);
        for (List<VAL> permutation : permutations) {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (VAL literalValue : permutation) {
                if(!first){
                    result.append(", ");
                }
                first = false;
                result.append(valPrefix);
                result.append(formatValue(literalValue));
                result.append(valPostfix);
            }
            permutationStrings.add(result.toString());
        }

        return permutationStrings;
    }

    public static <VAL> List<List<VAL>> generatePermutations(List<VAL> list) {
        List<List<VAL>> result = new ArrayList<>();
        permute(list, 0, result);
        return result;
    }

    private static <VAL> void permute(List<VAL> list, int start, List<List<VAL>> result) {
        if (start == list.size() - 1) {
            result.add(new ArrayList<>(list));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            Collections.swap(list, start, i);
            permute(list, start + 1, result);
            Collections.swap(list, start, i); // backtrack
        }
    }

    private static String getNodeString(Node node){
        if(node instanceof Variable){
            return ((Variable) node).getName();
        } else if(node instanceof NumberLiteral){
            return ((NumberLiteral) node).getValue() + "";
        } else if(node instanceof ArrayLength){
            Node array = ((ArrayLength) node).getParameter(0);
            if(array instanceof Variable){
                return "size(" + ((Variable) array).getName() + "[])";
            }
        } else if(node instanceof SubtractionOperator){
            Node op1 = ((SubtractionOperator) node).getOperand1();
            Node op2 = ((SubtractionOperator) node).getOperand2();
            return getNodeString(op1) + "-" + getNodeString(op2);
        }

        return null;
    }
}
