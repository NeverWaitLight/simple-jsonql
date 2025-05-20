package org.waitlight.simple.jsonql.statement;

import java.util.List;

public record Statements<T extends JsonQLStatement>(
        List<T> mainStatements,
        List<T> nestedStatements
) {
}
