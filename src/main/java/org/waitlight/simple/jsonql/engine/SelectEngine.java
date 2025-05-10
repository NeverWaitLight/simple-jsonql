package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.waitlight.simple.jsonql.engine.result.SelectResult;
import org.waitlight.simple.jsonql.engine.sqlparser.ClauseSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.engine.sqlparser.SelectSqlParser;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectEngine extends StatementEngine<SelectStatement, SelectResult> {
    private final ClauseSqlParser clauseSqlParser;
    private final SelectSqlParser selectSqlParser;

    public SelectEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.clauseSqlParser = new ClauseSqlParser(metadataSources);
        this.selectSqlParser = new SelectSqlParser(metadataSources);
    }

    @Override
    public SelectResult execute(Connection conn, SelectStatement statement) throws SQLException {
        PreparedSql<SelectStatement> preparedSql = selectSqlParser.parseSql(statement);

        if (preparedSql.getStatementType() != SelectStatement.class) {
            throw new IllegalArgumentException("SelectEngine can only execute SelectStatements");
        }

        log.info("执行查询语句 Entity: {}", statement.getEntityId());
        log.info("主SQL: {}", preparedSql.getSql());
        if (preparedSql.getParameters() != null && !preparedSql.getParameters().isEmpty()) {
            log.info("主SQL参数: {}", preparedSql.getParameters());
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> records = processResultSet(rs);

                // 处理分页信息
                Page page = statement.getPage();
                int pageSize = (page != null && page.getSize() != null) ? page.getSize() : records.size();
                int pageNumber = (page != null && page.getNumber() != null) ? page.getNumber() : 1;

                // 如果有分页信息，则需要执行计数查询
                if (page != null && page.getSize() != null) {
                    int totalCount = getTotalCount(conn, statement);
                    return SelectResult.of(records, totalCount, pageSize, pageNumber);
                } else {
                    return SelectResult.of(records);
                }
            }
        } catch (SQLException e) {
            log.error("查询执行失败 Entity: {}. SQL: {}. Error: {}", statement.getEntityId(), preparedSql.getSql(),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * 获取总记录数（用于分页）
     * 
     * @param conn      数据库连接
     * @param statement 查询语句
     * @return 总记录数
     * @throws SQLException 当SQL执行错误时抛出
     */
    private int getTotalCount(Connection conn, SelectStatement statement) throws SQLException {
        // 直接重用SelectSqlParser为我们生成的SQL，但只保留WHERE部分
        PreparedSql<SelectStatement> preparedSql = selectSqlParser.parseSql(statement);
        String fullSql = preparedSql.getSql();
        List<Object> fullParameters = preparedSql.getParameters();

        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) FROM ").append(statement.getEntityId());

        // 提取WHERE部分的SQL和参数
        List<Object> countParameters = new ArrayList<>();
        int whereIndex = fullSql.indexOf("WHERE");

        if (whereIndex != -1) {
            // 从完整SQL中提取WHERE子句部分
            int orderByIndex = fullSql.indexOf("ORDER BY");
            int limitIndex = fullSql.indexOf("LIMIT");

            int endIndex;
            if (orderByIndex != -1) {
                endIndex = orderByIndex;
            } else if (limitIndex != -1) {
                endIndex = limitIndex;
            } else {
                endIndex = fullSql.length();
            }

            // 提取条件部分并添加到计数SQL中
            countSql.append(" WHERE ");
            countSql.append(fullSql.substring(whereIndex + 6, endIndex).trim());

            // 计算需要多少个参数
            int placeholderCount = 0;
            for (int i = whereIndex; i < endIndex; i++) {
                if (fullSql.charAt(i) == '?') {
                    placeholderCount++;
                }
            }

            // 添加相应数量的参数
            for (int i = 0; i < placeholderCount; i++) {
                countParameters.add(fullParameters.get(i));
            }
        }

        log.info("执行计数SQL: {}", countSql);
        if (!countParameters.isEmpty()) {
            log.info("计数SQL参数: {}", countParameters);
        }

        try (PreparedStatement stmt = conn.prepareStatement(countSql.toString())) {
            // 设置参数
            for (int i = 0; i < countParameters.size(); i++) {
                stmt.setObject(i + 1, countParameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    private List<Map<String, Object>> processResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
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
            results.add(row);
        }
        return results;
    }
} 