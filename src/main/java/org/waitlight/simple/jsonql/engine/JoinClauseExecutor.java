package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.ComparisonCondition;
import org.waitlight.simple.jsonql.statement.model.Join;
import org.waitlight.simple.jsonql.statement.model.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.WhereCondition;

public class JoinClauseExecutor extends AbstractClauseExecutor {

    public JoinClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (!(condition instanceof SelectStatement select)) return "";
        if (select.getJoins() == null || select.getJoins().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Join join : select.getJoins()) {
            sb.append(" ")
              .append(join.getType().getValue())
              .append(" JOIN ")
              .append(join.getTable())
              .append(" ON ");

            // 处理 ON 条件
            WhereCondition onCondition = join.getOn();
            if (onCondition != null) {
                sb.append(buildOnCondition(onCondition));
            }
        }
        return sb.toString();
    }

    private String buildOnCondition(WhereCondition condition) {
        if (condition instanceof ComparisonCondition) {
            ComparisonCondition comp = (ComparisonCondition) condition;
            StringBuilder sb = new StringBuilder();
            if (comp.isNot()) {
                sb.append("NOT ");
            }
            sb.append(comp.getField())
              .append(" ")
              .append(comp.getOperatorType().getSymbol())
              .append(" ")
              .append(comp.getValue());
            return sb.toString();
        }
        throw new IllegalArgumentException("Unsupported ON condition type: " + condition.getType());
    }
}
