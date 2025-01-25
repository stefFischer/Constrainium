package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.evaluation.data.UnzipUtility;
import at.sfischer.constraints.evaluation.data.ZenodoDownloader;
import at.sfischer.constraints.evaluation.agora_data.testdata.TestCase;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.sfischer.constraints.miner.*;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.strings.OneOfString;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.utils.table.ValueTable;
import at.sfischer.utils.table.ValueTableRow;
import at.sfischer.utils.table.visitor.FilterValueTableRowVisitor;
import at.sfischer.utils.table.visitor.ValueTableRowVisitor;
import org.javatuples.Pair;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReadAgoraDataTest {

	private static final String ZENODO_URL = "https://zenodo.org/records/7970822/files/ReplicationPackage.zip";

	private static File dataDirectory;

	private static File testCasesDirectory;
	private static File invariantDirectory;

	private static final String DAIKON_ONE_OF_STRING = "daikon.inv.unary.string.OneOfString";

	private static final String DAIKON_STRING_EQUAL = "daikon.inv.binary.twoString.StringEqual";

	private final static Map<String, Node> ORIGINAL_INVARIANTS = new HashMap<>();

	static{
		ORIGINAL_INVARIANTS.put(DAIKON_ONE_OF_STRING, new OneOfString(new Variable("a"), new NumberLiteral(3)));
//		ORIGINAL_INVARIANTS.put(DAIKON_STRING_EQUAL, new StringEquals(new Variable("a"), new Variable("b")));
	}

	@BeforeAll
	public static void initData(){
		File targetDir = new File("data/downloads");
		targetDir.mkdirs();

		dataDirectory = new File(targetDir, "agoraData");
		testCasesDirectory = new File(dataDirectory, "ReplicationPackage/datasets/testCasesExperiment1");
		invariantDirectory = new File(dataDirectory, "ReplicationPackage/reports/invariantsClassified");
		if(dataDirectory.exists()){
			return;
		}

		// Download the evaluation data from the Zenodo repository. This will take a while for the first time running this evaluation.
		File destFile = ZenodoDownloader.downloadFile(ZENODO_URL, targetDir);
		UnzipUtility.unzip(destFile, dataDirectory);
		destFile.delete();
    }

	@Test
	public void parseCallData() {
		String dataFilePath = "AmadeusHotel/AmadeusHotel_50.csv";
		File dataFile = new File(testCasesDirectory, dataFilePath);

		List<TestCase> testCases = ReadAgoraData.parseCallData(dataFile);
		Pair<SimpleDataCollection, InOutputDataCollection> data = ReadAgoraData.readInputOutputData(testCases);

		String invariantFilePath = "AmadeusHotel/getMultiHotelOffers/50/invariants_50_original.csv";
		File invariantFile = new File(invariantDirectory, invariantFilePath);

		ORIGINAL_INVARIANTS.forEach((invariantType, term) -> {
            try {
                evaluateInvariant(data, invariantFile, invariantType, term);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

//		terms.add(new GreaterThanOperator(new Variable("a"), new Variable("b")));
//		terms.add(new StringEquals(new Variable("a"), new Variable("b")));




	}

	protected void evaluateInvariant(final Pair<SimpleDataCollection, InOutputDataCollection> data, final File invariantFile, final String invariantType, final Node term) throws IOException {
		ValueTable invData = ValueTable.parseCSV(invariantFile, ";", true, false, false);

		invData.print();

		replaceReturnVariables(invData);

		final ValueTable invariantData = invData.filter(new FilterValueTableRowVisitor() {
			@Override
			public boolean filter(ValueTableRow row) {
				if(!row.getValue("invariantType").equals(invariantType)){
					return false;
				}

				double tp = row.getValue("tp");
				double fp = row.getValue("fp");
				return tp != 0 || fp != 0;
			}
		});

		invariantData.addValue("covered", 0, false);
		invariantData.removeColumn("pptname");
		invariantData.removeColumn("tp");
		invariantData.removeColumn("fp");
		invariantData.removeColumn("enter");
		invariantData.removeColumn("bug");

		invariantData.print();

		SimpleDataSchema outputSchema = null;
		for (DataObject dataObject : data.getValue0().getDataCollection()) {
			SimpleDataSchema schema = SimpleDataSchema.deriveFromData(dataObject);
			if(outputSchema == null){
				outputSchema = schema;
			} else {
				outputSchema.unify(schema);
			}
		}
		assertNotNull(outputSchema);

		outputSchema.fillSchemaWithConstraints(term);

		System.out.println("Number of data entries: " + data.getValue1().numberOfDataEntries());


		EvaluationResults<DataSchema, DataObject> evaluationResults = outputSchema.evaluate(data.getValue0());

		evaluationResults.getPotentialConstraintResults().forEach((k, v) -> {
			for (ConstraintResults<DataObject> results : v) {
				if(results.validConstraintData().size() > 0 && results.invalidConstraintData().size() == 0) {
					System.out.println(k.getQualifiedName());
					System.out.println(results);
				}
			}
		});

		outputSchema.applyConstraintRetentionPolicy(evaluationResults, new NoViolationsPolicy(), new MinApplicationsPolicy(5));

		System.out.println("Schema with remaining constraints:");
		System.out.println(outputSchema);


		Map<DataSchemaEntry<SimpleDataSchema>, Set<Constraint>> constraints = new HashMap<>();
		outputSchema.collectAllConstraints(constraints, new HashMap<>());

		Map<DataSchemaEntry<SimpleDataSchema>, Set<Constraint>>  notInDaikonData = new HashMap<>();

		constraints.forEach((schemaEntry, constraintSet) -> {
			for (Constraint constraint : constraintSet) {
				Node node = constraint.term();
				Set<String> daikonStrings = termToDaikonString(invariantType, node, schemaEntry);

//				System.out.println(constraint);
//				System.out.println(daikonStrings);

				// TODO Add support for the ones still null.
				if(daikonStrings != null){
					boolean found = checkInvariant(invariantData, daikonStrings);
					if(!found){
						System.err.println(constraint);
						System.err.println(daikonStrings);

						Set<Constraint> notConstraints = notInDaikonData.computeIfAbsent(schemaEntry, k -> new HashSet<>());
						notConstraints.add(constraint);
					}
				}
			}
		});

		assertTrue(evaluationResults.getEvaluationResults().isEmpty());

		invariantData.print();

		assertTrue(false);
	}

	public static boolean checkInvariant(ValueTable invariantData, Set<String> daikonStrings){
		final boolean[] isContained = {false};
		invariantData.visitRows(new ValueTableRowVisitor() {
			@Override
			public void visit(ValueTableRow row) {
				String invariant = row.getValue("invariant");
				if(daikonStrings.contains(invariant)){
					isContained[0] = true;
					invariantData.addValue("covered", row.getY(), true);
				}
			}
		});

		return isContained[0];
	}

	public static Set<String> termToDaikonString(final String invariantType, Node term, DataSchemaEntry<?> schemaEntry){
		Set<String> strings = new HashSet<>();
		switch (invariantType){
			case DAIKON_ONE_OF_STRING:
				if(term instanceof OneOfString){
					@SuppressWarnings("unchecked")
					ArrayValues<StringLiteral> values = (ArrayValues<StringLiteral>)((OneOfString) term).getParameter(2);
					List<StringLiteral> literalValues = new LinkedList<>();
					for (StringLiteral stringLiteral : values.getValue()) {
						if(stringLiteral != null){
							literalValues.add(stringLiteral);
						}
					}

					if (literalValues.size() == 1){
						strings.add("\"" + schemaEntry.getQualifiedName() + " == \"\"" + literalValues.get(0).getValue() + "\"\"\"");
						return strings;
					} else if(literalValues.size() > 1){
						String start = "\"" + schemaEntry.getQualifiedName() + " one of {";
						List<List<StringLiteral>> permutations = generatePermutations(literalValues);
						for (List<StringLiteral> permutation : permutations) {
							StringBuilder result = new StringBuilder(start);
							boolean first = true;
							for (StringLiteral literalValue : permutation) {
								if(!first){
									result.append(",");
								}
								first = false;
								result.append(" \"\"").append(literalValue.getValue()).append("\"\"");
							}
							strings.add(result + " }\"");
						}

						return strings;
					}
				}
				break;
			case DAIKON_STRING_EQUAL:
				if(term instanceof StringEquals){
					DataReference ref1 = (DataReference)((StringEquals) term).getParameter(0);
					DataReference ref2 = (DataReference)((StringEquals) term).getParameter(1);
					strings.add(ref1.getDataSchemaEntry().getQualifiedName() + " == " + ref2.getDataSchemaEntry().getQualifiedName());
					strings.add(ref2.getDataSchemaEntry().getQualifiedName() + " == " + ref1.getDataSchemaEntry().getQualifiedName());
					return strings;
				}
				break;
		}

		return null;
	}

	public static List<List<StringLiteral>> generatePermutations(List<StringLiteral> list) {
		List<List<StringLiteral>> result = new ArrayList<>();
		permute(list, 0, result);
		return result;
	}

	private static void permute(List<StringLiteral> list, int start, List<List<StringLiteral>> result) {
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

	public static String extractOutputReference(String log) {
		Pattern pattern = Pattern.compile("\\d{3}(&([^()]+))?\\(");
		Matcher matcher = pattern.matcher(log);
		if (matcher.find() && matcher.group(2) != null) {
			return matcher.group(2).replace("&", ".");
		}
		return null;
	}

	public static void replaceReturnVariables(final ValueTable invariantData) {
		invariantData.visitRows(new ValueTableRowVisitor() {
			@Override
			public void visit(ValueTableRow row) {
				int rowIndex = row.getY();
				String outRef = extractOutputReference(row.getValue("pptname"));
				String invariant = row.getValue("invariant");
				String replacesInvariant = invariant.replaceAll("return.", outRef + ".");
				invariantData.addValue("invariant", rowIndex, replacesInvariant);

				String variables = row.getValue("variables");
				String replacesVariables = variables.replaceAll("return.", outRef + ".");
				invariantData.addValue("variables", rowIndex, replacesVariables);
			}
		});
	}
}
