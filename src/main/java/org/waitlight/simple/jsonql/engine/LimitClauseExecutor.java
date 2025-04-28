package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.SelectStatement;

public class LimitClauseExecutor extends AbstractClauseExecutor {
    public LimitClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (!(condition instanceof SelectStatement)) return "";
        
        SelectStatement select = (SelectStatement) condition;
        if (select.getLimit() == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(" LIMIT ").append(select.getLimit());
        if (select.getOffset() != null) {
            sb.append(" OFFSET ").append(select.getOffset());
        }
        return sb.toString();
    }
}
