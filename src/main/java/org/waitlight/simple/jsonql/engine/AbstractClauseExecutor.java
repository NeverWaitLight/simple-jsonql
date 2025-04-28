package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;

public abstract class AbstractClauseExecutor {
    protected final MetadataSources metadataSources;
    protected AbstractClauseExecutor next;

    public AbstractClauseExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public AbstractClauseExecutor setNext(AbstractClauseExecutor next) {
        this.next = next;
        return next;
    }

    public void process(Object condition, StringBuilder sql) {
        String result = buildClause(condition);
        if (result != null && !result.isEmpty()) sql.append(result);
        if (next != null) next.process(condition, sql);
    }

    protected abstract String buildClause(Object condition);
}
