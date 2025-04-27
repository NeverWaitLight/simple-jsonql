package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.Clause;
import org.waitlight.simple.jsonql.statement.model.OrderBy;

public class OrderByClauseExecutor extends ClauseExecutor {

    public OrderByClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Clause condition) {
        if (!(condition instanceof OrderBy)) {
            throw new IllegalArgumentException("Expected OrderBy but got " + condition.getClass().getSimpleName());
        }
        OrderBy orderBy = (OrderBy) condition;
        if (orderBy == null) return "";

        return " ORDER BY " + orderBy.getField() + " " + orderBy.getDirection().getValue();
    }

}
