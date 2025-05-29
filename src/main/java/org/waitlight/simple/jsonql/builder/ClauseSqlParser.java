package org.waitlight.simple.jsonql.builder;

import org.waitlight.simple.jsonql.metadata.MetadataSource;

import java.util.ArrayList;
import java.util.List;

public class ClauseSqlParser {
    private final List<AbstractClauseSqlBuilder> executors;
    protected final MetadataSource metadataSource;

    public ClauseSqlParser(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
        this.executors = new ArrayList<>();
        initExecutors();
    }

    private void initExecutors() {
        // 按照 SQL 子句的执行顺序添加执行器
        executors.add(new WhereClauseSqlBuilder(metadataSource));
        executors.add(new JoinClauseSqlBuilder(metadataSource));
        executors.add(new OrderByClauseSqlBuilder(metadataSource));
        executors.add(new LimitClauseSqlBuilder(metadataSource));
    }

    public void buildClause(Object condition, StringBuilder sql) {
        // 按顺序执行所有子句执行器
        for (AbstractClauseSqlBuilder executor : executors) {
            executor.process(condition, sql);
        }
    }
}
