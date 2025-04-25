package org.waitlight.simple.jsonql.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.config.DBConfig;
import org.waitlight.simple.jsonql.jql.JsonQL;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonQLEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MetadataSources metadataSources;

    public JsonQLEngine(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public Object execute(String jsonQuery) throws Exception {
        JsonQL jql = objectMapper.readValue(jsonQuery, JsonQL.class);
        try (Connection conn = DBConfig.getConnection()) {
            return execute(conn, jql);
        }
    }

    private Object execute(Connection conn, JsonQL jql) throws SQLException {
        String sql = "";
        switch (jql.statement()) {
            case SELECT -> {
                sql = new SelectEngine(metadataSources).parseSql(jql);
                log.info(sql);
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    return processResultSet(rs);
                }
            }
            case INSERT -> {
                sql = new InsertEngine(metadataSources).parseSql(jql);
                log.info(sql);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    return stmt.executeUpdate();
                }
            }
            case UPDATE -> {
                sql = new UpdateEngine(metadataSources).parseSql(jql);
                log.info(sql);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    return stmt.executeUpdate();
                }
            }
            case DELETE -> {
                sql = new DeleteEngine(metadataSources).parseSql(jql);
                log.info(sql);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    return stmt.executeUpdate();
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
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
