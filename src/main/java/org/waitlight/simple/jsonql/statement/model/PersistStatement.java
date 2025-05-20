package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.util.List;

/**
 * 持久化语句 （INSERT、UPDATE）
 */
@Getter
@Setter
public class PersistStatement extends JsonQLStatement {
    private String dataId;
    private List<FieldStatement> fields;
}