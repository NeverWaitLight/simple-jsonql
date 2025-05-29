package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.PageCriteria;

public class LimitClauseSqlBuilder extends AbstractClauseSqlBuilder {
    public LimitClauseSqlBuilder(MetadataSource metadataSource) {
        super(metadataSource);
    }

    @Override
    public String buildClause(Object condition) {
        if (!(condition instanceof SelectStatement)) return "";

        SelectStatement select = (SelectStatement) condition;
        PageCriteria page = select.getPage();

        if (page == null || page.getSize() == null) {
            return "";
        }

        // 在SelectExecutor中已经处理了LIMIT和OFFSET，这里避免重复添加
        return "";
    }
}
