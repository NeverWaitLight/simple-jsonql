package org.waitlight.simple.jsonql.statement.model;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Data
public class FieldStatement {
    private String field;
    private Object value;
    private List<PersistStatement> values;

    public boolean hasNestedStatement() {
        return isValid() && CollectionUtils.isNotEmpty(values);
    }

    /**
     * value 和 values 同时为空或者同时非空都是 invalid
     */
    public boolean isInvalid() {
        return (Objects.isNull(value) && CollectionUtils.isEmpty(values))
                || (Objects.nonNull(value) && CollectionUtils.isNotEmpty(values));
    }

    public boolean isValid() {
        return !isInvalid();
    }
}