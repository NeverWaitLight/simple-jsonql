package org.waitlight.simple.jsonql.engine.sqlparser;

import org.waitlight.simple.jsonql.metadata.MetadataSource;

import java.util.ArrayList;
import java.util.List;

public class ClauseSqlParser {
    private final List<AbstractClauseSqlParser> executors;
    protected final MetadataSource metadataSource;

    public ClauseSqlParser(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
        this.executors = new ArrayList<>();
        initExecutors();
    }

    private void initExecutors() {
        // 按照 SQL 子句的执行顺序添加执行器
        executors.add(new WhereClauseSqlParser(metadataSource));
        executors.add(new JoinClauseSqlParser(metadataSource));
        executors.add(new OrderByClauseSqlParser(metadataSource));
        executors.add(new LimitClauseSqlParser(metadataSource));
    }

    public void buildClause(Object condition, StringBuilder sql) {
        // 按顺序执行所有子句执行器
        for (AbstractClauseSqlParser executor : executors) {
            executor.process(condition, sql);
        }
    }
}
