package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.evaluation.agora_data.testdata.TestCase;
import at.sfischer.constraints.evaluation.data.UnzipUtility;
import at.sfischer.constraints.evaluation.data.ZenodoDownloader;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.numbers.SubtractionOperator;
import at.sfischer.utils.table.ArrayValueTable;
import at.sfischer.utils.table.ValueTable;
import at.sfischer.utils.table.ValueTableRow;
import at.sfischer.utils.table.visitor.FilterValueTableRowVisitor;
import at.sfischer.utils.table.visitor.ValueTableRowVisitor;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReadAgoraDataTest {

	private static final String ZENODO_URL = "https://zenodo.org/records/7970822/files/ReplicationPackage.zip";

	private static File dataDirectory;

	private static File testCasesDirectory;
	private static File invariantDirectory;

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
	public void evaluateAmadeusHotel50OriginalDaikon()  throws IOException {
		String dataFilePath = "AmadeusHotel/AmadeusHotel_50.csv";
		String invariantFilePath = "AmadeusHotel/getMultiHotelOffers/50/invariants_50_original.csv";
		Map<String, List<Pair<Node, ConstraintPolicy>>> invariants = DaikonConfigurations.ORIGINAL_INVARIANTS;

		parseCallData(dataFilePath, invariantFilePath, invariants);
	}

	@Test
	public void evaluateAmadeusHotel50ModifiedDaikon()  throws IOException {
		String dataFilePath = "AmadeusHotel/AmadeusHotel_50.csv";
		String invariantFilePath = "AmadeusHotel/getMultiHotelOffers/50/invariants_50_modified.csv";
		Map<String, List<Pair<Node, ConstraintPolicy>>> invariants = DaikonConfigurations.MODIFIED_INVARIANTS;

		parseCallData(dataFilePath, invariantFilePath, invariants);
	}

	public void parseCallData(String dataFilePath, String invariantFilePath, Map<String, List<Pair<Node, ConstraintPolicy>>> invariants)  throws IOException {
		File dataFile = new File(testCasesDirectory, dataFilePath);
		File invariantFile = new File(invariantDirectory, invariantFilePath);

		parseCallData(dataFile, invariantFile, invariants);
	}

	public void parseCallData(File dataFile, File invariantFile, Map<String, List<Pair<Node, ConstraintPolicy>>> invariants)  throws IOException {
		List<TestCase> testCases = ReadAgoraData.parseCallData(dataFile);
		Triplet<SimpleDataCollection, SimpleDataCollection, InOutputDataCollection> data = ReadAgoraData.readInputOutputData(testCases);

		ValueTable invData = ValueTable.parseCSV(invariantFile, ";", true, false, false);
		invData.print();
		replaceReturnVariables(invData);

		invariants.forEach((invariantType, term) -> {
			evaluateInvariant(invData, data, invariantType, term);
        });
	}

	private ValueTable getInvariantTypeDate(ValueTable invData, final String invariantType){
		ValueTable invariantData =  invData.filter(new FilterValueTableRowVisitor() {
            @Override
            public boolean filter(ValueTableRow row) {
                if(!row.getValue("invariantType").equals(invariantType)){
                    return false;
                }

//                double tp = row.getValue("tp");
//                double fp = row.getValue("fp");
//                return tp != 0 || fp != 0;
				return true;
            }
        });

		invariantData.addValue("covered", 0, null);
		invariantData.addValue("results", 0, null);
		invariantData.addValue("values", 0, null);
		invariantData.removeColumn("pptname");
		invariantData.removeColumn("tp");
		invariantData.removeColumn("fp");
		invariantData.removeColumn("enter");
		invariantData.removeColumn("bug");

		return invariantData;
	}

	protected void evaluateInvariant(ValueTable invData, final Triplet<SimpleDataCollection, SimpleDataCollection, InOutputDataCollection> data, final String invariantType, final List<Pair<Node, ConstraintPolicy>> terms) {
		final ValueTable invariantData = getInvariantTypeDate(invData, invariantType);
		final ValueTable remainingConstraintsData = new ArrayValueTable(0,0);

		evaluateInvariant(invariantData, remainingConstraintsData, data.getValue0(), invariantType, terms);
		evaluateInvariant(invariantData, remainingConstraintsData, data.getValue1(), invariantType, terms);
		evaluateInvariant(invariantData, remainingConstraintsData, data.getValue2(), invariantType, terms);

		invariantData.print();
		remainingConstraintsData.removeRow(0);
		remainingConstraintsData.print();
	}

	protected void evaluateInvariant(ValueTable invariantData, ValueTable remainingConstraintsData, final DataCollection<?> data, final String invariantType, final List<Pair<Node, ConstraintPolicy>> terms) {
		for (Pair<Node, ConstraintPolicy> term : terms) {
			evaluateInvariant(invariantData, remainingConstraintsData, data, invariantType, term.getValue0(), term.getValue1());
		}
	}

	protected void evaluateInvariant(ValueTable invariantData, ValueTable remainingConstraintsData, final DataCollection<?> data, final String invariantType, final Node term, final ConstraintPolicy retentionPolicy) {
		DataSchema schema = data.deriveSchema(new DefaultArrayTypePromotionPolicy());
		assertNotNull(schema);

		System.out.println(schema);

		Set<Node> replacementTerms = new HashSet<>();
		replacementTerms.add(new ArrayLength(new Variable("$a")));
		replacementTerms.add(new SubtractionOperator(new ArrayLength(new Variable("$b")), new NumberLiteral(1)));

		schema.fillSchemaWithConstraints(term, replacementTerms);

		Map<DataSchemaEntry<DataSchema>, Set<Constraint>> constraints = new HashMap<>();
		Map<DataSchemaEntry<DataSchema>, Set<Constraint>> potentialConstraints = new HashMap<>();
		schema.collectAllConstraints(constraints, potentialConstraints);

		int totalConstraints = constraints.values().stream()
				.mapToInt(Set::size)
				.sum();
		int totalPotentialConstraints = potentialConstraints.values().stream()
				.mapToInt(Set::size)
				.sum();
		System.out.println("constraints: " + totalConstraints);
		System.out.println("potentialConstraints: " + totalPotentialConstraints);

		Map<DataSchemaEntry<?>, Set<ConstraintResults<?>>>  notInDaikonData = new HashMap<>();
		EvaluationResults<DataSchema, ?> evaluationResults = schema.evaluate(data);
		evaluationResults.getPotentialConstraintResults().forEach((schemaEntry, constraintResults) -> {
			for (ConstraintResults<?> results : constraintResults) {
				Constraint constraint = results.constraint();
				Node node = constraint.term();
				boolean includeConstraint = retentionPolicy.includeConstraint(results);
				Set<String> daikonStrings = DaikonInvariantToStringUtil.termToDaikonString(invariantType, node, schemaEntry);

				System.out.println("daikonStrings: " +  daikonStrings);
				if(daikonStrings == null){
					System.out.println("node: " +  node);
				}

				if(!includeConstraint){
					DataCollection<?> invalidData = results.invalidConstraintData();
					if(invalidData.size() > 0) {
						Set<Variable> targetVariables = results.constraint().term().findInvolvedVariables();
						List<Map<Variable, Node>> combinations = invalidData.getAllValueCombinations(targetVariables);
						System.out.println("invalidData: " + formatCombinations(combinations));
					}
				}

				if(daikonStrings != null){
					boolean found = checkInvariant(invariantData, daikonStrings, results, includeConstraint);
					if(!found){
						Set<ConstraintResults<?>> notConstraints = notInDaikonData.computeIfAbsent(schemaEntry, k -> new HashSet<>());
						notConstraints.add(results);
					}
				} else {
					Set<ConstraintResults<?>> notConstraints = notInDaikonData.computeIfAbsent(schemaEntry, k -> new HashSet<>());
					notConstraints.add(results);
				}
			}
		});

		invariantData.visitRows(new ValueTableRowVisitor() {
			@Override
			public void visit(ValueTableRow row) {
				Object covered = row.getValue("covered");
				boolean isCovered = covered != null ? (Boolean) covered : false;
				if(!isCovered){
					String[] variables = getVariables(row);
					if(variables == null){
						return;
					}
					Set<Variable> targetVariables = new HashSet<>();
					for (String variable : variables) {
						targetVariables.add(new Variable(variable));
					}

					List<Map<Variable, Node>> combinations = data.getAllValueCombinations(targetVariables);
					combinations.removeIf(m -> m.size() < targetVariables.size());
					invariantData.addValue("covered", row.getY(), false);
					invariantData.addValue("values", row.getY(), formatCombinations(combinations));
				}
			}
		});

		int startRow = remainingConstraintsData.rows();
		AtomicInteger row = new AtomicInteger(startRow);
		notInDaikonData.forEach((schemaEntry, constraintResults) -> {
			for (ConstraintResults<?> results : constraintResults) {
				boolean includeConstraint = retentionPolicy.includeConstraint(results);
				if(!includeConstraint){
					continue;
				}

				Set<String> daikonStrings = DaikonInvariantToStringUtil.termToDaikonString(invariantType, results.constraint().term(), schemaEntry);

				if(daikonStrings == null){
					remainingConstraintsData.addValue("daikonString", row.get(), "");
				} else {
					remainingConstraintsData.addValue("daikonString", row.get(), daikonStrings.iterator().next());
				}
				remainingConstraintsData.addValue("constraint", row.get(), results.constraint().toString());
				remainingConstraintsData.addValue("results", row.get(), results.toString());

				row.getAndIncrement();
			}
		});
	}

	public static String formatCombinations(List<Map<Variable, Node>> combinations) {
		return combinations.stream()
				.sorted(Comparator.comparingInt((Map<Variable, Node> m) -> m.size()).reversed())
				.map(map -> map.entrySet().stream()
						.map(entry -> entry.getKey().getName() + "=" + entry.getValue())
						.collect(Collectors.joining(", ", "{", "}")))
				.collect(Collectors.joining(" | ")).replaceAll("\n", "");
	}

	public static boolean checkInvariant(ValueTable invariantData, Set<String> daikonStrings, ConstraintResults<?> results, boolean retentionResult){
		final boolean[] isContained = {false};
		invariantData.visitRows(new ValueTableRowVisitor() {
			@Override
			public void visit(ValueTableRow row) {
				String invariant = row.getValue("invariant");
				if(daikonStrings.contains(invariant)){
					isContained[0] = true;
					invariantData.addValue("covered", row.getY(), retentionResult);
					invariantData.addValue("results", row.getY(), results.toString());
				}
			}
		});

		return isContained[0];
	}

	public static String[] getVariables(ValueTableRow row){
		String variableString = row.getValue("variables");
		if(variableString == null){
			return null;
		}

		// Remove all size(...) wrappers by extracting the inside of each
		variableString = variableString.replaceAll("size\\s*\\(([^)]+)\\)", "$1");

		List<String> extractedVariables = new ArrayList<>();
		// Match patterns like: foo[bar] or foo[bar - 1]
		Matcher indexMatcher = Pattern.compile("([\\w\\.]+)\\[([^\\]]+)\\]").matcher(variableString);
		while (indexMatcher.find()) {
			extractedVariables.add(indexMatcher.group(1)); // the outer expression
			// Extract variable names from the inside (can be complex like bar - 1)
			String indexExpr = indexMatcher.group(2);
			Matcher innerVarMatcher = Pattern.compile("[a-zA-Z_][\\w\\.]*").matcher(indexExpr);
			while (innerVarMatcher.find()) {
				extractedVariables.add(innerVarMatcher.group());
			}
		}

		// Remove '[..]' array notation
		variableString = variableString.replaceAll("\\[\\.\\.]", "");
		// Remove everything inside brackets
		variableString = variableString.replaceAll("\\[[^\\]]+\\]", "");

		// Remove parentheses
		variableString = variableString.substring(1, variableString.length() - 1);

		// Split by ", " while keeping the dot-separated structure intact
		String[] splitVars = variableString.split("\\s*,\\s*");
		for (String var : splitVars) {
			if (!var.isBlank()) extractedVariables.add(var);
		}

		return extractedVariables.stream().distinct().toArray(String[]::new);
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
				if(outRef == null){
					outRef = "";
				} else {
					outRef += ".";
				}
				String invariant = row.getValue("invariant");
				String replacesInvariant = invariant.replaceAll("return.", "return." + outRef);
				invariantData.addValue("invariant", rowIndex, replacesInvariant);

				String variables = row.getValue("variables");
				String replacesVariables = variables.replaceAll("return.", "return." + outRef);
				invariantData.addValue("variables", rowIndex, replacesVariables);
			}
		});
	}
}
