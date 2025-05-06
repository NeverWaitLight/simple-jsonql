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
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
        if (preparedSql.statementType() != QueryStatement.class) {
            throw new IllegalArgumentException("QueryExecutor can only execute QueryStatements");
        }

        if (!preparedSql.nestedCreateStatements().isEmpty()) {
            log.warn("Nested statements found in a QUERY operation, they will be ignored.");
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.sql())) {
            List<Object> parameters = preparedSql.parameters();
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return processResultSet(rs);
            }
        }
    }

    @Override
    protected PreparedSql<QueryStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof QueryStatement queryStatement)) {
            throw new IllegalArgumentException("Expected QueryStatement but got " + statement.getClass().getSimpleName());
        }

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

        // 处理排序
        if (sort != null && !sort.isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> sortClauses = new ArrayList<>();
            for (Sort sortItem : sort) {
                String direction = sortItem.getDirection() != null ? 
                    sortItem.getDirection().toUpperCase() : "ASC";
                sortClauses.add(sortItem.getField() + " " + direction);
            }
            sql.append(String.join(", ", sortClauses));
        }

        // 处理分页
        if (page != null) {
            int pageSize = page.getSize() > 0 ? page.getSize() : 10;
            int offset = (page.getNumber() - 1) * pageSize;
            sql.append(" LIMIT ? OFFSET ?");
            parameters.add(pageSize);
            parameters.add(offset);
        }

        return new PreparedSql<>(sql.toString(), parameters, QueryStatement.class);
    }

    private List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            results.add(row);
        }

        return results;
    }
} 