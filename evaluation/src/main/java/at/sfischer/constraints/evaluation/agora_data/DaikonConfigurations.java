package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.miner.AndConstraintPolicy;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.miner.MinApplicationsPolicy;
import at.sfischer.constraints.miner.NoViolationsPolicy;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayIndex;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.numbers.*;
import at.sfischer.constraints.model.operators.strings.IsNumeric;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaikonConfigurations {

    public static final ConstraintPolicy ONE_FOUND_NO_VIOLATIONS = new AndConstraintPolicy(new NoViolationsPolicy(), new MinApplicationsPolicy(1));
    public static final ConstraintPolicy THREE_FOUND_NO_VIOLATIONS = new AndConstraintPolicy(new NoViolationsPolicy(), new MinApplicationsPolicy(3));
    public static final ConstraintPolicy FOUR_FOUND_NO_VIOLATIONS = new AndConstraintPolicy(new NoViolationsPolicy(), new MinApplicationsPolicy(4));
    public static final ConstraintPolicy FIVE_FOUND_NO_VIOLATIONS = new AndConstraintPolicy(new NoViolationsPolicy(), new MinApplicationsPolicy(5));

    public final static Map<String, List<Pair<Node, ConstraintPolicy>>> ORIGINAL_INVARIANTS = new HashMap<>();

    public final static Map<String, List<Pair<Node, ConstraintPolicy>>> MODIFIED_INVARIANTS = new HashMap<>();

    static{
        /// ORIGINAL DAIKON INVARIANTS

        // AndJoiner

        // EltLowerBound
        // EltLowerBoundFloat
//        ORIGINAL_INVARIANTS.put("EltLowerBound", List.of(
//				new Pair<>(new ForAll(new Variable("a"), new LowerBoundOperator(new Variable(ArrayQuantifier.ELEMENT_NAME))), ONE_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.unary.sequence.EltNonZero
        // EltNonZeroFloat
        // TODO

        // EltOneOf
        // EltOneOfFloat
//        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_ONE_OF_NUMBER_IN_SEQUENCE, List.of(
//				new Pair<>(new ForAll(new Variable("a"), new OneOfNumber(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(3))), ONE_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.unary.stringsequence.EltOneOfString
//        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_ONE_OF_STRING_IN_SEQUENCE, List.of(
//				new Pair<>(new ForAll(new Variable("a"), new OneOfString(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(3))), ONE_FOUND_NO_VIOLATIONS)
//		));

        // EltUpperBound
        // EltUpperBoundFloat
        // TODO Need to implement UpperBoundOperator

        /*
         EltwiseFloatEqual
            Represents equality between adjacent elements (x[i], x[i+1]) of a double sequence. Prints as x[] elements are equal.
        EltwiseFloatGreaterEqual
            Represents the invariant >= between adjacent elements (x[i], x[i+1]) of a double sequence. Prints as x[] sorted by >=.
        EltwiseFloatGreaterThan
            Represents the invariant > between adjacent elements (x[i], x[i+1]) of a double sequence. Prints as x[] sorted by >.
        EltwiseFloatLessEqual
            Represents the invariant <= between adjacent elements (x[i], x[i+1]) of a double sequence. Prints as x[] sorted by <=.
        EltwiseFloatLessThan
            Represents the invariant < between adjacent elements (x[i], x[i+1]) of a double sequence. Prints as x[] sorted by <.
        EltwiseIntEqual
            Represents equality between adjacent elements (x[i], x[i+1]) of a long sequence. Prints as x[] elements are equal.
        EltwiseIntGreaterEqual
            Represents the invariant >= between adjacent elements (x[i], x[i+1]) of a long sequence. Prints as x[] sorted by >=.
        EltwiseIntGreaterThan
            Represents the invariant > between adjacent elements (x[i], x[i+1]) of a long sequence. Prints as x[] sorted by >.
        EltwiseIntLessEqual
            Represents the invariant <= between adjacent elements (x[i], x[i+1]) of a long sequence. Prints as x[] sorted by <=.
        EltwiseIntLessThan
            Represents the invariant < between adjacent elements (x[i], x[i+1]) of a long sequence. Prints as x[] sorted by <.
        */

        /*
        Equality
            Keeps track of sets of variables that are equal. Other invariants are instantiated for only one member of the Equality set, the leader. If variables x, y, and z are members of the Equality set and x is chosen as the leader, then the Equality will internally convert into binary comparison invariants that print as x == y and x == z.
         */

        // daikon.inv.binary.twoScalar.IntEqual
        // daikon.inv.binary.twoScalar.FloatEqual
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_EQUAL, List.of(
//				new Pair<>(new EqualOperator(new Variable("a"), new Variable("b")), ONE_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.binary.twoScalar.IntGreaterEqual
        // daikon.inv.binary.twoScalar.FloatGreaterEqual
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_GREATER_EQUAL, List.of(
//				new Pair<>(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")), FOUR_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.binary.twoScalar.IntGreaterThan
        // daikon.inv.binary.twoScalar.FloatGreaterThan
//        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_GREATER, List.of(
//                new Pair<>(new GreaterThanOperator(new Variable("a"), new Variable("b")), FOUR_FOUND_NO_VIOLATIONS)
//        ));

        // daikon.inv.binary.twoScalar.IntLessEqual
        // daikon.inv.binary.twoScalar.FloatLessEqual
//        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_LESS_EQUAL, List.of(
//                new Pair<>(new LessThanOrEqualOperator(new Variable("a"), new Variable("b")), FOUR_FOUND_NO_VIOLATIONS)
//        ));

        // daikon.inv.binary.twoScalar.IntLessThan
        // daikon.inv.binary.twoScalar.FloatLessThan
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_LESS, List.of(
//				new Pair<>(new LessThanOperator(new Variable("a"), new Variable("b")), FOUR_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.binary.twoScalar.IntNonEqual
        // daikon.inv.binary.twoScalar.FloatNonEqual
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_NOT_EQUAL, List.of(
//				new Pair<>(new NotOperator(new EqualOperator(new Variable("a"), new Variable("b"))), FOUR_FOUND_NO_VIOLATIONS)
//		));

        /*
        FunctionBinary.BitwiseAndLong_{xyz, yxz, zxy}
            Represents the invariant x = BitwiseAnd(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.BitwiseOrLong_{xyz, yxz, zxy}
            Represents the invariant x = BitwiseOr(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.BitwiseXorLong_{xyz, yxz, zxy}
            Represents the invariant x = BitwiseXor(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.DivisionLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = Division(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinary.GcdLong_{xyz, yxz, zxy}
            Represents the invariant x = Gcd(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.LogicalAndLong_{xyz, yxz, zxy}
            Represents the invariant x = LogicalAnd(y, z) over three long scalars. For logical operations, Daikon treats 0 as false and all other values as true. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.LogicalOrLong_{xyz, yxz, zxy}
            Represents the invariant x = LogicalOr(y, z) over three long scalars. For logical operations, Daikon treats 0 as false and all other values as true. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.LogicalXorLong_{xyz, yxz, zxy}
            Represents the invariant x = LogicalXor(y, z) over three long scalars. For logical operations, Daikon treats 0 as false and all other values as true. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.LshiftLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = Lshift(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinary.MaximumLong_{xyz, yxz, zxy}
            Represents the invariant x = Maximum(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.MinimumLong_{xyz, yxz, zxy}
            Represents the invariant x = Minimum(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.ModLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = Mod(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinary.MultiplyLong_{xyz, yxz, zxy}
            Represents the invariant x = Multiply(y, z) over three long scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinary.PowerLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = Power(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinary.RshiftSignedLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = RshiftSigned(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinary.RshiftUnsignedLong_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = RshiftUnsigned(y, z) over three long scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinaryFloat.DivisionDouble_{xyz, xzy, yxz, yzx, zxy, zyx}
            Represents the invariant x = Division(y, z) over three double scalars. Since the function is non-symmetric, all six permutations of the variables are checked.
        FunctionBinaryFloat.MaximumDouble_{xyz, yxz, zxy}
            Represents the invariant x = Maximum(y, z) over three double scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinaryFloat.MinimumDouble_{xyz, yxz, zxy}
            Represents the invariant x = Minimum(y, z) over three double scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
        FunctionBinaryFloat.MultiplyDouble_{xyz, yxz, zxy}
            Represents the invariant x = Multiply(y, z) over three double scalars. Since the function is symmetric, only the permutations xyz, yxz, and zxy are checked.
         */

        /*
        Implication
            The Implication invariant class is used internally within Daikon to handle invariants that are only true when certain other conditions are also true (splitting).
         */

        /*
        LinearBinary
            Represents a Linear invariant between two long scalars x and y, of the form ax + by + c = 0. The constants a, b and c are mutually relatively prime, and the constant a is always positive.
        LinearBinaryFloat
            Represents a Linear invariant between two double scalars x and y, of the form ax + by + c = 0. The constants a, b and c are mutually relatively prime, and the constant a is always positive.
        LinearTernary
            Represents a Linear invariant over three long scalars x, y, and z, of the form ax + by + cz + d = 0. The constants a, b, c, and d are mutually relatively prime, and the constant a is always positive.
        LinearTernaryFloat
            Represents a Linear invariant over three double scalars x, y, and z, of the form ax + by + cz + d = 0. The constants a, b, c, and d are mutually relatively prime, and the constant a is always positive.
         */

        // daikon.inv.unary.scalar.LowerBound
        // daikon.inv.unary.scalar.LowerBoundFloat
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_LOWER_BOUND, List.of(
//				new Pair<>(new LowerBoundOperator(new Variable("a")), ONE_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.binary.sequenceScalar.Member
        // daikon.inv.binary.sequenceScalar.MemberFloat
//        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_INT_IN_SEQUENCE, List.of(
//                new Pair<>(new Exists(new Variable("a"), new EqualOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new Variable("b"))), ONE_FOUND_NO_VIOLATIONS)
//        ));

        // daikon.inv.binary.sequenceString.MemberString
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_IN_SEQUENCE, List.of(
//				new Pair<>(new Exists(new Variable("a"), new StringEquals(new Variable(ArrayQuantifier.ELEMENT_NAME), new Variable("b"))), ONE_FOUND_NO_VIOLATIONS)
//		));

        // daikon.inv.unary.scalar.OneOfFloat
        // daikon.inv.unary.scalar.OneOfScalar
        ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_ONE_OF_SCALAR, List.of(
                new Pair<>(new OneOfNumber(new Variable("a"), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS),
                new Pair<>(new OneOfNumber(new ArrayLength(new Variable("a")), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS)
//                new Pair<>(new OneOfNumber(new ArrayIndex(new Variable("a"), new Variable("i")), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS),
//                new Pair<>(new OneOfNumber(new ArrayIndex(new Variable("a"), new SubtractionOperator(new Variable("i"), new NumberLiteral(1))), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS)
        ));

        // OneOfFloatSequence
        // OneOfSequence



//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_ONE_OF_STRING, List.of(
//				new Pair<>(new OneOfString(new Variable("a"), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS)
//		));
//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_ONE_OF_STRING_SEQUENCE, List.of(
//				new Pair<>(new OneOfStringArray(new Variable("a"), new NumberLiteral(3)), ONE_FOUND_NO_VIOLATIONS)
//		));



//		ORIGINAL_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_EQUAL, List.of(
//				new Pair<>(new StringEquals(new Variable("a"), new Variable("b")), ONE_FOUND_NO_VIOLATIONS)
//		));















        /// MODIFIED DAIKON INVARIANTS

//		MODIFIED_INVARIANTS.put(DaikonInvariantTypes.DAIKON_SUB_STRING, List.of(
//				new Pair<>(new SubString(new Variable("a"), new Variable("b")), ONE_FOUND_NO_VIOLATIONS)
//		));

//		MODIFIED_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_SEQUENCE_FIXED_LENGTH_ELEMENTS, List.of(
//				new Pair<>(new ForAll(new Variable("a"), new OneOfNumber(new StringLength(new Variable(ArrayQuantifier.ELEMENT_NAME)), new NumberLiteral(1))), ONE_FOUND_NO_VIOLATIONS)
//		));

//		MODIFIED_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_IS_DATE, List.of(
//				new Pair<>(new IsDate(new Variable("a")), ONE_FOUND_NO_VIOLATIONS)
//		));

//        MODIFIED_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_FIXED_LENGTH, List.of(
//                new Pair<>(new OneOfNumber(new StringLength(new Variable("a")), new NumberLiteral(1)), THREE_FOUND_NO_VIOLATIONS)
//        ));



        MODIFIED_INVARIANTS.put(DaikonInvariantTypes.DAIKON_STRING_IS_NUMBER, List.of(
                new Pair<>(new IsNumeric(new Variable("a")), FIVE_FOUND_NO_VIOLATIONS)
        ));
    }
}
