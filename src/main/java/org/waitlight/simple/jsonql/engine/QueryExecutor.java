package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.QueryStatement;
import org.waitlight.simple.jsonql.statement.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryExecutor extends StatementExecutor {
    private static QueryExecutor instance;
    private final ClauseExecutor clauseExecutor;

    private QueryExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.clauseExecutor = new ClauseExecutor(metadataSources);
    }

    public static synchronized QueryExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new QueryExecutor(metadataSources);
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
    protected SqlAndParameters parseSql(JsonQLStatement statement) {
        if (!(statement instanceof QueryStatement queryStatement))
            throw new IllegalArgumentException(
                    "Expected QueryStatement but got " + statement.getClass().getSimpleName());

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        // 获取页面设置和筛选条件
        Page page = queryStatement.getPage();
        Filter filters = queryStatement.getFilters();
        List<Sort> sort = queryStatement.getSort();

        // 确定查询的实体/表名
        String entityId = queryStatement.getEntityId();
        if (entityId == null) {
            throw new IllegalArgumentException("EntityId is required for query");
        }

        // 构建基本 SELECT 语句
        sql.append("SELECT * FROM ").append(entityId);

        // 处理 WHERE 子句 (filters)
        if (filters != null && filters.getConditions() != null && !filters.getConditions().isEmpty()) {
            sql.append(" WHERE ");

            List<Condition> conditions = filters.getConditions();
            String rel = filters.getRel() != null ? filters.getRel().toUpperCase() : "AND";
            
            for (int i = 0; i < conditions.size(); i++) {
                Condition condition = conditions.get(i);
                
                if (i > 0) {
                    sql.append(" ").append(rel).append(" ");
                }
                
                sql.append(condition.getField()).append(" ");
                
                // 根据方法类型添加操作符
                switch (condition.getMethod()) {
                    case EQ -> sql.append("= ?");
                    case NE -> sql.append("<> ?");
                    case GT -> sql.append("> ?");
                    case GE -> sql.append(">= ?");
                    case LT -> sql.append("< ?");
                    case LE -> sql.append("<= ?");
                    case LIKE -> sql.append("LIKE ?");
                    case IN -> {
                        sql.append("IN (");
                        List<Object> values = condition.getValues();
                        if (values != null && !values.isEmpty()) {
                            for (int j = 0; j < values.size(); j++) {
                                if (j > 0) {
                                    sql.append(", ");
                                }
                                sql.append("?");
                                parameters.add(values.get(j));
                            }
                        } else {
                            sql.append("?");
                            parameters.add(condition.getValue());
                        }
                        sql.append(")");
                        continue; // 跳过下面的单值参数添加
                    }
                    default -> sql.append("= ?");
                }
                
                // 添加参数（针对非IN条件）
                if (condition.getMethod() != MethodType.IN) {
                    parameters.add(condition.getValue());
                }
            }
        }

        // 处理 ORDER BY 子句 (sort)
        if (sort != null && !sort.isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < sort.size(); i++) {
                Sort sortItem = sort.get(i);
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(sortItem.getField()).append(" ").append(sortItem.getDirection());
            }
        }

        // 处理 LIMIT 和 OFFSET 子句 (page)
        if (page != null) {
            if (page.getSize() != null) {
                sql.append(" LIMIT ").append(page.getSize());
            }
            if (page.getNumber() != null && page.getSize() != null) {
                int offset = (page.getNumber() - 1) * page.getSize();
                if (offset > 0) {
                    sql.append(" OFFSET ").append(offset);
                }
            }
        }

        return new SqlAndParameters(sql.toString(), parameters);
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