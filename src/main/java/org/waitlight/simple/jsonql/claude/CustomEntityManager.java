package org.waitlight.simple.jsonql.claude;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义实体管理器，处理实体与数据库之间的映射和操作
 */
@Component
public class CustomEntityManager {

    @PersistenceContext
    private EntityManager jpaEntityManager;

    // 存储实体类与表映射信息
    private final Map<Class<?>, EntityMetadata> entityMappings = new ConcurrentHashMap<>();

    /**
     * 注册实体类和其映射信息
     */
    public <T> CustomEntityManager registerEntity(Class<T> entityClass, String tableName, String primaryKey) {
        EntityMetadata metadata = new EntityMetadata(tableName, primaryKey);
        entityMappings.put(entityClass, metadata);
        return this;
    }

    /**
     * 定义列映射
     */
    public <T> CustomEntityManager mapColumn(Class<T> entityClass, String propertyName, String columnName, String dataType) {
        EntityMetadata metadata = entityMappings.get(entityClass);
        if (metadata == null) {
            throw new IllegalArgumentException("未注册的实体类: " + entityClass.getName());
        }

        metadata.getColumns().put(propertyName, new ColumnInfo(columnName, dataType));
        return this;
    }

    /**
     * 定义一对多关系
     */
    public <T, R> CustomEntityManager mapOneToMany(Class<T> entityClass, String propertyName,
                                                   Class<R> targetEntity, String foreignKey) {
        EntityMetadata metadata = entityMappings.get(entityClass);
        if (metadata == null) {
            throw new IllegalArgumentException("未注册的实体类: " + entityClass.getName());
        }

        metadata.getRelationships().put(propertyName, new RelationshipInfo("OneToMany", targetEntity, foreignKey));
        return this;
    }

    /**
     * 定义多对一关系
     */
    public <T, R> CustomEntityManager mapManyToOne(Class<T> entityClass, String propertyName,
                                                   Class<R> targetEntity, String foreignKey) {
        EntityMetadata metadata = entityMappings.get(entityClass);
        if (metadata == null) {
            throw new IllegalArgumentException("未注册的实体类: " + entityClass.getName());
        }

        metadata.getRelationships().put(propertyName, new RelationshipInfo("ManyToOne", targetEntity, foreignKey));
        return this;
    }

    /**
     * 获取实体元数据
     */
    public EntityMetadata getMetadata(Class<?> entityClass) {
        return entityMappings.get(entityClass);
    }

    /**
     * 创建查询构建器
     */
    public <T> QueryBuilder<T> createQuery(Class<T> entityClass) {
        return new QueryBuilder<>(this, entityClass);
    }

    /**
     * 执行JPQL查询并映射结果
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> executeQuery(QueryBuilder<T> queryBuilder) {
        String jpql = queryBuilder.toJPQL();
        var query = jpaEntityManager.createQuery(jpql);

        // 设置参数
        for (int i = 0; i < queryBuilder.getParameters().size(); i++) {
            query.setParameter(i + 1, queryBuilder.getParameters().get(i));
        }

        // 设置分页
        if (queryBuilder.getLimitValue() != null) {
            query.setMaxResults(queryBuilder.getLimitValue());
        }

        if (queryBuilder.getOffsetValue() != null) {
            query.setFirstResult(queryBuilder.getOffsetValue());
        }

        List<T> results = query.getResultList();

        // 处理关联数据
        if (!queryBuilder.getIncludes().isEmpty()) {
            loadRelationships(results, queryBuilder.getIncludes());
        }

        return results;
    }

    /**
     * 加载关联实体
     */
    private <T> void loadRelationships(List<T> entities, List<String> includes) {
        if (entities.isEmpty()) {
            return;
        }

        Class<?> entityClass = entities.get(0).getClass();
        EntityMetadata metadata = getMetadata(entityClass);

        for (String relationName : includes) {
            RelationshipInfo relationInfo = metadata.getRelationships().get(relationName);
            if (relationInfo == null) {
                continue;
            }

            try {
                Field relationField = entityClass.getDeclaredField(relationName);
                relationField.setAccessible(true);

                if ("OneToMany".equals(relationInfo.getType())) {
                    loadOneToManyRelationship(entities, relationField, relationInfo);
                } else if ("ManyToOne".equals(relationInfo.getType())) {
                    loadManyToOneRelationship(entities, relationField, relationInfo);
                }
            } catch (Exception e) {
                throw new RuntimeException("加载关联数据时出错", e);
            }
        }
    }

    /**
     * 加载一对多关系
     */
    private <T> void loadOneToManyRelationship(List<T> entities, Field relationField, RelationshipInfo relationInfo) {
        // 实现一对多关系的加载
        // 这里需要根据外键查询关联实体并设置到主实体中
    }

    /**
     * 加载多对一关系
     */
    private <T> void loadManyToOneRelationship(List<T> entities, Field relationField, RelationshipInfo relationInfo) {
        // 实现多对一关系的加载
        // 这里需要根据外键查询关联实体并设置到主实体中
    }

    /**
     * 保存实体
     */
    @Transactional
    public <T> void persist(T entity) {
        jpaEntityManager.persist(entity);
    }

    /**
     * 根据ID查找实体
     */
    public <T> T find(Class<T> entityClass, Object id) {
        return jpaEntityManager.find(entityClass, id);
    }

    /**
     * 更新实体
     */
    @Transactional
    public <T> T merge(T entity) {
        return jpaEntityManager.merge(entity);
    }

    /**
     * 删除实体
     */
    @Transactional
    public void remove(Object entity) {
        jpaEntityManager.remove(entity);
    }

    /**
     * 执行原生SQL查询
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> executeNativeQuery(String sql, Class<T> resultClass, Object... parameters) {
        var query = jpaEntityManager.createNativeQuery(sql, resultClass);

        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i + 1, parameters[i]);
        }

        return query.getResultList();
    }
}