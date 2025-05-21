package org.waitlight.simple.jsonql.metadata;

import org.apache.calcite.sql.type.SqlTypeName;
import org.jooq.impl.SQLDataType;

import java.sql.JDBCType;
import java.sql.Types;

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


}
