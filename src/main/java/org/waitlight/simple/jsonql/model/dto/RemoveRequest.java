package org.waitlight.simple.jsonql.model.dto;

import java.util.List;

/**
 * 数据删除请求
 */
public class RemoveRequest {
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 表单ID
     */
    private String formId;
    
    /**
     * 实体ID
     */
    private String entityId;
    
    /**
     * 要删除的数据ID数组
     */
    private List<String> ids;

    public RemoveRequest() {
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
} 