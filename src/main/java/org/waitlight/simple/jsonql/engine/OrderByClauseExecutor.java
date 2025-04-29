package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.QueryStatement;
import org.waitlight.simple.jsonql.statement.model.Sort;

import java.util.List;

public class OrderByClauseExecutor extends AbstractClauseExecutor {

    public OrderByClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (condition instanceof QueryStatement) {
            QueryStatement select = (QueryStatement) condition;
            List<Sort> sortList = select.getSort();

            if (sortList == null || sortList.isEmpty()) {
                return "";
            }

            // 在SelectExecutor中已经处理了ORDER BY子句，这里避免重复添加
            return "";
        }
        return "";
    }
}
