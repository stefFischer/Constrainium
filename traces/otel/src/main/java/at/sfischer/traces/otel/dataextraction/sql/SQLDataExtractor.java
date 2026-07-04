package at.sfischer.traces.otel.dataextraction.sql;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanAttributeExtractor;
import at.sfischer.traces.otel.dataextraction.StorageOperation;
import at.sfischer.traces.otel.dataextraction.StorageSpanData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.sql.JDBCType;
import java.util.*;

public class SQLDataExtractor implements DataExtractor {

    private final SpanAttributeExtractor<String> sqlExtractor;
    private final SpanAttributeExtractor<String> argumentsExtractor;
    private final SpanAttributeExtractor<String> resultsExtractor;

    public SQLDataExtractor(SpanAttributeExtractor<String> sqlExtractor, SpanAttributeExtractor<String> argumentsExtractor, SpanAttributeExtractor<String> resultsExtractor) {
        this.sqlExtractor = sqlExtractor;
        this.argumentsExtractor = argumentsExtractor;
        this.resultsExtractor = resultsExtractor;
    }

    @Override
    public StorageSpanData extractData(TraceNode<?> span) {
        String sql = sqlExtractor.extract(span);
        String arguments = argumentsExtractor.extract(span);
        String results = resultsExtractor.extract(span);

        try {
            return parseSQLColumns(sql, arguments, results);
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Object> parseArguments(String arguments){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(arguments);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode parameters = root.get("parameters");
        List<Object> args = new ArrayList<>();
        for (JsonNode node : parameters) {
            if (node.isTextual()) {
                args.add(node.asText());
            } else if (node.isInt()) {
                args.add(node.asInt());
            } else if (node.isDouble()) {
                args.add(node.asDouble());
            } else if (node.isBoolean()) {
                args.add(node.asBoolean());
            } else {
                args.add(node.toString());
            }
        }

        return args;
    }

    public static List<JDBCType> parseParameterTypes(String parameterTypes){
        List<JDBCType> jdbcTypes = new LinkedList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            int[] types = mapper.readValue(parameterTypes, int[].class);
            for (int type : types) {
                JDBCType parameterType = JDBCType.valueOf(type);
                jdbcTypes.add(parameterType);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jdbcTypes;
    }

    private static List<Map<String, Object>> parseResults(String results) {
        if (results == null) return List.of();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(results);
            JsonNode rows = root.get("results");
            List<Map<String, Object>> parsed = new ArrayList<>();
            for (JsonNode row : rows) {
                Map<String, Object> values = new LinkedHashMap<>();
                row.get("values").fields().forEachRemaining(e -> {
                    JsonNode v = e.getValue();
                    if (v.isTextual())       values.put(e.getKey(), v.asText());
                    else if (v.isInt())      values.put(e.getKey(), v.asInt());
                    else if (v.isDouble())   values.put(e.getKey(), v.asDouble());
                    else if (v.isBoolean())  values.put(e.getKey(), v.asBoolean());
                    else                     values.put(e.getKey(), v.toString());
                });
                parsed.add(values);
            }
            return parsed;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static StorageSpanData parseSQLColumns(String sql, String arguments, String results) throws JSQLParserException {
        DataObject inputData = null;

        Statement stmt = CCJSqlParserUtil.parse(sql);

        StorageOperation operation = getOperation(stmt);
        Set<String> affectedData = new HashSet<>();
        Set<String> selectors = new HashSet<>();

        if(arguments != null) {
            List<Object> args = parseArguments(arguments);
            List<ParameterBinding> bindings = collectBindings(stmt, args);

            inputData = new DataObject();
            for (ParameterBinding b : bindings) {
                if(b.role() != ParameterRole.WHERE){
                    affectedData.add(b.column());
                } else {
                    selectors.add(b.column());
                }

                putNested(inputData, b.column(), b.value());
            }
        }

        DataObject outputData = null;
        if(results != null) {
            List<Map<String, Object>> rs = parseResults(results);
            List<Map<String, Object>> qualifiedResults = collectResults(stmt, rs);
            outputData = new DataObject();

            DataObject[] resultObjs = new DataObject[qualifiedResults.size()];
            int i = 0;
            for (Map<String, Object> qualifiedResult : qualifiedResults) {
                DataObject row = new DataObject();
                qualifiedResult.forEach((k, v) -> putNested(row, k, v));
                resultObjs[i++] = row;
            }

            outputData.putValue("results", resultObjs);
        }

        return new StorageSpanData(inputData, outputData, operation, affectedData, selectors);
    }

    private static void putNested(DataObject root, String fqColumn, Object value) {
        int dot = fqColumn.indexOf('.');
        if (dot == -1) {
            putValue(root, fqColumn, value);
            return;
        }

        String table = fqColumn.substring(0, dot);
        String column = fqColumn.substring(dot + 1);

        DataValue<?> existing = root.getDataValue(table);
        DataObject nested = (existing != null && existing.getValue() instanceof DataObject obj)
                ? obj
                : new DataObject();

        putValue(nested, column, value);
        root.putValue(table, nested);
    }

    private static void putValue(DataObject target, String name, Object value) {
        switch (value) {
            case String s       -> target.putValue(name, s);
            case Integer i      -> target.putValue(name, i);
            case Double d       -> target.putValue(name, (Number) d);
            case Boolean b      -> target.putValue(name, b);
            case null, default  -> throw new RuntimeException("Unsupported type: " + value);
        }
    }

    private static StorageOperation getOperation(Statement stmt){
        return switch (stmt) {
            case Update ignored -> StorageOperation.UPDATE;
            case Delete ignored -> StorageOperation.DELETE;
            case Insert ignored -> StorageOperation.WRITE;
            case Select ignored -> StorageOperation.READ;
            case null, default -> throw new RuntimeException("Unsupported Type.");
        };
    }

    private static List<ParameterBinding> collectBindings(Statement stmt, List<Object> args) {
        List<ParameterBinding> bindings = new ArrayList<>();
        // counter is 0-based internally, exposed as 1-based index
        int[] pos = {0};

        if (stmt instanceof Update update) {
            String tableName = update.getTable().getName();

            // SET clause: each assignment is col = expr
            for (UpdateSet updateSet : update.getUpdateSets()) {
                Column column = updateSet.getColumn(0);
                Expression expr = updateSet.getValue(0);

                if (expr instanceof JdbcParameter) {
                    String fq = tableName + "." + column.getColumnName();
                    bindings.add(new ParameterBinding(
                            ++pos[0], fq, ParameterRole.SET, args.get(pos[0] - 1)
                    ));
                }
            }

            // WHERE clause
            if (update.getWhere() != null) {
                collectWhereBindings(update.getWhere(), tableName, null, args, pos, bindings);
            }

        } else if (stmt instanceof Delete delete) {
            String tableName = delete.getTable().getName();

            if (delete.getWhere() != null) {
                collectWhereBindings(delete.getWhere(), tableName, null, args, pos, bindings);
            }

        } else if (stmt instanceof Insert insert) {
            String tableName = insert.getTable().getName();
            List<Column> columns = insert.getColumns();

            // VALUES (?, ?, ?) — walk expressions in parallel with column list
            if (insert.getValues() != null) {
                List<? extends Expression> expressions = insert.getValues().getExpressions();
                for (int i = 0; i < expressions.size(); i++) {
                    if (expressions.get(i) instanceof JdbcParameter) {
                        String fq = tableName + "." + columns.get(i).getColumnName();
                        bindings.add(new ParameterBinding(
                                ++pos[0], fq, ParameterRole.INSERT, args.get(pos[0] - 1)
                        ));
                    }
                }
            }

        } else if (stmt instanceof Select select) {
            PlainSelect body = (PlainSelect) select;
            Map<String, String> aliasToTable = buildAliasMap(body);

            if (body.getWhere() != null) {
                collectWhereBindings(body.getWhere(), null, aliasToTable, args, pos, bindings);
            }
        }

        return bindings;
    }

    private static void collectWhereBindings(
            Expression where,
            String fallbackTable,
            Map<String, String> aliasToTable,
            List<Object> args,
            int[] pos,
            List<ParameterBinding> bindings) {

        where.accept(new ExpressionVisitorAdapter<>() {
            @Override
            public <S> Void visit(EqualsTo expr, S context) {
                handleBinaryBinding(expr.getLeftExpression(), expr.getRightExpression());
                handleBinaryBinding(expr.getRightExpression(), expr.getLeftExpression());
                return null;
            }

            @Override
            public <S> Void visit(GreaterThan expr, S context) {
                handleBinaryBinding(expr.getLeftExpression(), expr.getRightExpression());
                handleBinaryBinding(expr.getRightExpression(), expr.getLeftExpression());
                return null;
            }

            private void handleBinaryBinding(Expression maybePlaceholder, Expression maybeColumn) {
                if (maybePlaceholder instanceof JdbcParameter && maybeColumn instanceof Column col) {
                    String fq;
                    if(aliasToTable == null){
                        String resolvedTable = col.getTable() != null && col.getTable().getName() != null
                                ? col.getTable().getName()
                                : fallbackTable;
                        fq = resolvedTable + "." + col.getColumnName();
                    } else {
                        fq = resolveColumn(col, aliasToTable);
                    }
                    bindings.add(new ParameterBinding(
                            ++pos[0], fq, ParameterRole.WHERE, args.get(pos[0] - 1)
                    ));
                }
            }
        }, null);
    }

    private static List<Map<String, Object>> collectResults(Statement stmt, List<Map<String, Object>> results) {
        if (stmt instanceof Select select) {
            PlainSelect body = (PlainSelect) select;
            Map<String, String> aliasToTable = buildAliasMap(body);

            Map<String, String> columnQualification = new LinkedHashMap<>();
            for (SelectItem<?> item : body.getSelectItems()) {
                if (item.getExpression() instanceof Column column) {
                    String fq = resolveColumn(column, aliasToTable);
                    columnQualification.put(column.getColumnName(), fq);
                }
            }

            List<Map<String, Object>> qualifiedResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Map<String, Object> qualifiedRow = new LinkedHashMap<>();
                row.forEach((col, val) -> {
                    String fq = columnQualification.getOrDefault(col, col);
                    qualifiedRow.put(fq, val);
                });
                qualifiedResults.add(qualifiedRow);
            }

            return qualifiedResults;
        }

        return null;
    }

    private static Map<String, String> buildAliasMap(PlainSelect body) {
        Map<String, String> aliasToTable = new HashMap<>();
        FromItem fromItem = body.getFromItem();
        if (fromItem instanceof Table table) {
            String realTable = table.getName();
            Alias alias = table.getAlias();

            if (alias != null) {
                aliasToTable.put(alias.getName(), realTable);
            } else {
                aliasToTable.put(realTable, realTable);
            }
        }

        if (body.getJoins() != null) {
            for (Join join : body.getJoins()) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof Table table) {
                    String realTable = table.getName();
                    Alias alias = table.getAlias();
                    if (alias != null) {
                        aliasToTable.put(alias.getName(), realTable);
                    } else {
                        aliasToTable.put(realTable, realTable);
                    }
                }
            }
        }
        return aliasToTable;
    }

    private static String resolveColumn(Column column, Map<String, String> aliasMap) {
        String tableName = null;
        if (column.getTable() != null) {
            String alias = column.getTable().getName();
            tableName = aliasMap.getOrDefault(alias, alias);
        }

        if (tableName == null || tableName.isBlank()) {
            tableName = "UNKNOWN";
        }

        return tableName + "." + column.getColumnName();
    }
}
