package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class JsonQLStatement {
    private String appId;
    private String formId;
    private String entityId;

    /**
     * 将嵌套的PersistStatement转换为单层PersistStatement
     */
    public static StatementsPairs<PersistStatement> convert(PersistStatement statement) {
        List<FieldStatement> rootFieldStatements = statement.getFields().stream()
                .filter(field -> field.isInvalid() || !field.hasNestedStatement())
                .toList();

        PersistStatement mainStatement = new PersistStatement();
        mainStatement.setAppId(statement.getAppId());
        mainStatement.setFormId(statement.getFormId());
        mainStatement.setEntityId(statement.getEntityId());
        mainStatement.setFields(new ArrayList<>(rootFieldStatements));


        List<PersistStatement> nestedStatements = statement.getFields().stream()
                .filter(FieldStatement::isValid)
                .filter(FieldStatement::hasNestedStatement)
                .flatMap(field -> field.getValues().stream())
                .toList();

        return new StatementsPairs<>(mainStatement, nestedStatements);
    }
}