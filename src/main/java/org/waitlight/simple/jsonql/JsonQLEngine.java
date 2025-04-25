package org.waitlight.simple.jsonql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonQLEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Metadata metadata;

    public JsonQLEngine(MetadataSources metadataSources) {
        this.metadata = metadataSources.buildMetadata();
    }

    public List<Map<String, Object>> execute(String jsonQuery) throws Exception {
        JsonQL jql = objectMapper.readValue(jsonQuery, JsonQL.class);

        try (Connection conn = DBConfig.getConnection()) {
            return executeSelect(conn, jql);
        }
    }

    private List<Map<String, Object>> executeSelect(Connection conn, JsonQL query) throws SQLException {
        String sql = buildSelectSQL(query);

        log.info(sql);
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return processResultSet(rs);
        }
    }

    private String buildSelectSQL(JsonQL jql) {
        List<String> selectFields = new ArrayList<>(jql.select());
        PersistentClass entity = metadata.getEntityBinding(jql.from());

        StringBuilder sql = new StringBuilder();
        StringBuilder join = new StringBuilder();

        // 替换关联字段为实际的表字段
        for (Property prop : entity.getProperties()) {
            if (selectFields.contains(prop.getName()) && prop.getRelationshipType() != null) {

                selectFields.remove(prop.getName());
                PersistentClass targetEntity = metadata.getEntityBinding(prop.getTargetEntity().getSimpleName().toLowerCase());

                // 添加目标表字段
                for (Property targetProp : targetEntity.getProperties()) {
                    if (!"relationshipType".equals(targetProp.getName())) {
                        selectFields.add(String.format("%s.%s as %s_%s",
                                targetEntity.getTableName().toLowerCase(),
                                targetProp.getColumn(),
                                prop.getName(),
                                targetProp.getName()));
                    }
                }

                join.append(" LEFT JOIN ")
                        .append(targetEntity.getTableName().toLowerCase())
                        .append(" ON ")
                        .append(targetEntity.getTableName().toLowerCase())
                        .append(".")
                        .append(prop.getForeignKeyName())
                        .append(" = ")
                        .append(entity.getTableName().toLowerCase())
                        .append(".id");
            }
        }
        sql.append("SELECT ")
                .append(String.join(", ", selectFields))
                .append(" FROM ")
                .append(entity.getTableName().toLowerCase());

        // 处理WHERE
        if (jql.where() != null) {
            sql.append(" WHERE ")
                    .append(jql.where().condition());
        }
        sql.append(join);
        return sql.toString();
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
