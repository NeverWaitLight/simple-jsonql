package org.waitlight.simple.jsonql.builder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waitlight.simple.jsonql.metadata.Metadata;
import org.waitlight.simple.jsonql.metadata.MetadataException;
import org.waitlight.simple.jsonql.metadata.Property;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPersistSqlBuilder<T extends PersistStatement> extends AbstractSqlBuilder<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractPersistSqlBuilder.class);

    public AbstractPersistSqlBuilder(Metadata metadata) {
        super(metadata);
    }

    /**
     * 根据实体元数据和语句内容，生成SQL语句
     * <p>
     * 处理流程：
     * 1. 获取实体元数据，检查是否有关系字段
     * 2. 如果没有关系字段，直接构建简单SQL返回
     * 3. 提取非嵌套的基本字段，构造主实体语句
     * 4. 按关系类型处理嵌套实体
     * 5. 生成并返回最终SQL，包含主实体和关联实体的SQL
     * <p>
     * 支持的关系类型：
     * - 单一实体创建
     * - 一对多关系（如User->Blog）
     * - 多对一关系（如Blog->User）
     *
     * @param statement 待解析的CreateStatement语句
     * @return 包含SQL和参数的PreparedSql对象
     * @throws SqlBuildException 如果解析过程中发生错误，将抛出异常
     */
    public abstract PreparedSql<T> build(T statement) throws SqlBuildException;

    @Override
    protected Map<FieldStatement, Property> map(String entityName, PersistStatement statement) {
        Map<FieldStatement, Property> result = new HashMap<>();
        List<Property> properties = metadata.getEntity(entityName).getProperties();
        List<FieldStatement> fieldStatements = statement.getFields().stream().filter(FieldStatement::isValid).toList();
        for (FieldStatement fieldStatement : fieldStatements) {
            Property property = properties.stream()
                    .filter(prop -> StringUtils.equals(prop.fieldName(), fieldStatement.getField()))
                    .findFirst()
                    .orElseThrow(() -> new MetadataException("Could not find metadata definition for field: " + fieldStatement.getField()));
            result.put(fieldStatement, property);
        }
        return result;
    }

}