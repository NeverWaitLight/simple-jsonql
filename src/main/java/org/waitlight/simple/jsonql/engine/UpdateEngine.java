package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
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
public class UpdateEngine extends StatementEngine<UpdateStatement> {
    private final WhereClauseSqlParser whereClauseExecutor;
    private final UpdateSqlParser updateSqlParser;

    public UpdateEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseSqlParser(metadataSources);
        this.updateSqlParser = new UpdateSqlParser(metadataSources);
    }

    @Override
    public Object execute(Connection conn, UpdateStatement statement) throws SQLException {
        PreparedSql<UpdateStatement> preparedSql = updateSqlParser.parseSql(statement);

        if (preparedSql.getStatementType() != UpdateStatement.class) {
            throw new IllegalArgumentException("UpdateExecutor can only execute UpdateStatements");
        }

        try (PreparedStatement stmt = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            return stmt.executeUpdate();
        }
    }
}
