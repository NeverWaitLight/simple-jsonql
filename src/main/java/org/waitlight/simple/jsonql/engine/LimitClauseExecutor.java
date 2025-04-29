package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.QueryStatement;
import org.waitlight.simple.jsonql.statement.model.Page;

public class LimitClauseExecutor extends AbstractClauseExecutor {
    public LimitClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (!(condition instanceof QueryStatement)) return "";

        QueryStatement select = (QueryStatement) condition;
        Page page = select.getPage();

        if (page == null || page.getSize() == null) {
            return "";
        }

        // 在SelectExecutor中已经处理了LIMIT和OFFSET，这里避免重复添加
        return "";
    }
}
