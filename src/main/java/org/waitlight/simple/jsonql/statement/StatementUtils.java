package org.waitlight.simple.jsonql.statement;

import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.PersistStatement;

import java.util.ArrayList;
import java.util.List;

public class StatementUtils {
    /**
     * 将嵌套的PersistStatement转换为单层PersistStatement
     */
    public static StatementsPairs<PersistStatement> convert2SingleLevel(PersistStatement statement) {
        List<FieldStatement> rootFieldStatements = statement.getFields().stream()
                .filter(field -> field.isInvalid() || !field.hasNestedStatement())
                .toList();

        PersistStatement mainStatement = new PersistStatement();
        mainStatement.setAppId(statement.getAppId());
        mainStatement.setFormId(statement.getFormId());
        mainStatement.setEntityId(statement.getEntityId());
        mainStatement.setDataId(statement.getDataId());
        mainStatement.setFields(new ArrayList<>(rootFieldStatements));

        List<PersistStatement> nestedStatements = statement.getFields().stream()
                .filter(FieldStatement::isValid)
                .filter(FieldStatement::hasNestedStatement)
                .flatMap(field -> field.getValues().stream())
                .toList();

        return new StatementsPairs<>(mainStatement, nestedStatements);
    }
}
