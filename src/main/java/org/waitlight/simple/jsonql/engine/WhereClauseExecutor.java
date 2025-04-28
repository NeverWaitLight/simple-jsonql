package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.*;

import java.util.List;

public class WhereClauseExecutor extends ClauseExecutor {

    public WhereClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof SelectStatement) {
            SelectStatement select = (SelectStatement) condition;
            if (select.getWhere() == null) return "";
            return " WHERE " + buildWhereCondition(select.getWhere());
        }
        return "";
    }

    private String buildWhereCondition(WhereCondition whereCondition) {
        return switch (whereCondition.getType()) {
            case COMPARISON -> buildComparison((ComparisonCondition) whereCondition);
            case LOGICAL -> buildLogical((LogicalCondition) whereCondition);
            case BETWEEN -> buildBetween((BetweenCondition) whereCondition);
            case SUBQUERY -> buildSubquery((SubqueryCondition) whereCondition);
            default -> throw new IllegalArgumentException("Unsupported condition type: " + whereCondition.getType());
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
            sb.append(buildClause(conditions.get(i)));
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

}
