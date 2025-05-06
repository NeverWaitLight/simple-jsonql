package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.QueryStatement;
import org.waitlight.simple.jsonql.statement.model.*;

import java.util.List;

public class WhereClauseExecutor extends AbstractClauseExecutor {

    public WhereClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof QueryStatement selectStatement) {
            Filter filters = selectStatement.getFilters();
            if (filters == null || filters.getConditions() == null || filters.getConditions().isEmpty()) {
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(" WHERE ");
            
            List<Condition> conditions = filters.getConditions();
            String rel = filters.getRel() != null ? filters.getRel().toUpperCase() : "AND";
            
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sb.append(" ").append(rel).append(" ");
                }
                
                Condition cond = conditions.get(i);
                sb.append(buildCondition(cond));
            }
            
            return sb.toString();
        }
        
        if (condition instanceof WhereCondition whereCondition) {
            return buildWhereCondition(whereCondition);
        }
        return "";
    }
    
    private String buildCondition(Condition condition) {
        StringBuilder sb = new StringBuilder();
        sb.append(condition.getField()).append(" ");
        
        MethodType method = condition.getMethod();
        switch (method) {
            case IS -> {
                sb.append("IS ");
                if (condition.getValue() == null) {
                    sb.append("NULL");
                } else {
                    sb.append(condition.getValue());
                }
            }
            case IN -> {
                sb.append("IN (");
                if (condition.getValues() != null && !condition.getValues().isEmpty()) {
                    for (int i = 0; i < condition.getValues().size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(formatValue(condition.getValues().get(i)));
                    }
                } else {
                    sb.append(formatValue(condition.getValue()));
                }
                sb.append(")");
            }
            case LIKE -> sb.append("LIKE ").append(formatValue(condition.getValue()));
            default -> sb.append(method.getSymbol()).append(" ").append(formatValue(condition.getValue()));
        }
        
        return sb.toString();
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
                .append(QueryExecutor.getInstance(metadataSources).parseSql(condition.getSubquery()).get(0).sql())
                .append(")");
        return sb.toString();
    }

    private String buildBetween(BetweenCondition condition) {
        StringBuilder sb = new StringBuilder();
        sb.append(condition.getField());
        if (condition.isNot()) {
            sb.append(" NOT");
        }
        sb.append(" BETWEEN ")
                .append(formatValue(condition.getStart()))
                .append(" AND ")
                .append(formatValue(condition.getEnd()));
        return sb.toString();
    }

    private String buildLogical(LogicalCondition condition) {
        List<WhereCondition> conditions = condition.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                sb.append(" ").append(condition.getOperator().getSymbol()).append(" ");
            }
            sb.append(buildWhereCondition(conditions.get(i)));
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildComparison(ComparisonCondition condition) {
        StringBuilder sb = new StringBuilder();
        sb.append(condition.getField()).append(" ");
        if (condition.isNot()) {
            sb.append("NOT ");
        }
        
        MethodType op = condition.getOperatorType();
        switch (op) {
            case IS -> {
                sb.append("IS ");
                if (condition.getValue() == null) {
                    sb.append("NULL");
                } else {
                    sb.append(condition.getValue());
                }
            }
            case IN -> {
                sb.append("IN (");
                if (condition.getValue() instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(formatValue(list.get(i)));
                    }
                } else {
                    sb.append(formatValue(condition.getValue()));
                }
                sb.append(")");
            }
            default -> sb.append(op.getSymbol()).append(" ").append(formatValue(condition.getValue()));
        }
        
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value + "'";
    }
}
