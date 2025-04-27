package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.model.Clause;

public abstract class ClauseExecutor {
    protected final MetadataSources metadataSources;

    public ClauseExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public abstract String buildClause(Clause clauseCondition);
}
