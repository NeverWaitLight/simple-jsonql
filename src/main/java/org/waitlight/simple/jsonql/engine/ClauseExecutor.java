package org.waitlight.simple.jsonql.engine;

import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.ArrayList;
import java.util.List;

public class ClauseExecutor {
    private final List<AbstractClauseExecutor> executors;
    protected final MetadataSources metadataSources;

    public ClauseExecutor(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.executors = new ArrayList<>();
        initExecutors();
    }

    private void initExecutors() {
        // 按照 SQL 子句的执行顺序添加执行器
        executors.add(new WhereClauseExecutor(metadataSources));
        executors.add(new JoinClauseExecutor(metadataSources));
        executors.add(new OrderByClauseExecutor(metadataSources));
        executors.add(new LimitClauseExecutor(metadataSources));
    }

    public void buildClause(Object condition, StringBuilder sql) {
        // 按顺序执行所有子句执行器
        for (AbstractClauseExecutor executor : executors) {
            executor.process(condition, sql);
        }
    }
}
