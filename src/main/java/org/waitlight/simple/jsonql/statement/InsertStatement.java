package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;
import org.waitlight.simple.jsonql.statement.model.NestedStatement;

import java.util.List;

@Getter
@Setter
public class InsertStatement extends NestedStatement {
    private List<FieldStatement> fields;
}