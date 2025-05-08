package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.waitlight.simple.jsonql.engine.sqlparser.DeleteSqlParser;
import org.waitlight.simple.jsonql.engine.sqlparser.PreparedSql;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.DeleteStatement;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class DeleteEngine extends StatementEngine<DeleteStatement> {
    private static DeleteEngine instance;
    private final DeleteSqlParser deleteSqlParser;

    private DeleteEngine(MetadataSources metadataSources) {
        super(metadataSources);
        this.deleteSqlParser = new DeleteSqlParser();
    }

    public static synchronized DeleteEngine getInstance(MetadataSources metadataSources) {
        if (instance == null) {
            instance = new DeleteEngine(metadataSources);
        }
        return instance;
    }

    @Override
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
         if (preparedSql.getStatementType() != DeleteStatement.class) {
             throw new IllegalArgumentException("DeleteExecutor can only execute DeleteStatements");
         }

        try (PreparedStatement preparedStatement = conn.prepareStatement(preparedSql.getSql())) {
            List<Object> parameters = preparedSql.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }
            return preparedStatement.executeUpdate();
        }
    }

    @Override
    protected PreparedSql<DeleteStatement> parseSql(JsonQLStatement statement) {
        return deleteSqlParser.parseSql(statement);
    }
}
