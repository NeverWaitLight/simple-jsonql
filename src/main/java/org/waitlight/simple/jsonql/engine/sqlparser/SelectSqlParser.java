package org.waitlight.simple.jsonql.engine.sqlparser;

import org.waitlight.simple.jsonql.metadata.MetadataSources; // Keep for potential use by ClauseExecutor
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;
import org.waitlight.simple.jsonql.statement.model.Condition;
import org.waitlight.simple.jsonql.statement.model.Filter;
import org.waitlight.simple.jsonql.statement.model.Page;
import org.waitlight.simple.jsonql.statement.model.Sort;

import java.util.ArrayList;
import java.util.List;

public class SelectSqlParser {

    private final ClauseSqlParser clauseSqlParser; // For potential future use to delegate clause building

    public SelectSqlParser(MetadataSources metadataSources) {
        // Initialize ClauseExecutor if it's going to be used by the parser directly
        // Or, if QueryExecutor manages it, it could be passed in.
        // For now, let's assume if this parser becomes complex, it might instantiate
        // its own clause handlers
        // or be given them.
        // As per QueryExecutor, ClauseExecutor takes MetadataSources.
        this.clauseSqlParser = new ClauseSqlParser(metadataSources);
    }

    public PreparedSql<SelectStatement> parseSql(JsonQLStatement statement) {
        if (!(statement instanceof SelectStatement selectStatement)) {
            throw new IllegalArgumentException(
                    "Expected QueryStatement but got " + statement.getClass().getSimpleName());
        }

        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        Page page = selectStatement.getPage();
        Filter filters = selectStatement.getFilters();
        List<Sort> sort = selectStatement.getSort();

        String entityId = selectStatement.getEntityId();
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("EntityId is required for query");
        }

        // TODO: SELECT clause should be customizable, not always SELECT *
        sql.append("SELECT * FROM ").append(entityId);

        // WHERE clause (currently handled inline, could use WhereClauseExecutor from
        // ClauseExecutor)
        if (filters != null && filters.getConditions() != null && !filters.getConditions().isEmpty()) {
            sql.append(" WHERE ");
            String rel = filters.getRel() != null ? filters.getRel().toUpperCase() : "AND";
            List<Condition> conditions = filters.getConditions();

            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sql.append(" ").append(rel).append(" ");
                }

                Condition condition = conditions.get(i);
                // TODO: Sanitize condition.getField() to prevent SQL injection if not from
                // trusted source
                sql.append(condition.getField()).append(" ");

                switch (condition.getMethod()) {
                    case IS:
                        sql.append("IS ");
                        if (condition.getValue() == null) {
                            sql.append("NULL");
                        } else {
                            // This branch seems problematic for IS, typically it's IS NULL or IS NOT NULL
                            // If value is boolean (true/false), it should be appended directly.
                            // For other IS usages, this might need adjustment based on SQL dialect.
                            sql.append(condition.getValue()); // Re-evaluate this for IS with non-null value
                        }
                        break;
                    case IN:
                        sql.append("IN (");
                        if (condition.getValues() != null && !condition.getValues().isEmpty()) {
                            sql.append(String.join(", ",
                                    java.util.Collections.nCopies(condition.getValues().size(), "?")));
                            parameters.addAll(condition.getValues());
                        } else if (condition.getValue() != null) { // Handle single value for IN as well
                            sql.append("?");
                            parameters.add(condition.getValue());
                        } else {
                            // IN clause with no values is invalid SQL, could throw error or render as FALSE
                            // condition
                            sql.append("NULL"); // Or make it something like 1=0 to ensure no match
                        }
                        sql.append(")");
                        break;
                    case LIKE:
                        sql.append("LIKE ?");
                        parameters.add("%" + condition.getValue() + "%");
                        break;
                    // Add other operators as needed e.g., GT, LT, GTE, LTE, NEQ
                    default: // EQ or unspecified
                        sql.append("= ?");
                        parameters.add(condition.getValue());
                }
            }
        }

        // ORDER BY clause (currently handled inline, could use OrderByClauseExecutor
        // from ClauseExecutor)
        if (sort != null && !sort.isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> sortClauses = new ArrayList<>();
            for (Sort sortItem : sort) {
                // TODO: Sanitize sortItem.getField()
                String direction = sortItem.getDirection() != null ? sortItem.getDirection().toUpperCase() : "ASC";
                if (!("ASC".equals(direction) || "DESC".equals(direction))) {
                    direction = "ASC"; // Default to ASC if invalid direction
                }
                sortClauses.add(sortItem.getField() + " " + direction);
            }
            sql.append(String.join(", ", sortClauses));
        }

        // LIMIT/OFFSET clause (currently handled inline, could use LimitClauseExecutor
        // from ClauseExecutor)
        if (page != null) {
            int pageSize = page.getSize() > 0 ? page.getSize() : 10; // Default page size
            int pageNumber = page.getNumber() > 0 ? page.getNumber() : 1; // Default page number
            int offset = (pageNumber - 1) * pageSize;
            // SQL dialect for LIMIT/OFFSET can vary (e.g., LIMIT x OFFSET y vs LIMIT y, x)
            // Assuming MySQL/PostgreSQL style
            sql.append(" LIMIT ? OFFSET ?");
            parameters.add(pageSize);
            parameters.add(offset);
        }

        PreparedSql<SelectStatement> sqlObj = new PreparedSql<>();
        sqlObj.setSql(sql.toString());
        sqlObj.setParameters(parameters);
        sqlObj.setStatementType(SelectStatement.class);
        return sqlObj;
    }
}