package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.engine.sqlparser.CreateSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.CreateStatement;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CreateEngine extends StatementEngine<CreateStatement> {
    private final CreateSqlParser createSqlParser;

    public CreateEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.createSqlParser = new CreateSqlParser(metadataSources.buildMetadata());
    }

    @Override
    public Object execute(Connection conn, CreateStatement stmt) throws SQLException {
        final PreparedSql<CreateStatement> preparedSql = createSqlParser.parseSql(stmt);

        if (preparedSql.getSql() == null || preparedSql.getSql().isEmpty()) {
            return 0;
        }

        log.info("Execute create statement on entity: {}", stmt.getEntityId());
        log.info("Main SQL: {}", preparedSql.getSql());
        if (preparedSql.getParameters() != null && !preparedSql.getParameters().isEmpty()) {
            log.info("Main Parameters: {}", preparedSql.getParameters());
        }

        if (!preparedSql.getNestedSQLs().isEmpty()) {
            log.info("Create statement contains {} nested SQL statements", preparedSql.getNestedSQLs().size());
        }

        int totalAffectedRows = 0;
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            Long generatedId = null;

            try (PreparedStatement ps = conn.prepareStatement(preparedSql.getSql(),
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < preparedSql.getParameters().size(); i++) {
                    ps.setObject(i + 1, preparedSql.getParameters().get(i));
                }

                int affected = ps.executeUpdate();
                totalAffectedRows += affected;

                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getLong(1);
                            log.info("Generated ID for main entity: {}", generatedId);
                        }
                    }
                }
            }

            if (generatedId != null && !preparedSql.getNestedSQLs().isEmpty()) {
                for (PreparedSql<CreateStatement> childSql : preparedSql.getNestedSQLs()) {
                    log.info("Executing nested SQL: {}", childSql.getSql());
                    if (childSql.getParameters() != null && !childSql.getParameters().isEmpty()) {
                        log.info("Nested Parameters (before ID replacement): {}", childSql.getParameters());
                    }

                    try (PreparedStatement ps = conn.prepareStatement(childSql.getSql())) {
                        List<Object> params = new ArrayList<>(childSql.getParameters());

                        for (int j = 0; j < params.size(); j++) {
                            Object param = params.get(j);
                            if (CreateSqlParser.FOREIGN_KEY_PLACEHOLDER.equals(param)) {
                                params.set(j, generatedId);
                            }
                            ps.setObject(j + 1, params.get(j));
                        }

                        log.info("Nested Parameters (after ID replacement): {}", params);
                        totalAffectedRows += ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            log.info("Transaction committed successfully, total affected rows: {}", totalAffectedRows);
            return totalAffectedRows;

        } catch (SQLException e) {
            try {
                conn.rollback();
                log.error("Transaction rolled back due to error: {}", e.getMessage());
            } catch (SQLException rollbackEx) {
                log.error("Error rolling back transaction", rollbackEx);
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                log.error("Error restoring autoCommit setting", e);
            }
        }
    }
}