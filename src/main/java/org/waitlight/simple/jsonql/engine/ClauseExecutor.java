package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;

public class ClauseExecutor extends AbstractClauseExecutor {
    private WhereClauseExecutor whereExecutor;
    private JoinClauseExecutor joinExecutor;
    private OrderByClauseExecutor orderByExecutor;
    private LimitClauseExecutor limitExecutor;
    protected final MetadataSources metadataSources;

    public ClauseExecutor(MetadataSources metadataSources) {
        super(metadataSources);
        this.metadataSources = metadataSources;
    }

    private void initChain() {
        if (whereExecutor == null) {
            this.whereExecutor = new WhereClauseExecutor(metadataSources);
            this.joinExecutor = new JoinClauseExecutor(metadataSources);
            this.orderByExecutor = new OrderByClauseExecutor(metadataSources);
            this.limitExecutor = new LimitClauseExecutor(metadataSources);
            
            // 构建责任链
            whereExecutor.setNext(joinExecutor)
                       .setNext(orderByExecutor)
                       .setNext(limitExecutor);
        }
    }

    @Override
    public String buildClause(Object condition) {
        initChain();
        StringBuilder sql = new StringBuilder();
        whereExecutor.process(condition, sql);
        return sql.toString();
    }
}
