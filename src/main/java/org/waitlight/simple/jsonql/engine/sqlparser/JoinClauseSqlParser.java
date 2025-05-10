package org.waitlight.simple.jsonql.engine.sqlparser;

import org.waitlight.simple.jsonql.metadata.MetadataSources;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.Filter;

public class JoinClauseSqlParser extends AbstractClauseSqlParser {

    public JoinClauseSqlParser(MetadataSources metadataSources) {
        super(metadataSources);
    }

    @Override
    public String buildClause(Object condition) {
        if (!(condition instanceof SelectStatement select)) return "";

        // 新版本的QueryStatement没有joins属性，我们需要从filters中获取关联信息
        Filter filters = select.getFilters();
        if (filters == null) return "";

        // 如果filters中指定了rel属性，我们可以创建一个JOIN子句
        String rel = filters.getRel();
        if (rel == null || rel.isEmpty()) return "";

        // 构建简单JOIN
        StringBuilder sb = new StringBuilder();
        sb.append(" LEFT JOIN ").append(rel).append(" ON ");

        // 假设我们使用entityId作为主表，rel作为关联表，并且它们有一个共同的关联字段
        String entityId = select.getEntityId();
        if (entityId == null) return "";

        // 根据常见约定构建JOIN条件（这里简化处理，实际可能需要更复杂的逻辑）
        sb.append(entityId).append(".id = ").append(rel).append(".").append(entityId).append("_id");

        return sb.toString();
    }
}
