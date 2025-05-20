package org.waitlight.simple.jsonql.execute.result;

import lombok.Getter;

/**
 * 删除操作执行结果类，包含影响行数信息
 */
@Getter
public class DeleteResult implements ExecuteResult {
    private final int affectedRows;

    private DeleteResult(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public static DeleteResult of(int affectedRows) {
        return new DeleteResult(affectedRows);
    }
}
