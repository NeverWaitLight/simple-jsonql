package org.waitlight.simple.jsonql.engine.sqlparser;

import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.metadata.MetadataSources;

public abstract class AbstractClauseSqlParser {
    protected final MetadataSources metadataSources;

    public AbstractClauseSqlParser(MetadataSources metadataSources) {
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
