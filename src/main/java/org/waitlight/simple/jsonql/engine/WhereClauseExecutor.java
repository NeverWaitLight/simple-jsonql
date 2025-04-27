package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class WhereClauseExecutor extends StatementExecutor {

    public WhereClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    public String buildWhereClause(WhereCondition condition) {
        if (condition == null) return "";

        return switch (condition.getType()) {
            case COMPARISON -> buildComparison((ComparisonCondition) condition);
            case LOGICAL -> buildLogical((LogicalCondition) condition);
            case BETWEEN -> buildBetween((BetweenCondition) condition);
            case SUBQUERY -> buildSubquery((SubqueryCondition) condition);
            default -> throw new IllegalArgumentException("Unsupported condition type: " + condition.getType());
        };
    }

    private String buildSubquery(SubqueryCondition condition) {
        StringBuilder sb = new StringBuilder();
        if (condition.isNot()) {
            sb.append("NOT ");
        }
        sb.append("EXISTS (")
                .append(SelectExecutor.getInstance(metadataSources).parseSql(condition.getSubquery()).sql())
                .append(")");
        return sb.toString();
    }

    private String buildComparison(ComparisonCondition condition) {
        StringBuilder sb = new StringBuilder();
        if (condition.isNot()) {
            sb.append("NOT ");
        }
        sb.append(condition.getField())
                .append(" ")
                .append(condition.getOperatorType().getSymbol())
                .append(" ")
                .append(formatValue(condition.getValue()));
        return sb.toString();
    }

    private String buildLogical(LogicalCondition condition) {
        StringBuilder sb = new StringBuilder("(");
        List<WhereCondition> conditions = condition.getConditions();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                sb.append(" ").append(condition.getOperator().getSymbol()).append(" ");
            }
            sb.append(buildWhereClause(conditions.get(i)));
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildBetween(BetweenCondition condition) {
        StringBuilder sb = new StringBuilder();
        if (condition.isNot()) {
            sb.append("NOT ");
        }
        sb.append(condition.getField())
                .append(" BETWEEN ")
                .append(formatValue(condition.getStart()))
                .append(" AND ")
                .append(formatValue(condition.getEnd()));
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    @Override
    protected Object doExecute(Connection conn, SqlAndParameters sqlAndParameters) throws SQLException {
        throw new UnsupportedOperationException("WhereExecutor is only for building where clauses");
    }

    @Override
    protected SqlAndParameters parseSql(JsonqlStatement statement) {
        throw new UnsupportedOperationException("WhereExecutor is only for building where clauses");
    }
}
