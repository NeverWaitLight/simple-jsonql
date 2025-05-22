package org.waitlight.simple.jsonql.statement;

import java.util.List;

public record StatementsPairs<T extends JsonQLStatement>(
        T mainStatement,
        List<T> nestedStatements
) {
}
