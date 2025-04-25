package org.waitlight.simple.jsonql;


import jakarta.persistence.*;

import java.lang.reflect.Field;

public class AnnotationBinder {

    public void bindEntity(Class<?> entityClass, PersistentClass metadata) {
        if (!entityClass.isAnnotationPresent(Entity.class)) return;

        metadata.setEntityName(entityClass.getSimpleName());

        // 2. 处理@Table注解(如果有)
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            metadata.setTableName(table.name());
        } else {
            metadata.setTableName(entityClass.getSimpleName());
        }

        // 3. 处理属性字段
        for (Field field : entityClass.getDeclaredFields()) {
            // 处理@Column注解
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                Property prop = new Property(field.getName(), columnName, field.getType().getSimpleName());
                prop.setNullable(column.nullable());
                metadata.addProperty(prop);
            }

            // 处理@Id注解(简化版)
            if (field.isAnnotationPresent(Id.class)) {
                metadata.addProperty(new Property(field.getName(), "id", field.getType().getSimpleName()));
            }

            // 处理@OneToMany注解
            if (field.isAnnotationPresent(OneToMany.class)) {
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType("OneToMany");
                property.setTargetEntity(oneToMany.targetEntity());
                property.setForeignKeyName(entityClass.getSimpleName().toLowerCase() + "_id");
                property.setMappedBy(oneToMany.mappedBy());
                metadata.addProperty(property);
            }

            // 处理@ManyToOne注解
            if (field.isAnnotationPresent(ManyToOne.class)) {
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType("ManyToOne");
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null) {
                    property.setForeignKeyName(joinColumn.name());
                }
                metadata.addProperty(property);
            }

            // 处理@ManyToMany注解
            if (field.isAnnotationPresent(ManyToMany.class)) {
                Property property = new Property();
                property.setName(field.getName());
                property.setRelationshipType("ManyToMany");

                ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
                property.setTargetEntity(manyToMany.targetEntity());

                // 处理关联表配置
                if (field.isAnnotationPresent(JoinTable.class)) {
                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    property.setJoinTableName(joinTable.name());
                    property.setJoinColumns(joinTable.joinColumns());
                    property.setInverseJoinColumns(joinTable.inverseJoinColumns());
                }

                metadata.addProperty(property);
            }
        }
    }
}
