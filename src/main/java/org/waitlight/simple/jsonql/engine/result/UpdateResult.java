package org.waitlight.simple.jsonql.engine.result;

import lombok.Getter;

/**
 * 更新操作执行结果类，包含影响行数信息
 */
@Getter
public class UpdateResult implements ExecuteResult {
    private final int affectedRows;
    private final int mainAffectedRows;
    private final int nestedAffectedRows;

    private UpdateResult(int affectedRows, int mainAffectedRows, int nestedAffectedRows) {
        this.affectedRows = affectedRows;
        this.mainAffectedRows = mainAffectedRows;
        this.nestedAffectedRows = nestedAffectedRows;
    }

    /**
     * 创建更新结果对象
     *
     * @param affectedRows 总影响行数
     * @return 更新结果对象
     */
    public static UpdateResult of(int affectedRows) {
        return new UpdateResult(affectedRows, affectedRows, 0);
    }

    /**
     * 创建包含主表和嵌套表更新信息的结果对象
     *
     * @param mainAffectedRows   主表影响行数
     * @param nestedAffectedRows 嵌套表影响行数
     * @return 更新结果对象
     */
    public static UpdateResult of(int mainAffectedRows, int nestedAffectedRows) {
        return new UpdateResult(mainAffectedRows + nestedAffectedRows, mainAffectedRows, nestedAffectedRows);
    }
}
