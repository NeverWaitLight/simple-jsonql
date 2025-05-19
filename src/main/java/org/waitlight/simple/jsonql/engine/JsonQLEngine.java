package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.config.DBConfig;
import org.waitlight.simple.jsonql.engine.result.ExecuteResult;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

@Slf4j
public class JsonQLEngine {

    private final MetadataSource metadataSource;
    private final StatementParser parser;
    private final Map<Class<? extends JsonQLStatement>, StatementEngine<? extends JsonQLStatement, ? extends ExecuteResult>> executors;

    public JsonQLEngine(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
        this.parser = new StatementParser();
        this.executors = initializeExecutors();
    }

    private Map<Class<? extends JsonQLStatement>, StatementEngine<? extends JsonQLStatement, ? extends ExecuteResult>> initializeExecutors() {
        Map<Class<? extends JsonQLStatement>, StatementEngine<? extends JsonQLStatement, ? extends ExecuteResult>> executors = new HashMap<>();
        executors.put(SelectStatement.class, new SelectEngine(metadataSource));
        executors.put(InsertStatement.class, new InsertEngine(metadataSource));
        executors.put(UpdateStatement.class, new UpdateEngine(metadataSource));
        executors.put(DeleteStatement.class, new DeleteEngine(metadataSource));
        return executors;
    }

    public ExecuteResult execute(String jsonQuery, Class<? extends JsonQLStatement> jsonQLStatementType) throws Exception {
        JsonQLStatement statement = parser.parse(jsonQuery, jsonQLStatementType);
        try (Connection conn = DBConfig.getConnection()) {
            return execute(conn, statement);
        }
    }

    private ExecuteResult execute(Connection conn, JsonQLStatement statement) throws SQLException {
        StatementEngine<JsonQLStatement, ExecuteResult> executor = (StatementEngine<JsonQLStatement, ExecuteResult>) executors.get(statement.getClass());

        if (executor == null) {
            throw new IllegalStateException("Unsupported statement type: " + statement.getClass());
        }

        return executor.execute(conn, statement);
    }

    private static List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            Map<String, Map<String, Object>> relationData = new LinkedHashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);

                // 处理关联字段 (格式: relation_field)
                if (columnName.contains("_")) {
                    String[] parts = columnName.split("_", 2);
                    String relation = parts[0];
                    String field = parts[1];

                    relationData.computeIfAbsent(relation, k -> new LinkedHashMap<>())
                            .put(field, value);
                } else {
                    // 普通字段直接放入row
                    row.put(columnName, value);
                }
            }

            // 合并关联数据到结果
            relationData.forEach(row::put);

            result.add(row);
        }
        return result;
    }
}
