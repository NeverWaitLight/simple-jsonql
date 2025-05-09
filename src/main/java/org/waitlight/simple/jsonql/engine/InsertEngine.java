package org.waitlight.simple.jsonql.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.engine.sqlparser.InsertSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.InsertStatement;

import lombok.extern.slf4j.Slf4j;

/**
 * 处理插入语句的引擎实现类，负责将InsertStatement转换为SQL并执行
 */
@Slf4j
public class InsertEngine extends StatementEngine<InsertStatement> {
    private final InsertSqlParser insertSqlParser;

    /**
     * 创建InsertEngine实例
     * 
     * @param metadataSources 元数据来源，用于SQL解析器初始化
     */
    public InsertEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.insertSqlParser = new InsertSqlParser(metadataSources.buildMetadata());
    }

    /**
     * 执行插入语句
     * 
     * @param conn 数据库连接
     * @param stmt 待执行的插入语句
     * @return 受影响的行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    @Override
    public Object execute(Connection conn, InsertStatement stmt) throws SQLException {
        final PreparedSql<InsertStatement> preparedSql = insertSqlParser.parseStmt2Sql(stmt);

        if (StringUtils.isBlank(preparedSql.getSql())) {
            return 0;
        }

        logStatementInfo(stmt, preparedSql);

        int totalAffectedRows = 0;
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            Long generatedId = executeMainStatement(conn, preparedSql);
            int nestedRows = executeNestedStatements(conn, preparedSql, generatedId);

            // 将主实体和嵌套实体的行数加起来
            totalAffectedRows = (generatedId != null ? 1 : 0) + nestedRows;

            conn.commit();
            log.info("事务提交成功，总影响行数: {}", totalAffectedRows);
            return totalAffectedRows;
        } catch (SQLException e) {
            handleTransactionError(conn, e);
            throw e;
        } finally {
            restoreAutoCommit(conn, originalAutoCommit);
        }
    }

    /**
     * 记录SQL语句和相关参数的日志信息
     * 
     * @param stmt        插入语句
     * @param preparedSql 预处理SQL对象
     */
    private void logStatementInfo(InsertStatement stmt, PreparedSql<InsertStatement> preparedSql) {
        log.info("执行实体插入语句: {}", stmt.getEntityId());
        log.info("主SQL: {}", preparedSql.getSql());

        if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
            log.info("主SQL参数: {}", preparedSql.getParameters());
        }

        if (CollectionUtils.isNotEmpty(preparedSql.getNestedSQLs())) {
            log.info("插入语句包含 {} 个嵌套SQL语句", preparedSql.getNestedSQLs().size());
        }
    }

    /**
     * 执行主SQL语句
     * 
     * @param conn        数据库连接
     * @param preparedSql 预处理SQL对象
     * @return 生成的主键ID，如果没有生成则返回null
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private Long executeMainStatement(Connection conn, PreparedSql<InsertStatement> preparedSql)
            throws SQLException {
        if (StringUtils.isBlank(preparedSql.getSql())) {
            return null;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                preparedSql.getSql(), PreparedStatement.RETURN_GENERATED_KEYS)) {

            setStatementParameters(ps, preparedSql.getParameters());
            int affected = ps.executeUpdate();

            return extractGeneratedId(ps, affected);
        }
    }

    /**
     * 从执行结果中提取生成的主键ID
     * 
     * @param ps       已执行的PreparedStatement
     * @param affected 受影响的行数
     * @return 生成的主键ID，如果没有生成则返回null
     * @throws SQLException 当获取生成键时发生错误时抛出
     */
    private Long extractGeneratedId(PreparedStatement ps, int affected) throws SQLException {
        if (affected <= 0) {
            return null;
        }

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                Long generatedId = rs.getLong(1);
                log.info("主实体生成ID: {}", generatedId);
                return generatedId;
            }
        }

        return null;
    }

    /**
     * 执行所有嵌套SQL语句
     * 
     * @param conn        数据库连接
     * @param preparedSql 包含嵌套SQL的预处理SQL对象
     * @param generatedId 主语句生成的ID
     * @return 嵌套语句影响的总行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private int executeNestedStatements(
            Connection conn, PreparedSql<InsertStatement> preparedSql, Long generatedId)
            throws SQLException {

        int nestedAffectedRows = 0;

        if (CollectionUtils.isEmpty(preparedSql.getNestedSQLs())) {
            return nestedAffectedRows;
        }

        // 即使generatedId为null，仍然尝试执行嵌套SQL，因为可能存在多对一关系
        // 这种情况下嵌套SQL可能是引用现有记录而不需要主记录ID
        for (PreparedSql<InsertStatement> childSql : preparedSql.getNestedSQLs()) {
            // 检查SQL不为空才执行
            if (StringUtils.isNotBlank(childSql.getSql())) {
                nestedAffectedRows += executeNestedStatement(conn, childSql, generatedId);
            }
        }

        return nestedAffectedRows;
    }

    /**
     * 执行单个嵌套SQL语句
     * 
     * @param conn        数据库连接
     * @param childSql    嵌套SQL对象
     * @param generatedId 主语句生成的ID
     * @return 该嵌套语句影响的行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private int executeNestedStatement(
            Connection conn, PreparedSql<InsertStatement> childSql, Long generatedId)
            throws SQLException {

        log.info("执行嵌套SQL: {}", childSql.getSql());

        if (CollectionUtils.isNotEmpty(childSql.getParameters())) {
            log.info("嵌套SQL参数 (ID替换前): {}", childSql.getParameters());
        }

        try (PreparedStatement ps = conn.prepareStatement(childSql.getSql())) {
            List<Object> params = replaceForeignKeyPlaceholders(childSql.getParameters(), generatedId);
            setStatementParameters(ps, params);

            log.info("嵌套SQL参数 (ID替换后): {}", params);
            return ps.executeUpdate();
        }
    }

    private List<Object> replaceForeignKeyPlaceholders(List<Object> parameters, Long generatedId) {
        if (CollectionUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }

        List<Object> params = new ArrayList<>(parameters);
        for (int j = 0; j < params.size(); j++) {
            Object param = params.get(j);
            if (param != null && InsertSqlParser.FOREIGN_KEY_PLACEHOLDER.equals(param.toString())) {
                params.set(j, generatedId);
            }
        }

        return params;
    }

    private void setStatementParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        if (CollectionUtils.isEmpty(parameters)) {
            return;
        }

        for (int i = 0; i < parameters.size(); i++) {
            ps.setObject(i + 1, parameters.get(i));
        }
    }

    private void handleTransactionError(Connection conn, SQLException e) {
        try {
            conn.rollback();
            log.error("事务回滚，错误原因: {}", e.getMessage());
        } catch (SQLException rollbackEx) {
            log.error("事务回滚失败", rollbackEx);
        }
    }

    private void restoreAutoCommit(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
            log.error("恢复AutoCommit设置失败", e);
        }
    }
}