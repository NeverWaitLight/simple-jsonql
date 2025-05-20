package org.waitlight.simple.jsonql.builder;

import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.metadata.MetadataSource;

public abstract class AbstractClauseSqlParser {
    protected final MetadataSource metadataSource;

    public AbstractClauseSqlParser(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public void process(Object condition, StringBuilder sql) {
        String result = buildClause(condition);
        if (StringUtils.isNotBlank(result)) {
            sql.append(result);
        }
    }

    protected abstract String buildClause(Object condition);
}
