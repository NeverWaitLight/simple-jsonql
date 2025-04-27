package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.Join;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.SQLException;

public class JoinClauseExecutor extends StatementExecutor {
    private final WhereClauseExecutor whereClauseExecutor;

    public JoinClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseExecutor(metadataSources);
    }

    public String buildJoinClause(Join join) {
        if (join == null) return "";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(join.getType().getValue())
                .append(" JOIN ")
                .append(join.getTable())
                .append(" ON ")
                .append(whereClauseExecutor.buildWhereClause(join.getOn()));
        return stringBuilder.toString();
    }

    @Override
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        throw new UnsupportedOperationException("JoinClauseExecutor is only for building join clauses");
    }

    @Override
    protected SqlAndParameters parseSql(JsonqlStatement statement) {
        throw new UnsupportedOperationException("JoinClauseExecutor is only for building join clauses");
    }
}
