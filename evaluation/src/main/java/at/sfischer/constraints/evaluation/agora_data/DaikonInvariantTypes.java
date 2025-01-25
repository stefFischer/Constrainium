package at.sfischer.constraints.evaluation.agora_data;

public class DaikonInvariantTypes {

    public static final String DAIKON_ONE_OF_STRING = "daikon.inv.unary.string.OneOfString";
    public static final String DAIKON_ONE_OF_STRING_SEQUENCE = "daikon.inv.unary.stringsequence.OneOfStringSequence";
    public static final String DAIKON_ONE_OF_STRING_IN_SEQUENCE = "daikon.inv.unary.stringsequence.EltOneOfString";

    public static final String DAIKON_STRING_IN_SEQUENCE = "daikon.inv.binary.sequenceString.MemberString";

    public static final String DAIKON_STRING_EQUAL = "daikon.inv.binary.twoString.StringEqual";

    public static final String DAIKON_STRING_IS_DATE = "daikon.inv.unary.string.dates.IsDateYYYYMMDD";
    public static final String DAIKON_STRING_IS_NUMBER = "daikon.inv.unary.string.IsNumeric";
    public static final String DAIKON_SUB_STRING = "daikon.inv.binary.twoString.StdString$SubString";
    public static final String DAIKON_STRING_FIXED_LENGTH = "daikon.inv.unary.string.FixedLengthString";
    public static final String DAIKON_STRING_SEQUENCE_FIXED_LENGTH_ELEMENTS = "daikon.inv.unary.stringsequence.SequenceFixedLengthString";

    public static final String DAIKON_ONE_OF_SCALAR = "daikon.inv.unary.scalar.OneOfScalar";
    public static final String DAIKON_LOWER_BOUND = "daikon.inv.unary.scalar.LowerBound";

    public static final String DAIKON_INT_NOT_EQUAL = "daikon.inv.binary.twoScalar.IntNonEqual";
    public static final String DAIKON_INT_EQUAL = "daikon.inv.binary.twoScalar.IntEqual";
    public static final String DAIKON_INT_GREATER_EQUAL = "daikon.inv.binary.twoScalar.IntGreaterEqual";
    public static final String DAIKON_INT_GREATER = "daikon.inv.binary.twoScalar.IntGreaterThan";
    public static final String DAIKON_INT_LESS_EQUAL = "daikon.inv.binary.twoScalar.IntLessEqual";
    public static final String DAIKON_INT_LESS = "daikon.inv.binary.twoScalar.IntLessThan";

    public static final String DAIKON_INT_IN_SEQUENCE = "daikon.inv.binary.sequenceScalar.Member";

    public static final String DAIKON_ONE_OF_NUMBER_IN_SEQUENCE = "daikon.inv.unary.sequence.EltOneOf";


    /// daikon.inv.unary.sequence.EltNonZero
    /// daikon.inv.unary.scalar.NonZero
}
