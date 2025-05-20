package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.SortCriteria;

import java.util.List;

public class OrderByClauseSqlParser extends AbstractClauseSqlParser {

    public OrderByClauseSqlParser(MetadataSource metadataSource) {
        super(metadataSource);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof SelectStatement) {
            SelectStatement select = (SelectStatement) condition;
            List<SortCriteria> sortList = select.getSort();

            if (sortList == null || sortList.isEmpty()) {
                return "";
            }

            // 在SelectExecutor中已经处理了ORDER BY子句，这里避免重复添加
            return "";
        }
        return "";
    }
}
