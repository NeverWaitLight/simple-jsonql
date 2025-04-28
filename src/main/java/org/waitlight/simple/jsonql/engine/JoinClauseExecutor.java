package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.Join;
import org.waitlight.simple.jsonql.statement.model.SelectStatement;

public class JoinClauseExecutor extends ClauseExecutor {
    private final WhereClauseExecutor whereClauseExecutor;

    public JoinClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseExecutor(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof SelectStatement) {
            SelectStatement select = (SelectStatement) condition;
            if (select.getJoins() == null || select.getJoins().isEmpty()) return "";
            
            StringBuilder sb = new StringBuilder();
            for (Join join : select.getJoins()) {
                sb.append(" ")
                  .append(join.getType().getValue())
                  .append(" JOIN ")
                  .append(join.getTable())
                  .append(" ON ")
                  .append(whereClauseExecutor.buildClause(join.getOn()));
            }
            return sb.toString();
        }
        return "";
    }

}
