package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.UpdateStatement;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;

import java.util.ArrayList;
import java.util.List;

public class UpdateSqlParser {

    private final MetadataSource metadataSource;
    // If WhereClauseExecutor logic needs to be integrated, it would be a dependency
    // here.
    // private final WhereClauseExecutor whereClauseExecutor;

    public UpdateSqlParser(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
        // this.whereClauseExecutor = new WhereClauseExecutor(this.metadataSources); //
        // If needed
    }

    public PreparedSql<UpdateStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof UpdateStatement updateStatement)) {
            throw new IllegalArgumentException(
                    "Expected UpdateStatement but got " + statement.getClass().getSimpleName());
        }

        if (updateStatement.getFields() == null || updateStatement.getFields().isEmpty()) {
            throw new IllegalArgumentException("Fields to update cannot be empty for an UPDATE statement.");
        }
        if (updateStatement.getDataId() == null) {
            // Or handle cases where update might be based on a more complex where clause
            throw new IllegalArgumentException(
                    "Data ID is required for the WHERE clause in this UPDATE statement implementation.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(updateStatement.getEntityId())
                .append(" SET ");

        List<String> setClauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        for (FieldStatement field : updateStatement.getFields()) {
            setClauses.add(field.getField() + " = ?");
            parameters.add(field.getValue());
        }

        sql.append(String.join(", ", setClauses));

        // Current implementation uses a simple WHERE id = ?
        // This could be expanded to use a WhereClauseParser if more complex conditions
        // are needed.
        sql.append(" WHERE id = ?"); // Assuming 'id' is the primary key column to update by
        parameters.add(updateStatement.getDataId());

        PreparedSql<UpdateStatement> sqlObj = new PreparedSql<>();
        sqlObj.setSql(sql.toString());
        sqlObj.setParameters(parameters);
        sqlObj.setStatementType(UpdateStatement.class);
        return sqlObj;
    }
}