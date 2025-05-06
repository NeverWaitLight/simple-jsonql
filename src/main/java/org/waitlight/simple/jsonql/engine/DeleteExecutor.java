package org.waitlight.simple.jsonql.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.DeleteStatement;
import org.waitlight.simple.jsonql.statement.model.JsonQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DeleteExecutor extends StatementExecutor<DeleteStatement> {
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
    protected Object doExecute(Connection conn, PreparedSql<?> preparedSql) throws SQLException {
         if (preparedSql.statementType() != DeleteStatement.class) {
             throw new IllegalArgumentException("DeleteExecutor can only execute DeleteStatements");
         }

        try (PreparedStatement preparedStatement = conn.prepareStatement(preparedSql.sql())) {
            List<Object> parameters = preparedSql.parameters();
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }
            return preparedStatement.executeUpdate();
        }
    }

    @Override
    protected List<PreparedSql<DeleteStatement>> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof DeleteStatement deleteStatement)) {
            throw new IllegalArgumentException("Expected DeleteStatement but got " + statement.getClass().getSimpleName());
        }

        String entityId = deleteStatement.getEntityId();
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId is required for delete statements");
        }

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        // DELETE FROM clause
        sql.append("DELETE FROM ").append(entityId);

        // WHERE clause (优先处理 ids 列表)
        List<String> idsToDelete = deleteStatement.getIds();

        if (CollectionUtils.isNotEmpty(idsToDelete)) {
            // Build WHERE id IN (?, ?, ...) clause
            sql.append("\nWHERE id IN ("); // Assuming the primary key column is named 'id'
            sql.append(String.join(", ", java.util.Collections.nCopies(idsToDelete.size(), "?")));
            sql.append(")");
            parameters.addAll(idsToDelete); // Add all IDs as parameters
        } else {
            // Safety measure: Prevent accidental deletion of all rows if no condition specified
            log.error("Executing DELETE statement without a WHERE clause (no 'ids' provided) for entity: {}. Aborting.", entityId);
            // Throw an error to prevent deleting all records
            throw new IllegalArgumentException("WHERE clause (using 'ids') is mandatory for DELETE statements to prevent accidental data loss.");
        }

        List<PreparedSql<DeleteStatement>> result = new ArrayList<>();
        result.add(new PreparedSql<>(sql.toString(), parameters, DeleteStatement.class));
        return result;
    }
}
