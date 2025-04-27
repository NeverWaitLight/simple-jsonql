package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.statement.model.Join;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;
import org.waitlight.simple.jsonql.statement.model.SelectStatement;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SelectExecutor extends StatementExecutor {
    private static SelectExecutor instance;

    private SelectExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public static synchronized SelectExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new SelectExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return processResultSet(rs);
        }
    }

    @Override
    protected String parseSql(JsonqlStatement statement) {
        if (!(statement instanceof SelectStatement)) {
            throw new IllegalArgumentException("Expected SelectStatement but got " + statement.getClass().getSimpleName());
        }
        SelectStatement selectStatement = (SelectStatement) statement;
        List<String> selectFields = new ArrayList<>(selectStatement.getSelect());
        PersistentClass entity = metadata.getEntityBinding(selectStatement.getFrom());

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

        // 处理 WHERE
        if (selectStatement.getWhere() != null) {
            sql.append(" WHERE ")
                    .append(selectStatement.getWhere().toString());
        }

        // 处理 JOIN
        if (selectStatement.getJoins() != null && !selectStatement.getJoins().isEmpty()) {
            for (Join joinItem : selectStatement.getJoins()) {
                sql.append(" ")
                        .append(joinItem.getType().toUpperCase())
                        .append(" JOIN ")
                        .append(joinItem.getTable())
                        .append(" ON ")
                        .append(joinItem.getOn().toString());
            }
        } else {
            sql.append(join);
        }

        // 处理 ORDER BY
        if (selectStatement.getOrderBy() != null) {
            sql.append(" ORDER BY ")
                    .append(selectStatement.getOrderBy().getField())
                    .append(" ")
                    .append(selectStatement.getOrderBy().getDirection());
        }

        // 处理 LIMIT 和 OFFSET
        if (selectStatement.getLimit() != null) {
            sql.append(" LIMIT ").append(selectStatement.getLimit());
            if (selectStatement.getOffset() != null) {
                sql.append(" OFFSET ").append(selectStatement.getOffset());
            }
        }

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