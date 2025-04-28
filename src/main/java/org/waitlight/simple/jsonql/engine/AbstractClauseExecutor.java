package org.waitlight.simple.jsonql.engine;

import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

public abstract class AbstractClauseExecutor {
    protected final MetadataSources metadataSources;

    public AbstractClauseExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
    }

    public void process(Object condition, StringBuilder sql) {
        String result = buildClause(condition);
        if (StringUtils.isNotBlank(result)) {
            sql.append(result);
        }
    }

    protected abstract String buildClause(Object condition);
}
