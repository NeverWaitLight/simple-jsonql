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

    public List<PersistStatement> convert2SingleLayer(InsertStatement statement) {
        List<FieldStatement> rootFieldStatements = statement.getFields().stream()
                .filter(field -> field.isInvalid() || !field.hasNestedStatement())
                .toList();

        PersistStatement rootStatement = new PersistStatement();
        rootStatement.setAppId(statement.getAppId());
        rootStatement.setFormId(statement.getFormId());
        rootStatement.setEntityId(statement.getEntityId());
        rootStatement.setFields(new ArrayList<>(rootFieldStatements));

        List<PersistStatement> statements = new ArrayList<>();
        statements.add(rootStatement);

        List<PersistStatement> persistStatements = statement.getFields().stream()
                .filter(FieldStatement::isValid)
                .filter(FieldStatement::hasNestedStatement)
                .flatMap(field -> field.getValues().stream())
                .toList();
        statements.addAll(persistStatements);

        return statements;
    }
}