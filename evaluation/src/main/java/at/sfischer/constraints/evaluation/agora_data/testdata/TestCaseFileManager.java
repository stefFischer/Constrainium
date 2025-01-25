package at.sfischer.constraints.evaluation.agora_data.testdata;

import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseFileManager {

    private final Integer testCaseIdIndex;
    private final Integer operationIdIndex;
    private final Integer pathIndex;
    private final Integer httpMethodIndex;
    private final Integer headerParametersIndex;
    private final Integer pathParametersIndex;
    private final Integer queryParametersIndex;
    private final Integer formParametersIndex;
    private final Integer bodyParameterIndex;
    private final Integer statusCodeIndex;
    private final Integer responseBodyIndex;

    public TestCaseFileManager(String header) throws IOException {

        CSVRecord headers = CSVManager.getCSVRecord(header);

        this.testCaseIdIndex = getIndexOfElementInHeaders(headers, "testCaseId");
        this.operationIdIndex = getIndexOfElementInHeaders(headers,"operationId");
        this.pathIndex = getIndexOfElementInHeaders(headers,"path");
        this.httpMethodIndex = getIndexOfElementInHeaders(headers,"httpMethod");
        this.headerParametersIndex = getIndexOfElementInHeaders(headers,"headerParameters");
        this.pathParametersIndex = getIndexOfElementInHeaders(headers, "pathParameters");
        this.queryParametersIndex = getIndexOfElementInHeaders(headers, "queryParameters");
        this.formParametersIndex = getIndexOfElementInHeaders(headers, "formParameters");
        this.bodyParameterIndex = getIndexOfElementInHeaders(headers, "bodyParameter");
        this.statusCodeIndex = getIndexOfElementInHeaders(headers, "statusCode");
        this.responseBodyIndex = getIndexOfElementInHeaders(headers,"responseBody");

    }

    public TestCase getTestCase(CSVRecord row) {

        Map<String, String> headerParameters = stringToMap(row.get(this.headerParametersIndex));
        Map<String, String> pathParameters = stringToMap(row.get(this.pathParametersIndex));
        Map<String, String> queryParameters = queryToMap(row.get(this.queryParametersIndex));
        Map<String, String> formParameters = stringToMap(row.get(this.formParametersIndex));

        return new TestCase(row.get(this.testCaseIdIndex), row.get(this.operationIdIndex), row.get(this.pathIndex),
                row.get(this.httpMethodIndex), headerParameters, pathParameters, queryParameters,
                formParameters, row.get(this.bodyParameterIndex),
                row.get(this.statusCodeIndex), row.get(this.responseBodyIndex));

    }

    private static int getIndexOfElementInHeaders(CSVRecord headers, String header){
        for(int i = 0; i < headers.size(); i++) {
            if(headers.get(i).equalsIgnoreCase(header)) {
                return i;
            }
        }
        throw new NullPointerException("Element " + header + " not found in the csv headers");
    }

    private static Map<String, String> stringToMap(String str) {
        if(str.trim().isEmpty()){
            return new HashMap<>();
        }else {
            Map<String, String> res = Arrays.asList(str.split("\\s*;\\s*")).stream().map(s -> s.split("="))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1]));
            // Remove all new line chars (\n and \r). The parameter value line of the dTrace must be specified in a single line
            res.replaceAll((k,v) -> removeNewLineChars(v));
            return res;
        }
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, List<String>> paramMap = parseQuery(query);
        Map<String, String> stringMap = new HashMap<>();
        paramMap.forEach((k,v) -> {
            stringMap.put(k, v.size() == 1 ? v.get(0) : v.toString());
        });

        return stringMap;
    }

    public static Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> paramMap = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return paramMap;

        for (String pair : query.split("\\s*;\\s*")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String decodedValue = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                // Split by comma if it's a list
                String[] values = decodedValue.split(",");
                paramMap.put(key, List.of(values));
            }
        }

        return paramMap;
    }

    public static String removeNewLineChars(String str) {
        String res = str.replace("\n","\\n");
        res = res.replace("\r","\\r");
        return res;
    }

}
