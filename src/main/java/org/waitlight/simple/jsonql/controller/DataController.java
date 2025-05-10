package org.waitlight.simple.jsonql.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.waitlight.simple.jsonql.model.dto.*;
import org.waitlight.simple.jsonql.service.DataService;

import java.util.Map;

/**
 * 数据操作API控制器
 * 提供创建、删除、修改、查询接口
 */
@RestController
@RequestMapping("/api/v1/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * 创建数据
     *
     * @param createRequest 创建请求参数
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody CreateRequest createRequest) {
        Map<String, Object> data = dataService.create(createRequest);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 删除数据
     *
     * @param removeRequest 删除请求参数
     * @return 删除结果
     */
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<RemoveResponse>> remove(@RequestBody RemoveRequest removeRequest) {
        int count = dataService.remove(removeRequest);
        RemoveResponse response = new RemoveResponse(count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新数据
     *
     * @param updateRequest 更新请求参数
     * @return 更新结果
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@RequestBody UpdateRequest updateRequest) {
        Map<String, Object> data = dataService.update(updateRequest);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 分页查询数据
     *
     * @param pageRequest 查询请求参数
     * @return 查询结果
     */
    @PostMapping("/page")
    public ResponseEntity<PageResponse<Map<String, Object>>> page(@RequestBody PageRequest pageRequest) {
        PageResult<Map<String, Object>> pageResult = dataService.page(pageRequest);

        PageResponse<Map<String, Object>> response = new PageResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(pageResult.getData());
        response.setTotal(pageResult.getTotal());
        response.setPage(pageResult.getPage());
        response.setSize(pageResult.getSize());
        response.setTotalPages(pageResult.getTotalPages());

        return ResponseEntity.ok(response);
    }
} 