package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DeleteStatement extends JsonQLStatement {
    private String id; // 单个ID删除
    private List<String> ids; // 保留原有字段以保持兼容性
}