package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.config.DBConfig;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.StatementParser;
import org.waitlight.simple.jsonql.statement.model.StatementType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

@Slf4j
public class JsonQLEngine {

    private final MetadataSources metadataSources;
    private final StatementParser parser;
    private final Map<StatementType, StatementEngine> executors;

    public JsonQLEngine(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.parser = new StatementParser();
        this.executors = initializeExecutors();
    }

    private Map<StatementType, StatementEngine> initializeExecutors() {
        Map<StatementType, StatementEngine> executors = new HashMap<>();
        executors.put(StatementType.QUERY, new SelectEngine(metadataSources));
        executors.put(StatementType.CREATE, new InsertEngine(metadataSources));
        executors.put(StatementType.UPDATE, new UpdateEngine(metadataSources));
        executors.put(StatementType.DELETE, new DeleteEngine(metadataSources));
        return executors;
    }

    public Object execute(String jsonQuery) throws Exception {
        JsonQLStatement statement = parser.parse2Stmt(jsonQuery);
        try (Connection conn = DBConfig.getConnection()) {
            return execute(conn, statement);
        }
    }

    private Object execute(Connection conn, JsonQLStatement statement) throws SQLException {
        StatementEngine executor = executors.get(statement.getStatement());
        if (executor == null) {
            throw new IllegalStateException("Unsupported statement type: " + statement.getStatement());
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
