package org.waitlight.simple.jsonql.metadata;

import jakarta.persistence.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.waitlight.simple.jsonql.util.IStringUtil;

import java.lang.reflect.Field;
import java.sql.JDBCType;
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

            handleTableAnnotation(entityClass, persistentClass);

            for (Field field : entityClass.getDeclaredFields()) {
                Property property = handlePropertyMapping(field);

                handleOneToManyAnnotation(field, entityClass, property);
                handleManyToOneAnnotation(field, property);
                handleManyToManyAnnotation(field, entityClass, property);

                persistentClass.addProperty(property);
            }

            metadata.add(persistentClass);
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


    private void handleOneToManyAnnotation(Field field, Class<?> entityClass, Property property) {
        if (!field.isAnnotationPresent(OneToMany.class)) {
            return;
        }

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        property.setRelationship(RelationshipType.ONE_TO_MANY);
        property.setTargetEntity(oneToMany.targetEntity());
        property.setJoinTableName(IStringUtil.camelToSnake(property.getTargetEntity().getSimpleName()));
        property.setForeignKeyName(IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_id");
        property.setMappedBy(oneToMany.mappedBy());
    }

    private void handleManyToOneAnnotation(Field field, Property property) {
        if (!field.isAnnotationPresent(ManyToOne.class)) {
            return;
        }

        property.setRelationship(RelationshipType.MANY_TO_ONE);
        property.setTargetEntity(field.getClass());
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null) {
            property.setForeignKeyName(joinColumn.name());
        }
    }

    private void handleManyToManyAnnotation(Field field, Class<?> entityClass, Property property) {
        if (!field.isAnnotationPresent(ManyToMany.class)) {
            return;
        }

        property.setRelationship(RelationshipType.MANY_TO_MANY);

        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        property.setTargetEntity(manyToMany.targetEntity());

        if (field.isAnnotationPresent(JoinTable.class)) {
            handleJoinTableAnnotation(field, entityClass, property, manyToMany);
        } else {
            String joinTableName = IStringUtil.camelToSnake(entityClass.getSimpleName()) + "_"
                    + IStringUtil.camelToSnake(manyToMany.targetEntity().getSimpleName());
            property.setJoinTableName(joinTableName);
        }
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

    /**
     * 处理普通字段的属性映射
     *
     * @param field 字段
     * @return 创建的属性对象
     */
    private Property handlePropertyMapping(Field field) {
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

        JDBCType columnType = getJDBCTypeFromJavaType(fieldType);
        return new Property(fieldName, fieldType, columnName, columnType);
    }

    /**
     * 将Java类型转换为对应的JDBC类型
     *
     * @param javaType Java类型
     * @return 对应的JDBC类型
     */
    private JDBCType getJDBCTypeFromJavaType(Class<?> javaType) {
        if (javaType == String.class) {
            return JDBCType.VARCHAR;
        } else if (javaType == Integer.class || javaType == int.class) {
            return JDBCType.INTEGER;
        } else if (javaType == Long.class || javaType == long.class) {
            return JDBCType.BIGINT;
        } else if (javaType == Double.class || javaType == double.class) {
            return JDBCType.DOUBLE;
        } else if (javaType == Float.class || javaType == float.class) {
            return JDBCType.FLOAT;
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return JDBCType.BOOLEAN;
        } else if (javaType == java.util.Date.class) {
            return JDBCType.TIMESTAMP;
        } else if (javaType == java.sql.Date.class) {
            return JDBCType.DATE;
        } else if (javaType == java.sql.Time.class) {
            return JDBCType.TIME;
        } else if (javaType == java.sql.Timestamp.class) {
            return JDBCType.TIMESTAMP;
        } else if (javaType == byte[].class) {
            return JDBCType.VARBINARY;
        } else if (javaType == Short.class || javaType == short.class) {
            return JDBCType.SMALLINT;
        } else if (javaType == Byte.class || javaType == byte.class) {
            return JDBCType.TINYINT;
        } else if (javaType == java.math.BigDecimal.class) {
            return JDBCType.DECIMAL;
        } else if (javaType == Character.class || javaType == char.class) {
            return JDBCType.CHAR;
        } else {
            return JDBCType.VARCHAR;
        }
    }
}
