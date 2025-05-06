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
public class QueryExecutor extends StatementExecutor<QueryStatement> {
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
        if (preparedSql.getStatementType() != QueryStatement.class) {
            throw new IllegalArgumentException("QueryExecutor can only execute QueryStatements");
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
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
            String rel = filters.getRel() != null ? filters.getRel().toUpperCase() : "AND";
            List<Condition> conditions = filters.getConditions();

            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sql.append(" ").append(rel).append(" ");
                }

                Condition condition = conditions.get(i);
                sql.append(condition.getField()).append(" ");

                switch (condition.getMethod()) {
                    case IS:
                        sql.append("IS ");
                        if (condition.getValue() == null) {
                            sql.append("NULL");
                        } else {
                            sql.append(condition.getValue());
                        }
                        break;
                    case IN:
                        sql.append("IN (");
                        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
                            sql.append(String.join(", ", java.util.Collections.nCopies(condition.getValues().size(), "?")));
                            parameters.addAll(condition.getValues());
                        } else {
                            sql.append("?");
                            parameters.add(condition.getValue());
                        }
                        sql.append(")");
                        break;
                    case LIKE:
                        sql.append("LIKE ?");
                        parameters.add("%" + condition.getValue() + "%");
                        break;
                    default:
                        sql.append("= ?");
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

        PreparedSql<QueryStatement> sqlObj = new PreparedSql<>();
        sqlObj.setSql(sql.toString());
        sqlObj.setParameters(parameters);
        sqlObj.setStatementType(QueryStatement.class);
        return sqlObj;
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