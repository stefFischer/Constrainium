package at.sfischer.constraints.evaluation.agora_data;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.evaluation.agora_data.testdata.CSVManager;
import at.sfischer.constraints.evaluation.agora_data.testdata.TestCase;
import at.sfischer.constraints.evaluation.agora_data.testdata.TestCaseFileManager;
import org.javatuples.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ReadAgoraData {

    public static Pair<SimpleDataCollection, InOutputDataCollection> readInputOutputData(List<TestCase> testCases){
        SimpleDataCollection outputDataCollection = new SimpleDataCollection();
        InOutputDataCollection inOutputDataCollection = new InOutputDataCollection();

        for (TestCase testCase : testCases) {
            DataObject inputData;
            try {
                inputData = DataObject.parseData(testCase.getBodyParameter());
            } catch (org.json.JSONException e){
                inputData = new DataObject();
            }
            addInputData(inputData, testCase.getHeaderParameters());
            addInputData(inputData, testCase.getPathParameters());
            addInputData(inputData, testCase.getQueryParameters());
            addInputData(inputData, testCase.getFormParameters());

            DataObject outputData = DataObject.parseData(testCase.getResponseBody());

            outputDataCollection.addDataEntry(outputData);
            inOutputDataCollection.addDataEntry(new Pair<>(inputData, outputData));
        }

        return new Pair<>(outputDataCollection, inOutputDataCollection);
    }

    private static void addInputData(DataObject inputData, Map<String, String> parameters){
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if(isInteger(entry.getValue())){
                inputData.putValue(entry.getKey(), Integer.parseInt(entry.getValue()));
            } else if(isLong(entry.getValue())){
                inputData.putValue(entry.getKey(), Long.parseLong(entry.getValue()));
            } else if(isDouble(entry.getValue())){
                inputData.putValue(entry.getKey(), Double.parseDouble(entry.getValue()));
            } else if(entry.getValue().equals("true") || entry.getValue().equals("false")){
                inputData.putValue(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
            } else {
                inputData.putValue(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean isLong(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Long.parseLong(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean isDouble(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static List<TestCase> parseCallData(File dataFile){
        try {
            List<TestCase> testCases = new LinkedList<>();
            BufferedReader in = new BufferedReader(new FileReader(dataFile));
            String line = in.readLine();
            TestCaseFileManager testCaseFileManager = new TestCaseFileManager(line);
            while((line = in.readLine()) != null){
                TestCase testCase = testCaseFileManager.getTestCase(CSVManager.getCSVRecord(line));
                testCases.add(testCase);
            }

            return testCases;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
