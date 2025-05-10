package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.engine.result.UpdateResult;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.engine.sqlparser.UpdateSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.WhereClauseSqlParser;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.UpdateStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class UpdateEngine extends StatementEngine<UpdateStatement, UpdateResult> {
    private final WhereClauseSqlParser whereClauseExecutor;
    private final UpdateSqlParser updateSqlParser;

    // Helper record/class for main statement execution result
    private record MainUpdateDetail(int affectedRows) {
    }

    // Helper record/class for nested statement execution result
    private record NestedUpdateDetail(int affectedRows) {
    }

    public UpdateEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseSqlParser(metadataSources);
        this.updateSqlParser = new UpdateSqlParser(metadataSources);
    }

    @Override
    public UpdateResult execute(Connection conn, UpdateStatement statement) throws SQLException {
        PreparedSql<UpdateStatement> preparedSql = updateSqlParser.parseSql(statement);

        if (preparedSql.getStatementType() != UpdateStatement.class) {
            throw new IllegalArgumentException("UpdateEngine can only execute UpdateStatements");
        }

        logStatementInfo(statement, preparedSql);

        boolean originalAutoCommit = conn.getAutoCommit();
        int mainAffectedRows = 0;
        int nestedAffectedRows = 0;

        try {
            conn.setAutoCommit(false);

            MainUpdateDetail mainResult = executeMainStatement(conn, preparedSql);
            mainAffectedRows = mainResult.affectedRows();

            // 执行嵌套更新（如果有的话）
            if (CollectionUtils.isNotEmpty(preparedSql.getNestedSQLs())) {
                for (PreparedSql<UpdateStatement> nestedSql : preparedSql.getNestedSQLs()) {
                    NestedUpdateDetail nestedResult = executeNestedStatement(conn, nestedSql);
                    nestedAffectedRows += nestedResult.affectedRows();
                }
            }

            conn.commit();
            int totalAffectedRows = mainAffectedRows + nestedAffectedRows;
            log.info("更新事务提交成功，总影响行数: {}, 主表影响行数: {}, 嵌套表影响行数: {}",
                    totalAffectedRows, mainAffectedRows, nestedAffectedRows);

            return UpdateResult.of(mainAffectedRows, nestedAffectedRows);
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
     * @param stmt        更新语句
     * @param preparedSql 预处理SQL对象
     */
    private void logStatementInfo(UpdateStatement stmt, PreparedSql<UpdateStatement> preparedSql) {
        log.info("执行实体更新语句: {}, ID: {}", stmt.getEntityId(), stmt.getDataId());
        if (StringUtils.isNotBlank(preparedSql.getSql())) {
            log.info("主SQL: {}", preparedSql.getSql());
            if (CollectionUtils.isNotEmpty(preparedSql.getParameters())) {
                log.info("主SQL参数: {}", preparedSql.getParameters());
            }
        }
        if (CollectionUtils.isNotEmpty(preparedSql.getNestedSQLs())) {
            log.info("更新语句包含 {} 个嵌套SQL语句", preparedSql.getNestedSQLs().size());
        }
    }

    /**
     * 执行主SQL语句
     * 
     * @param conn        数据库连接
     * @param preparedSql 预处理SQL对象
     * @return MainUpdateDetail 包含影响的行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private MainUpdateDetail executeMainStatement(Connection conn, PreparedSql<UpdateStatement> preparedSql)
            throws SQLException {
        if (StringUtils.isBlank(preparedSql.getSql())) {
            return new MainUpdateDetail(0);
        }

        try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql())) {
            setStatementParameters(ps, preparedSql.getParameters());
            int affected = ps.executeUpdate();
            return new MainUpdateDetail(affected);
        }
    }

    /**
     * 执行嵌套SQL语句
     * 
     * @param conn      数据库连接
     * @param nestedSql 嵌套的预处理SQL对象
     * @return NestedUpdateDetail 包含影响的行数
     * @throws SQLException 当SQL执行发生错误时抛出
     */
    private NestedUpdateDetail executeNestedStatement(Connection conn, PreparedSql<UpdateStatement> nestedSql)
            throws SQLException {
        if (StringUtils.isBlank(nestedSql.getSql())) {
            return new NestedUpdateDetail(0);
        }

        log.info("执行嵌套更新SQL: {}", nestedSql.getSql());
        if (CollectionUtils.isNotEmpty(nestedSql.getParameters())) {
            log.info("嵌套SQL参数: {}", nestedSql.getParameters());
        }

        try (PreparedStatement ps = conn.prepareStatement(nestedSql.getSql())) {
            setStatementParameters(ps, nestedSql.getParameters());
            int affected = ps.executeUpdate();
            return new NestedUpdateDetail(affected);
        }
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
            log.error("更新事务回滚，错误原因: {}", e.getMessage());
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
