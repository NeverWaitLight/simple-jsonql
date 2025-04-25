package org.waitlight.simple.jsonql.claude;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询构建器，用于构建JPQL查询
 */
public class QueryBuilder<T> {
    private final CustomEntityManager entityManager;
    private final Class<T> entityClass;
    private final List<String> conditions = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();
    private final List<OrderByClause> orderByClauses = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;
    private final List<String> includes = new ArrayList<>(); // 关联查询

    public QueryBuilder(CustomEntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    /**
     * 添加查询条件
     */
    public QueryBuilder<T> where(String condition, Object... params) {
        conditions.add(condition);
        for (Object param : params) {
            parameters.add(param);
        }
        return this;
    }

    /**
     * 添加排序条件
     */
    public QueryBuilder<T> orderBy(String field, String direction) {
        orderByClauses.add(new OrderByClause(field, direction));
        return this;
    }

    /**
     * 设置返回结果数量限制
     */
    public QueryBuilder<T> limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    /**
     * 设置结果集偏移量
     */
    public QueryBuilder<T> offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    /**
     * 添加关联查询
     */
    public QueryBuilder<T> include(String relationProperty) {
        includes.add(relationProperty);
        return this;
    }

    /**
     * 转换为JPQL查询
     */
    public String toJPQL() {
        EntityMetadata metadata = entityManager.getMetadata(entityClass);
        if (metadata == null) {
            throw new IllegalStateException("未找到实体类 " + entityClass.getName() + " 的元数据");
        }

        StringBuilder jpql = new StringBuilder("SELECT e FROM ")
                .append(entityClass.getSimpleName())
                .append(" e");

        // 添加WHERE条件
        if (!conditions.isEmpty()) {
            jpql.append(" WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    jpql.append(" AND ");
                }
                jpql.append(conditions.get(i));
            }
        }

        // 添加ORDER BY
        if (!orderByClauses.isEmpty()) {
            jpql.append(" ORDER BY ");
            for (int i = 0; i < orderByClauses.size(); i++) {
                OrderByClause clause = orderByClauses.get(i);
                if (i > 0) {
                    jpql.append(", ");
                }
                jpql.append("e.").append(clause.getField()).append(" ").append(clause.getDirection());
            }
        }

        return jpql.toString();
    }

    /**
     * 执行查询并返回结果
     */
    public List<T> getResults() {
        return entityManager.executeQuery(this);
    }

    /**
     * 获取单个结果
     */
    public T getSingleResult() {
        List<T> results = getResults();
        return results.isEmpty() ? null : results.get(0);
    }

    // Getters
    public List<Object> getParameters() {
        return parameters;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public Integer getOffsetValue() {
        return offsetValue;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 排序子句
     */
    private static class OrderByClause {
        private final String field;
        private final String direction;

        public OrderByClause(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }

        public String getField() {
            return field;
        }

        public String getDirection() {
            return direction;
        }
    }
}