package org.waitlight.simple.jsonql.builder;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.statement.DeleteStatement;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.util.ArrayList;
import java.util.List;

public class DeleteSqlParser {

    private static final Logger log = LoggerFactory.getLogger(DeleteSqlParser.class);
    // No MetadataSources needed for basic delete by ID currently

    public DeleteSqlParser() {
        // Constructor
    }

    public PreparedSql<DeleteStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof DeleteStatement deleteStatement)) {
            throw new IllegalArgumentException(
                    "Expected DeleteStatement but got " + statement.getClass().getSimpleName());
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
            // Safety measure: Prevent accidental deletion of all rows if no condition
            // specified
            log.warn("Attempting to build DELETE statement without specific IDs for entity: {}. " +
                    "This parser requires 'ids' for DELETE.", entityId);
            // Throw an error to prevent generating a statement that would delete all
            // records.
            // The executor might have further checks, but the parser itself should indicate
            // this limitation.
            throw new IllegalArgumentException(
                    "Condition (using 'ids') is mandatory for building DELETE SQL statements.");
        }

        PreparedSql<DeleteStatement> sqlObj = new PreparedSql<>();
        sqlObj.setSql(sql.toString());
        sqlObj.setParameters(parameters);
        sqlObj.setStatementType(DeleteStatement.class);
        return sqlObj;
    }
}