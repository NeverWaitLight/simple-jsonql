package org.waitlight.simple.jsonql.service;

import org.waitlight.simple.jsonql.model.dto.CreateRequest;
import org.waitlight.simple.jsonql.model.dto.PageRequest;
import org.waitlight.simple.jsonql.model.dto.PageResult;
import org.waitlight.simple.jsonql.model.dto.RemoveRequest;
import org.waitlight.simple.jsonql.model.dto.UpdateRequest;

import java.util.Map;

/**
 * 数据操作服务接口
 */
public interface DataService {

    /**
     * 创建数据
     *
     * @param request 创建请求
     * @return 创建的数据
     */
    Map<String, Object> create(CreateRequest request);

    /**
     * 删除数据
     *
     * @param request 删除请求
     * @return 删除的记录数
     */
    int remove(RemoveRequest request);

    /**
     * 更新数据
     *
     * @param request 更新请求
     * @return 更新后的数据
     */
    Map<String, Object> update(UpdateRequest request);

    /**
     * 分页查询数据
     *
     * @param request 查询请求
     * @return 查询结果
     */
    PageResult<Map<String, Object>> page(PageRequest request);
} 