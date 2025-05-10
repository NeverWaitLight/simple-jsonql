package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.util.StringUtil;

import java.lang.reflect.Field;

public class EntityMetadataBuilder {

    public PersistentClass bindEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new UnsupportedOperationException();
        }

        final PersistentClass persistentClass = new PersistentClass();
        persistentClass.setEntityName(entityClass.getSimpleName());
        persistentClass.setEntityClass(entityClass);

        // 处理@Table注解
        handleTableAnnotation(entityClass, persistentClass);

        // 处理实体属性字段
        for (Field field : entityClass.getDeclaredFields()) {
            handleColumnAnnotation(field, persistentClass);
            handleIdAnnotation(field, persistentClass);
            handleOneToManyAnnotation(field, entityClass, persistentClass);
            handleManyToOneAnnotation(field, persistentClass);
            handleManyToManyAnnotation(field, entityClass, persistentClass);
        }

        return persistentClass;
    }

    private void handleTableAnnotation(Class<?> entityClass, PersistentClass persistentClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            persistentClass.setTableName(table.name());
        } else {
            persistentClass.setTableName(StringUtil.camelToSnake(entityClass.getSimpleName()));
        }
    }

    private void handleColumnAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(Column.class)) {
            return;
        }

        Column column = field.getAnnotation(Column.class);
        String columnName = StringUtils.isBlank(column.name()) ? StringUtil.camelToSnake(field.getName())
                : column.name();
        Property prop = new Property(field.getName(), columnName, field.getType().getSimpleName());
        prop.setNullable(column.nullable());
        persistentClass.addProperty(prop);
    }

    private void handleIdAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(Id.class)) {
            return;
        }

        persistentClass.addProperty(new Property(field.getName(), "id", field.getType().getSimpleName()));
    }

    private void handleOneToManyAnnotation(Field field, Class<?> entityClass, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(OneToMany.class)) {
            return;
        }

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        Property property = new Property();
        property.setName(field.getName());
        property.setRelationshipType(RelationshipType.ONE_TO_MANY);
        property.setTargetEntity(oneToMany.targetEntity());
        property.setJoinTableName(StringUtil.camelToSnake(property.getTargetEntity().getSimpleName()));
        property.setForeignKeyName(StringUtil.camelToSnake(entityClass.getSimpleName()) + "_id");
        property.setMappedBy(oneToMany.mappedBy());
        persistentClass.addProperty(property);
    }

    private void handleManyToOneAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(ManyToOne.class)) {
            return;
        }

        Property property = new Property();
        property.setName(field.getName());
        property.setRelationshipType(RelationshipType.MANY_TO_ONE);
        property.setTargetEntity(field.getClass());
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            property.setForeignKeyName(joinColumn.name());
        }
        persistentClass.addProperty(property);
    }

    private void handleManyToManyAnnotation(Field field, Class<?> entityClass, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(ManyToMany.class)) {
            return;
        }

        Property property = new Property();
        property.setName(field.getName());
        property.setRelationshipType(RelationshipType.MANY_TO_MANY);

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        property.setTargetEntity(manyToMany.targetEntity());

        // 处理关联表配置
        if (field.isAnnotationPresent(JoinTable.class)) {
            handleJoinTableAnnotation(field, entityClass, property, manyToMany);
        } else {
            // 如果没有指定 JoinTable，使用默认命名规则
            String joinTableName = StringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                    + StringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName());
            property.setJoinTableName(joinTableName);
        }

        persistentClass.addProperty(property);
    }

    private void handleJoinTableAnnotation(Field field, Class<?> entityClass, Property property,
                                           ManyToMany manyToMany) {
        if (!field.isAnnotationPresent(JoinTable.class)) {
            return;
        }

        JoinTable joinTable = field.getAnnotation(JoinTable.class);
        // 设置关联表名，如果没有指定则使用默认命名规则
        String joinTableName = StringUtils.isBlank(joinTable.name())
                ? StringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                + StringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName())
                : joinTable.name().toLowerCase();
        property.setJoinTableName(joinTableName);
        property.setJoinColumns(joinTable.joinColumns());
        property.setInverseJoinColumns(joinTable.inverseJoinColumns());
    }
}
