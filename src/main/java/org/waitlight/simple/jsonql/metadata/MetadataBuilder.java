package org.waitlight.simple.jsonql.metadata;

import java.sql.JDBCType;

public abstract class MetadataBuilder {
    /**
     * 构建元数据实例
     * <p>
     * 此方法负责从MetadataSource中获取所有实体类，并为每个实体类创建一个PersistentClass实例，
     * 然后将这些实例添加到Metadata对象中返回的Metadata实例包含了所有实体的映射信息，
     * 可用于后续的数据库操作和ORM映射
     *
     * @return {@link Metadata} 元数据实例，包含所有实体的映射信息
     */
    public abstract Metadata build();

    /**
     * 将Java类型转换为对应的JDBC类型
     *
     * @param javaType Java类型
     * @return 对应的JDBC类型
     */
    protected JDBCType getJDBCType(Class<?> javaType) {
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
