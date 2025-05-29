package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.util.IStringUtil;
import org.waitlight.simple.jsonql.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;

import static org.waitlight.simple.jsonql.util.JDBCTypeUtils.getJDBCType;

public class LocalClassMetadataBuilder extends MetadataBuilder {

    private final MetadataSource metadataSource;

    public LocalClassMetadataBuilder(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    @Override
    public Metadata build() {
        Metadata metadata = new Metadata();
        Set<Class<?>> entityClasses = metadataSource.getEntityClasses();

        if (CollectionUtils.isEmpty(entityClasses)) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }

        for (Class<?> entityClass : entityClasses) {
            if (!entityClass.isAnnotationPresent(Entity.class)) {
                continue;
            }

            final PersistentClass persistentClass = new PersistentClass(entityClass, entityClass.getSimpleName());
            handleTableAnnotation(entityClass, persistentClass);
            for (Field field : entityClass.getDeclaredFields()) {
                Property.Builder propertyBuilder = handlePropertyMapping(field);

                handleOneToManyAnnotation(persistentClass, field, entityClass, propertyBuilder);
                handleManyToOneAnnotation(persistentClass, field, propertyBuilder);
                handleManyToManyAnnotation(persistentClass, field, entityClass, propertyBuilder);

                persistentClass.addProperty(propertyBuilder.build());
            }
            metadata.add(persistentClass);
        }

        return metadata;
    }

    private void handleTableAnnotation(Class<?> entityClass, PersistentClass persistentClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            persistentClass.setTableName(table.name());
        } else {
            persistentClass.setTableName(IStringUtil.camelToSnake(entityClass.getSimpleName()));
        }
    }


    private void handleOneToManyAnnotation(PersistentClass persistentClass,
                                           Field field,
                                           Class<?> entityClass,
                                           Property.Builder propertyBuilder) {
        if (!field.isAnnotationPresent(OneToMany.class)) {
            return;
        }

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        Class<?> targetEntityClass = oneToMany.targetEntity();
        Class<?> targetEntity = (targetEntityClass == void.class)
                ? ReflectionUtils.getGenericType(field)
                : targetEntityClass;

        propertyBuilder.setRelationship(RelationshipType.ONE_TO_MANY)
                .setTargetEntity(targetEntity)
                .setJoinTableName(IStringUtil.camelToSnake(targetEntity.getSimpleName()))
                .setForeignKeyName(IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_id")
                .setMappedBy(oneToMany.mappedBy());

        persistentClass.addRelationProperty(targetEntity, RelationshipType.ONE_TO_MANY);
    }

    private void handleManyToOneAnnotation(PersistentClass persistentClass,
                                           Field field,
                                           Property.Builder propertyBuilder) {
        if (!field.isAnnotationPresent(ManyToOne.class)) {
            return;
        }

        propertyBuilder.setRelationship(RelationshipType.MANY_TO_ONE);
        propertyBuilder.setTargetEntity(field.getClass());
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (Objects.nonNull(joinColumn) && StringUtils.isNotBlank(joinColumn.name())) {
            propertyBuilder.setColumnName(joinColumn.name());
            propertyBuilder.setForeignKeyName(joinColumn.name());
        } else {
            String columnName = IStringUtil.camelToSnake(field.getName()) + "_id";
            propertyBuilder.setColumnName(columnName);
            propertyBuilder.setForeignKeyName(columnName);
        }

        persistentClass.addRelationProperty(field.getType(), RelationshipType.MANY_TO_ONE);
    }

    private void handleManyToManyAnnotation(PersistentClass persistentClass,
                                            Field field,
                                            Class<?> entityClass,
                                            Property.Builder propertyBuilder) {
        if (!field.isAnnotationPresent(ManyToMany.class)) {
            return;
        }

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        propertyBuilder.setRelationship(RelationshipType.MANY_TO_MANY)
                .setTargetEntity(manyToMany.targetEntity());

        if (field.isAnnotationPresent(JoinTable.class)) {
            handleJoinTableAnnotation(field, entityClass, propertyBuilder, manyToMany);
        } else {
            String joinTableName = IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                    + IStringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName());
            propertyBuilder.setJoinTableName(joinTableName);
        }

        persistentClass.addRelationProperty(field.getType(), RelationshipType.MANY_TO_MANY);
    }

    private void handleJoinTableAnnotation(Field field, Class<?> entityClass, Property.Builder propertyBuilder, ManyToMany manyToMany) {
        if (!field.isAnnotationPresent(JoinTable.class)) {
            return;
        }

        JoinTable joinTable = field.getAnnotation(JoinTable.class);
        // 设置关联表名，如果没有指定则使用默认命名规则
        String joinTableName = StringUtils.isBlank(joinTable.name())
                ? IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                + IStringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName())
                : joinTable.name().toLowerCase();
        propertyBuilder.setJoinTableName(joinTableName)
                .setJoinColumns(joinTable.joinColumns())
                .setInverseJoinColumns(joinTable.inverseJoinColumns());
    }

    /**
     * 处理普通字段的属性映射
     *
     * @param field 字段
     * @return 创建的属性对象
     */
    private Property.Builder handlePropertyMapping(Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        String columnName;
        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            columnName = StringUtils.isBlank(column.name())
                    ? IStringUtil.camelToSnake(fieldName)
                    : column.name();
        } else {
            columnName = IStringUtil.camelToSnake(fieldName);
        }

        return new Property.Builder()
                .setFieldName(fieldName)
                .setFieldType(fieldType)
                .setColumnName(columnName)
                .setColumnType(getJDBCType(fieldType));
    }
}
