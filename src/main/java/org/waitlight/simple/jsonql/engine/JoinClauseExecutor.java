package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.Clause;
import org.waitlight.simple.jsonql.statement.model.Join;

public class JoinClauseExecutor extends ClauseExecutor {
    private final WhereClauseExecutor whereClauseExecutor;

    public JoinClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseExecutor(metadataSources);
    }

    @Override
    public String buildClause(Clause condition) {
        if (!(condition instanceof Join)) {
            throw new IllegalArgumentException("Expected Join but got " + condition.getClass().getSimpleName());
        }
        Join join = (Join) condition;
        if (join == null) return "";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(join.getType().getValue())
                .append(" JOIN ")
                .append(join.getTable())
                .append(" ON ")
                .append(whereClauseExecutor.buildClause(join.getOn()));
        return stringBuilder.toString();
    }

}
