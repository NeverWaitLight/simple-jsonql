package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.DeleteStatement;
import org.waitlight.simple.jsonql.statement.model.JsonqlStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class DeleteExecutor extends StatementExecutor {
    private static DeleteExecutor instance;

    private DeleteExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public static synchronized DeleteExecutor getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new DeleteExecutor(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sqlAndParameters.sql())) {
            return stmt.executeUpdate();
        }
    }

    @Override
    protected SqlAndParameters parseSql(JsonqlStatement statement) {
        if (!(statement instanceof DeleteStatement)) {
            throw new IllegalArgumentException("Expected DeleteStatement but got " + statement.getClass().getSimpleName());
        }
        DeleteStatement deleteStatement = (DeleteStatement) statement;
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ")
                .append(deleteStatement.getFrom());

        // 处理 WHERE 子句
        if (deleteStatement.getWhere() != null) {
            sql.append(" WHERE ")
                    .append(deleteStatement.getWhere().toString());
        }
        return new SqlAndParameters(sql.toString(), null);
    }
} 