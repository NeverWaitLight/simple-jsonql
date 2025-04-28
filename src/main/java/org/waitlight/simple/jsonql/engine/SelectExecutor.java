package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.metadata.PersistentClass;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.statement.model.*;

import java.sql.*;
import java.util.*;

@Slf4j
public class SelectExecutor extends StatementExecutor {
    private static SelectExecutor instance;
    private final ClauseExecutor clauseExecutor;

    private SelectExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.clauseExecutor = new ClauseExecutor(metadataSources);
    }

    public static synchronized SelectExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new SelectExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sqlAndParameters.sql());
             ResultSet rs = stmt.executeQuery()) {
            return processResultSet(rs);
        }
    }

    @Override
    protected SqlAndParameters parseSql(JsonqlStatement statement) {
        if (!(statement instanceof SelectStatement selectStatement))
            throw new IllegalArgumentException(
                    "Expected SelectStatement but got " + statement.getClass().getSimpleName());

        List<String> selectFields = new ArrayList<>(selectStatement.getSelect());
        PersistentClass entity = metadata.getEntityBinding(selectStatement.getFrom());

        // 处理关联字段
        for (Property prop : entity.getProperties()) {
            if (!selectFields.contains(prop.getName()) || Objects.isNull(prop.getRelationshipType())) continue;

            selectFields.remove(prop.getName());
            PersistentClass targetEntity = metadata.getEntityBinding(prop.getJoinTableName());

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

            // 添加 JOIN 信息
            Join join = new Join();
            join.setType(JoinType.LEFT); // 默认使用 LEFT JOIN
            join.setTable(targetEntity.getTableName().toLowerCase());

            // 构建 ON 条件
            ComparisonCondition onCondition = new ComparisonCondition();
            onCondition.setField(prop.getForeignKeyName());
            onCondition.setOperatorType(OperatorType.EQ);
            onCondition.setValue(targetEntity.getTableName().toLowerCase() + ".id");
            join.setOn(onCondition);

            // 添加到 joins 列表
            if (selectStatement.getJoins() == null) {
                selectStatement.setJoins(new ArrayList<>());
            }
            selectStatement.getJoins().add(join);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(String.join(", ", selectFields))
                .append(" FROM ")
                .append(entity.getTableName().toLowerCase());

        // 处理所有子句
        clauseExecutor.buildClause(selectStatement, sql);

        return new SqlAndParameters(sql.toString(), null);
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
