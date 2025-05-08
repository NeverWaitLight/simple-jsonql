package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

public class AnnotationBinder {

    public PersistentClass bindEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class))
            throw new UnsupportedOperationException();

        final PersistentClass persistentClass = new PersistentClass();
        persistentClass.setEntityName(entityClass.getSimpleName().toLowerCase());

        // 2. 处理@Table注解(如果有)
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            persistentClass.setTableName(table.name().toLowerCase());
        } else {
            persistentClass.setTableName(entityClass.getSimpleName().toLowerCase());
        }

        // 3. 处理属性字段
        for (Field field : entityClass.getDeclaredFields()) {
            // 处理@Column注解
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String columnName = StringUtils.isBlank(column.name()) ? field.getName() : column.name();
                Property prop = new Property(field.getName(), columnName, field.getType().getSimpleName());
                prop.setNullable(column.nullable());
                persistentClass.addProperty(prop);
            }

            // 处理@Id注解(简化版)
            if (field.isAnnotationPresent(Id.class)) {
                persistentClass.addProperty(new Property(field.getName(), "id", field.getType().getSimpleName()));
            }

            // 处理@OneToMany注解
            if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType(RelationshipType.ONE_TO_MANY);
                property.setTargetEntity(oneToMany.targetEntity());
                property.setJoinTableName(property.getTargetEntity().getSimpleName().toLowerCase());
                property.setForeignKeyName(entityClass.getSimpleName().toLowerCase() + "_id");
                property.setMappedBy(oneToMany.mappedBy());
                persistentClass.addProperty(property);
            }

            // 处理@ManyToOne注解
            if (field.isAnnotationPresent(ManyToOne.class)) {
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType(RelationshipType.MANY_TO_ONE);
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null) {
                    property.setForeignKeyName(joinColumn.name());
                }
                persistentClass.addProperty(property);
            }

            // 处理@ManyToMany注解
            if (field.isAnnotationPresent(ManyToMany.class)) {
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType(RelationshipType.MANY_TO_MANY);

                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                property.setTargetEntity(manyToMany.targetEntity());

                // 处理关联表配置
                if (field.isAnnotationPresent(JoinTable.class)) {
                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    // 设置关联表名，如果没有指定则使用默认命名规则
                    String joinTableName = StringUtils.isBlank(joinTable.name())
                            ? entityClass.getSimpleName().toLowerCase() + "_" + manyToMany.targetEntity().getSimpleName().toLowerCase()
                            : joinTable.name().toLowerCase();
                    property.setJoinTableName(joinTableName);
                    property.setJoinColumns(joinTable.joinColumns());
                    property.setInverseJoinColumns(joinTable.inverseJoinColumns());
                } else {
                    // 如果没有指定 JoinTable，使用默认命名规则
                    String joinTableName = entityClass.getSimpleName().toLowerCase() + "_" + manyToMany.targetEntity().getSimpleName().toLowerCase();
                    property.setJoinTableName(joinTableName);
                }

                persistentClass.addProperty(property);
            }
        }

        return persistentClass;
    }
}
