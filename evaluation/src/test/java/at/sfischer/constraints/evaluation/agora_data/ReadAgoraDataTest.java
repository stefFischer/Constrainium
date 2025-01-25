package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.evaluation.agora_data.testdata.TestCase;
import java.io.File;
import java.util.*;

import at.sfischer.constraints.miner.ConstraintMiner;
import at.sfischer.constraints.miner.ConstraintMinerFromData;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.VariableVisitor;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import org.javatuples.Pair;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReadAgoraDataTest {
	@Test
	public void parseCallData() {
		String dataFilePath = "C:\\Users\\sfischer\\Downloads\\ReplicationPackage\\datasets\\testCasesExperiment1\\AmadeusHotel\\AmadeusHotel_50.csv";
		File dataFile = new File(dataFilePath);
		List<TestCase> expected = new ArrayList<>();
		List<TestCase> actual = ReadAgoraData.parseCallData(dataFile);

		Pair<SimpleDataCollection, InOutputDataCollection> data = ReadAgoraData.readInputOutputData(actual);

		SimpleDataSchema  outputSchema = null;
		for (DataObject dataObject : data.getValue0().getDataCollection()) {
			SimpleDataSchema schema = SimpleDataSchema.deriveFromData(dataObject);
			if(outputSchema == null){
				outputSchema = schema;
			} else {
				outputSchema.unify(schema);
			}
		}

		System.out.println(outputSchema);
		assertTrue(false);

		System.out.println("Number of data entries: " + data.getValue1().numberOfDataEntries());

		ConstraintMiner miner = new ConstraintMinerFromData(data.getValue0());

		// Define the pool of base terms.
		Set<Node> terms = new HashSet<>();

		terms.add(new GreaterThanOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> constraints = miner.getPossibleConstraints(terms);

		System.out.println("Number of constraints: " + constraints.size());

		for (Constraint constraint : constraints) {
			ConstraintResults results = constraint.applyData(data.getValue0());
			if(results.foundCounterExample()){
				continue;
			}

			System.out.println("-----------------");
			System.out.println(constraint.term());

			System.out.println(results);
			System.out.println(results.numberOfValidDataEntries());
			System.out.println(results.numberOfViolations());
			System.out.println(results.applicationRate());


//			if(results.numberOfInapplicableEntries() == data.getValue1().numberOfDataEntries()){
//				Set<String> fieldNames = new HashSet<>();
//				constraint.term().visitNodes((VariableVisitor) variable -> fieldNames.add(variable.getName()));
//
//				System.out.println("fieldNames: " + fieldNames);
//
//				results.inapplicableConstraintData().visitDataEntries(new HashSet<>(), (values, dataEntry) -> {
//					System.out.println(values);
////					System.out.println(dataEntry);
//                });
//			}
		}

//		assertEquals(expected, actual);
	}
}
