package org.waitlight.simple.jsonql.execute.result;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 查询操作执行结果类，包含查询结果记录和总记录数
 */
@Getter
public class SelectResult implements ExecuteResult {
    private final List<Map<String, Object>> records;
    private final int totalCount;
    private final int pageSize;
    private final int pageNumber;

    private SelectResult(List<Map<String, Object>> records, int totalCount, int pageSize, int pageNumber) {
        this.records = records;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    /**
     * 创建查询结果
     *
     * @param records    查询结果记录
     * @param totalCount 总记录数
     * @param pageSize   每页大小
     * @param pageNumber 当前页码
     * @return 查询结果对象
     */
    public static SelectResult of(List<Map<String, Object>> records, int totalCount, int pageSize, int pageNumber) {
        if (records == null) {
            records = Collections.emptyList();
        }
        return new SelectResult(records, totalCount, pageSize, pageNumber);
    }

    /**
     * 创建包含所有记录的查询结果（无分页）
     *
     * @param records 查询结果记录
     * @return 查询结果对象
     */
    public static SelectResult of(List<Map<String, Object>> records) {
        if (records == null) {
            records = Collections.emptyList();
        }
        return new SelectResult(records, records.size(), records.size(), 1);
    }
}
