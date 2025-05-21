package org.waitlight.simple.jsonql.util;

import org.apache.calcite.sql.type.SqlTypeName;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import java.sql.JDBCType;
import java.sql.Types;

public class JDBCTypeUtils {
    /**
     * 将Java类型转换为对应的JDBC类型
     *
     * @param javaType Java类型
     * @return 对应的JDBC类型
     */
    public static JDBCType getJDBCType(Class<?> javaType) {
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

    /**
     * 将Java类型转换为对应的{@link SqlTypeName}
     *
     * @param jdbcType Java类型
     * @return 对应的SQL类型名称
     */
    public static SqlTypeName getSqlTypeName(JDBCType jdbcType) {
        return switch (jdbcType) {
            case VARCHAR, LONGVARCHAR, NVARCHAR, LONGNVARCHAR -> SqlTypeName.VARCHAR;
            case INTEGER -> SqlTypeName.INTEGER; 
            case BIGINT -> SqlTypeName.BIGINT;
            case DOUBLE -> SqlTypeName.DOUBLE;
            case FLOAT, REAL -> SqlTypeName.FLOAT;
            case BOOLEAN, BIT -> SqlTypeName.BOOLEAN;
            case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> SqlTypeName.TIMESTAMP;
            case DATE -> SqlTypeName.DATE;
            case TIME, TIME_WITH_TIMEZONE -> SqlTypeName.TIME;
            case BINARY, VARBINARY, LONGVARBINARY -> SqlTypeName.VARBINARY;
            case SMALLINT -> SqlTypeName.SMALLINT;
            case TINYINT -> SqlTypeName.TINYINT;
            case DECIMAL, NUMERIC -> SqlTypeName.DECIMAL;
            case CHAR, NCHAR -> SqlTypeName.CHAR;
            default -> SqlTypeName.VARCHAR;
        };
    }

    public static DataType<?> getDataType(JDBCType jdbcType) {
        return switch (jdbcType.getVendorTypeNumber()) {
            case Types.BIGINT -> SQLDataType.BIGINT;
            case Types.BINARY -> SQLDataType.BINARY;
            case Types.BIT -> SQLDataType.BIT;
            case Types.BLOB -> SQLDataType.BLOB;
            case Types.BOOLEAN -> SQLDataType.BOOLEAN;
            case Types.CHAR -> SQLDataType.CHAR;
            case Types.CLOB -> SQLDataType.CLOB;
            case Types.DATE -> SQLDataType.DATE;
            case Types.DECIMAL -> SQLDataType.DECIMAL;
            case Types.DOUBLE -> SQLDataType.DOUBLE;
            case Types.FLOAT -> SQLDataType.FLOAT;
            case Types.INTEGER -> SQLDataType.INTEGER;
            case Types.LONGNVARCHAR -> SQLDataType.LONGNVARCHAR;
            case Types.LONGVARBINARY -> SQLDataType.LONGVARBINARY;
            case Types.LONGVARCHAR -> SQLDataType.LONGVARCHAR;
            case Types.NCHAR -> SQLDataType.NCHAR;
            case Types.NCLOB -> SQLDataType.NCLOB;
            case Types.NUMERIC -> SQLDataType.NUMERIC;
            case Types.NVARCHAR -> SQLDataType.NVARCHAR;
            case Types.REAL -> SQLDataType.REAL;
            case Types.REF_CURSOR -> SQLDataType.RESULT;
            case Types.SMALLINT -> SQLDataType.SMALLINT;
            case Types.SQLXML -> SQLDataType.XML;
            case Types.STRUCT -> SQLDataType.RECORD;
            case Types.TIME -> SQLDataType.TIME;
            case Types.TIME_WITH_TIMEZONE -> SQLDataType.TIMEWITHTIMEZONE;
            case Types.TIMESTAMP -> SQLDataType.TIMESTAMP;
            case Types.TIMESTAMP_WITH_TIMEZONE -> SQLDataType.TIMESTAMPWITHTIMEZONE;
            case Types.TINYINT -> SQLDataType.TINYINT;
            case Types.VARBINARY -> SQLDataType.VARBINARY;
            case Types.VARCHAR -> SQLDataType.VARCHAR;
            default -> SQLDataType.OTHER;
        };
    }
}
