package org.waitlight.simple.jsonql.engine.sqlparser;

import org.waitlight.simple.jsonql.metadata.MetadataSources;

import java.util.ArrayList;
import java.util.List;

public class ClauseSqlParser {
    private final List<AbstractClauseSqlParser> executors;
    protected final MetadataSources metadataSources;

    public ClauseSqlParser(MetadataSources metadataSources) {
        this.metadataSources = metadataSources;
        this.executors = new ArrayList<>();
        initExecutors();
    }

    private void initExecutors() {
        // 按照 SQL 子句的执行顺序添加执行器
        executors.add(new WhereClauseSqlParser(metadataSources));
        executors.add(new JoinClauseSqlParser(metadataSources));
        executors.add(new OrderByClauseSqlParser(metadataSources));
        executors.add(new LimitClauseSqlParser(metadataSources));
    }

    public void buildClause(Object condition, StringBuilder sql) {
        // 按顺序执行所有子句执行器
        for (AbstractClauseSqlParser executor : executors) {
            executor.process(condition, sql);
        }
    }
}
