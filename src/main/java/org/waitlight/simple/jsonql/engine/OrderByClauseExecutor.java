package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.SelectStatement;

public class OrderByClauseExecutor extends ClauseExecutor {

    public OrderByClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof SelectStatement) {
            SelectStatement select = (SelectStatement) condition;
            if (select.getOrderBy() == null) return "";
            return " ORDER BY " + select.getOrderBy().getField() + " " + select.getOrderBy().getDirection().getValue();
        }
        return "";
    }

}
