package org.waitlight.simple.jsonql.execute.result;

import lombok.Value;

import java.util.List;

/**
 * 插入操作执行结果类，包含影响行数和生成的ID信息
 */
@Value
public class InsertResult implements ExecuteResult {
    int affectedRows;
    List<Long> mainIds;
    List<Long> nestedIds;
}