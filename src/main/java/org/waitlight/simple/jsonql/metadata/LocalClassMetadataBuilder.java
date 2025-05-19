package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.waitlight.simple.jsonql.util.IStringUtil;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;

public class LocalClassMetadataBuilder implements MetadataBuilder {

    private final MetadataSource metadataSource;

    public LocalClassMetadataBuilder(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    /**
     * 构建元数据实例
     * <p>
     * 此方法负责从MetadataSource中获取所有实体类，并为每个实体类创建一个PersistentClass实例，
     * 然后将这些实例添加到Metadata对象中返回的Metadata实例包含了所有实体的映射信息，
     * 可用于后续的数据库操作和ORM映射
     *
     * @return {@link Metadata} 元数据实例，包含所有实体的映射信息
     */
    @Override
    public Metadata build() {
        Metadata metadata = new Metadata();
        mapClassToMetadata(metadata, metadataSource.getEntityClasses());
        return metadata;
    }


    /**
     * 映射Java类到PersistentClass
     */
    private void mapClassToMetadata(Metadata metadata, Collection<Class<?>> entityClasses) {
        if (Objects.isNull(metadata)) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (CollectionUtils.isEmpty(entityClasses)) {
            return;
        }


        for (Class<?> entityClass : entityClasses) {
            if (!entityClass.isAnnotationPresent(Entity.class)) {
                continue;
            }

            final PersistentClass persistentClass = new PersistentClass(entityClass, entityClass.getSimpleName());

            // 处理@Table注解
            handleTableAnnotation(entityClass, persistentClass);

            // 处理实体属性字段
            for (Field field : entityClass.getDeclaredFields()) {
                handleIdAnnotation(field, persistentClass);
                handleColumnAnnotation(field, persistentClass);
                handleOneToManyAnnotation(field, entityClass, persistentClass);
                handleManyToOneAnnotation(field, persistentClass);
                handleManyToManyAnnotation(field, entityClass, persistentClass);
            }

            metadata.addEntity(entityClass.getSimpleName().toLowerCase(), persistentClass);
        }
    }

    private void handleTableAnnotation(Class<?> entityClass, PersistentClass persistentClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            persistentClass.setTableName(table.name());
        } else {
            persistentClass.setTableName(IStringUtil.camelToSnake(entityClass.getSimpleName()));
        }
    }

    private void handleIdAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(Id.class)) {
            return;
        }

        if (field.isAnnotationPresent(Column.class)) {
            Column column = field.getAnnotation(Column.class);
            String columnName = StringUtils.isBlank(column.name())
                    ? IStringUtil.camelToSnake(field.getName())
                    : column.name();
            Property prop = new Property(field.getName(), columnName, field.getType().getSimpleName());
            prop.setNullable(false);
            persistentClass.addProperty(prop);
        } else {
            Property prop = new Property(field.getName(), CaseUtils.toCamelCase(field.getName(), false, '_'), field.getType().getSimpleName());
            prop.setNullable(false);
            persistentClass.addProperty(prop);
        }
    }

    private void handleColumnAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(Column.class)) {
            return;
        }

        Column column = field.getAnnotation(Column.class);
        String columnName = StringUtils.isBlank(column.name())
                ? IStringUtil.camelToSnake(field.getName())
                : column.name();
        Property prop = new Property(field.getName(), columnName, field.getType().getSimpleName());
        prop.setNullable(column.nullable());
        persistentClass.addProperty(prop);
    }


    private void handleOneToManyAnnotation(Field field, Class<?> entityClass, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(OneToMany.class)) {
            return;
        }

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        Property property = new Property();
        property.setFieldName(field.getName());
        property.setRelationship(RelationshipType.ONE_TO_MANY);
        property.setTargetEntity(oneToMany.targetEntity());
        property.setJoinTableName(IStringUtil.camelToSnake(property.getTargetEntity().getSimpleName()));
        property.setForeignKeyName(IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_id");
        property.setMappedBy(oneToMany.mappedBy());
        persistentClass.addProperty(property);
    }

    private void handleManyToOneAnnotation(Field field, PersistentClass persistentClass) {
        if (!field.isAnnotationPresent(ManyToOne.class)) {
            return;
        }

        Property property = new Property();
        property.setFieldName(field.getName());
        property.setRelationship(RelationshipType.MANY_TO_ONE);
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
        property.setFieldName(field.getName());
        property.setRelationship(RelationshipType.MANY_TO_MANY);

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        property.setTargetEntity(manyToMany.targetEntity());

        // 处理关联表配置
        if (field.isAnnotationPresent(JoinTable.class)) {
            handleJoinTableAnnotation(field, entityClass, property, manyToMany);
        } else {
            // 如果没有指定 JoinTable，使用默认命名规则
            String joinTableName = IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                    + IStringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName());
            property.setJoinTableName(joinTableName);
        }

        persistentClass.addProperty(property);
    }

    private void handleJoinTableAnnotation(Field field, Class<?> entityClass, Property property, ManyToMany manyToMany) {
        if (!field.isAnnotationPresent(JoinTable.class)) {
            return;
        }

        JoinTable joinTable = field.getAnnotation(JoinTable.class);
        // 设置关联表名，如果没有指定则使用默认命名规则
        String joinTableName = StringUtils.isBlank(joinTable.name())
                ? IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                + IStringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName())
                : joinTable.name().toLowerCase();
        property.setJoinTableName(joinTableName);
        property.setJoinColumns(joinTable.joinColumns());
        property.setInverseJoinColumns(joinTable.inverseJoinColumns());
    }
}
