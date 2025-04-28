package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;

public class ClauseExecutor extends AbstractClauseExecutor {
    private final WhereClauseExecutor whereClauseExecutor;
    private final JoinClauseExecutor joinClauseExecutor;
    private final OrderByClauseExecutor orderByClauseExecutor;

    public ClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.whereClauseExecutor = new WhereClauseExecutor(metadataSources);
        this.joinClauseExecutor = new JoinClauseExecutor(metadataSources);
        this.orderByClauseExecutor = new OrderByClauseExecutor(metadataSources);

        // 构建责任链
        whereClauseExecutor.setNext(joinClauseExecutor)
                .setNext(orderByClauseExecutor);
    }

    @Override
    public String buildClause(Object condition) {
        StringBuilder sql = new StringBuilder();
        whereClauseExecutor.process(condition, sql);
        return sql.toString();
    }
}
