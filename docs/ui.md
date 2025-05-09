> [从实现原理看低代码前端框架-github.io](https://wulucxy.github.io/blog/principle-of-lowcode/)

# 公用配置

公用配置为所有组件公用，在实际需求上做适当删减

## 状态

```json
{
  "visible": true,
  "visibleOn": "${return input-text}",
  "hidden": true,
  "hiddenOn": "${return input-text}",
  "clearValueOnHidden": true,
  "static": false,
  "staticOn": "${return input-text}",
  "readOnly": true,
  "readOnlyOn": "${return input-text}",
  "disabled": false,
  "disabledOn": "${return input-text}"
}
```

| 字段名             | 值示例       | 含义                                                                                                                 |
| ------------------ | ------------ | -------------------------------------------------------------------------------------------------------------------- |
| visible            | true / false | 静态显隐控制：直接控制组件是否显示（优先级最低）。若为 false，无论其他条件如何，组件均隐藏。                         |
| visibleOn          | JS 脚本      | 动态显隐条件：通过表达式计算结果决定是否显示。当表达式返回 true 时显示组件（优先级高于 visible）。                   |
| hidden             | true / false | 静态显隐反向控制：与 visible 互斥。若为 true，直接隐藏组件（优先级低于 hiddenOn）。                                  |
| hiddenOn           | JS 脚本      | 动态隐藏条件：表达式返回 true 时隐藏组件（优先级高于 hidden）。若同时设置 visibleOn 和 hiddenOn，以 visibleOn 优先。 |
| clearValueOnHidden | true         | 隐藏时清空值：组件隐藏时自动清空其绑定的数据值，常用于表单字段联动场景。                                             |
| static             | false        | 静态模式开关：当为 true 时，组件渲染为纯静态内容（如不可编辑的文本），禁用交互行为。                                 |
| staticOn           | JS 脚本      | 动态静态模式条件：表达式返回 true 时启用静态模式。                                                                   |
| readOnly           | true         | 只读状态：组件内容不可编辑但可复制（如输入框显示为灰色边框），适用于查看模式。                                       |
| readOnlyOn         | JS 脚本      | 动态只读条件：表达式返回 true 时启用只读模式。                                                                       |
| disabled           | false        | 禁用状态：组件完全不可交互（如按钮变灰且无法点击），优先级高于 readOnly。                                            |
| disabledOn         | JS 脚本      | 动态禁用条件：表达式返回 true 时禁用组件。                                                                           |

## 校验

```json
{
  "required": true,
  "validations": {
    "maxLength": 50,
    "minLength": 10
  },
  "validationErrors": {
    "maxLength": "超出最大限制",
    "minLength": "超出最小限制"
  },
  "validateApi": {
    "url": "https://demo.com/check",
    "method": "get"
  },
  "validateOnChange": true
}
```

| 字段名                     | 值                         | 含义                     |
| -------------------------- | -------------------------- | ------------------------ |
| required                   | true                       | 是否必填字段             |
| validations.maxLength      | 50                         | 最大长度限制（50 字符）  |
| validations.minLength      | 10                         | 最小长度限制（10 字符）  |
| validationErrors.maxLength | "超出最大限制"             | 超过最大长度时的错误提示 |
| validationErrors.minLength | "超出最小限制"             | 低于最小长度时的错误提示 |
| validateApi.url            | "<https://demo.com/check>" | 校验接口地址             |
| validateApi.method         | "get"                      | 接口请求方法             |
| validateOnChange           | true                       | 值变化时实时触发校验     |

## 组件样式

```json
{
  "size": "lg",
  "labelAlign": "inherit",
  "themeCss": {
    "inputControlClassName": {
      "radius:default": {
        "top-left-border-radius": "var(--borders-radius-3)",
        "top-right-border-radius": "var(--borders-radius-3)",
        "bottom-left-border-radius": "var(--borders-radius-3)",
        "bottom-right-border-radius": "var(--borders-radius-3)"
      }
    }
  }
}
```

| 字段名     | 值      | 含义              |
| ---------- | ------- | ----------------- |
| size       | lg      | 输入框大          |
| labelAlign | inherit | 标题样式          |
| themeCss   | JsonObj | 组件样式 css 对象 |

## 组件事件

```json
{
  "onEvent": {
    "formItemValidateError": {
      "weight": 0,
      "actions": [
        {
          "ignoreError": false,
          "actionType": "toast",
          "args": {
            "msgType": "error",
            "position": "top-right",
            "closeButton": true,
            "showIcon": true,
            "msg": "文本输入框校验失败",
            "className": "theme-toast-action-scope"
          }
        }
      ]
    }
  }
}
```

| 字段名                                   | 值                | 含义                  |
| ---------------------------------------- | ----------------- | --------------------- |
| onEvent.change.weight                    | 0                 | 变更事件权重          |
| onEvent.change.actions[0].ignoreError    | false             | 动作配置-是否忽略错误 |
| onEvent.change.actions[0].outputVar      | responseResult    | 动作配置-输出变量名   |
| onEvent.change.actions[0].actionType     | ajax              | 动作类型为 AJAX 请求  |
| onEvent.change.actions[0].options.silent | true              | 动作选项-静默模式     |
| onEvent.change.actions[0].api.url        | <http://test.com> | API 请求地址          |
| onEvent.change.actions[0].api.method     | get               | API 请求方法          |

amis 中事件需要和 action 绑定，amis 支持 action 行为类型：

- ui
  - drawer: 抽屉
  - toast: 消息弹窗
  - dialog: 弹窗
- 动作
  - submit
  - clear
  - search
  - 下载
  - 跳转链接
- 网络
  - ajax
- 表单
  - 校验
  - 提交
  - 重置

# Input-text

```json
{
  "type": "input-text",
  "label": "文本输入框",
  "name": "input-text",
  "id": "u:05b770bbad2d",
  "value": "",
  "clearable": true,
  "showCounter": true,
  "maxLength": 100,
  "placeholder": "placeholder",
  "description": "文本输入框描述",
  "addOn": {
    "label": "校验",
    "type": "button",
    "icon": "fa fa-snapchat-ghost",
    "id": "u:3b3f695044b0"
  }
}
```

| 字段名      | 值               | 含义                   |
| ----------- | ---------------- | ---------------------- |
| type        | "input-text"     | 组件类型（文本输入框） |
| label       | "文本输入框"     | 输入框标签名称         |
| name        | "input-text"     | 表单提交的字段名       |
| id          | "u:05b770bbad2d" | 组件唯一标识符         |
| value       | ""               | 输入框默认值           |
| clearable   | true             | 是否显示清除按钮       |
| showCounter | true             | 是否显示字符计数器     |
| maxLength   | 100              | 最大允许输入字符数     |
| placeholder | "placeholder"    | 输入框占位提示文本     |
| description | "文本输入框描述" | 输入框下方辅助描述文本 |

## addOn

| 字段名      | 值                     | 含义               |
| ----------- | ---------------------- | ------------------ |
| addOn.label | "校验"                 | 附加按钮的标签文本 |
| addOn.type  | "button"               | 附加按钮类型       |
| addOn.icon  | "fa fa-snapchat-ghost" | 附加按钮图标       |
| addOn.id    | "u:3b3f695044b0"       | 附加按钮唯一标识符 |

# 下拉框

```json
{
  "type": "select",
  "label": "选项",
  "name": "select",
  "id": "u:1020ebc55d73",
  "multiple": false,
  "clearable": false,
  "source": "https://demo.com/options",
  "labelField": "article.name",
  "valueField": "article.id",
  "selectFirst": true,
  "loadingConfig": {
    "show": true
  }
}
```

| 字段名             | 值                           | 含义                         |
| ------------------ | ---------------------------- | ---------------------------- |
| type               | "select"                     | 组件类型（下拉选择框）       |
| label              | "选项"                       | 下拉框标签名称               |
| name               | "select"                     | 表单提交字段名               |
| id                 | "u:1020ebc55d73"             | 组件唯一标识符               |
| multiple           | false                        | 是否允许多选                 |
| clearable          | false                        | 是否显示清除选中项按钮       |
| source             | "<https://demo.com/options>" | 动态选项数据源接口地址       |
| labelField         | 自定义数据字段               | 选项显示文本字段路径         |
| valueField         | 自定义数据字段               | 选项值字段路径               |
| selectFirst        | true                         | 是否默认选中数据源第一个选项 |
| loadingConfig.show | true                         | 是否显示数据加载状态指示器   |

# 日期选择框

```json
{
  "type": "input-datetime",
  "label": "日期",
  "name": "date",
  "id": "u:276301685dfe",
  "placeholder": "请选择日期以及时间",
  "valueFormat": "YYYY-MM-DD HH:mm:ss",
  "displayFormat": "YYYY-MM-DD HH:mm:ss",
  "minDate": "2025-01-01 09:47:44",
  "maxDate": "2025-12-01 09:48:08",
  "value": "${DATE()}",
  "utc": false
}
```

| 字段名        | 值                    | 含义                               |
| ------------- | --------------------- | ---------------------------------- |
| type          | "input-datetime"      | 组件类型（日期时间选择器）         |
| label         | "日期"                | 日期选择器标签名称                 |
| name          | "date"                | 表单提交字段名                     |
| id            | "u:276301685dfe"      | 组件唯一标识符                     |
| placeholder   | "请选择日期以及时间"  | 输入框占位提示文本                 |
| valueFormat   | "YYYY-MM-DD HH:mm:ss" | 存储值的日期格式（提交到后端格式） |
| displayFormat | "YYYY-MM-DD HH:mm:ss" | 界面显示的日期格式                 |
| minDate       | "2025-01-01 09:47:44" | 允许选择的最小日期（含时间）       |
| maxDate       | "2025-12-01 09:48:08" | 允许选择的最大日期（含时间）       |
| value         | 自定义脚本            | 默认值（示例为动态获取当前日期）   |
| utc           | false                 | 是否将日期时间转换为 UTC 格式      |

# 复选框

```json
{
  "type": "checkboxes",
  "label": "复选框",
  "name": "checkboxes",
  "multiple": true,
  "options": [
    {
      "label": "选项A",
      "value": "A"
    },
    {
      "label": "选项B",
      "value": "B"
    }
  ],
  "id": "u:f9fc06722afc",
  "checkAll": false,
  "joinValues": true,
  "delimiter": ",",
  "defaultCheckAll": false
}
```

| 字段名           | 值               | 含义                                 |
| ---------------- | ---------------- | ------------------------------------ |
| type             | "checkboxes"     | 组件类型（复选框组）                 |
| label            | "复选框"         | 复选框组标签名称                     |
| name             | "checkboxes"     | 表单提交字段名                       |
| multiple         | true             | 是否允许多选（默认 true，可省略）    |
| options[0].label | "选项 A"         | 第一个选项的显示文本                 |
| options[0].value | "A"              | 第一个选项的提交值                   |
| options[1].label | "选项 B"         | 第二个选项的显示文本                 |
| options[1].value | "B"              | 第二个选项的提交值                   |
| id               | "u:f9fc06722afc" | 组件唯一标识符                       |
| checkAll         | false            | 是否显示"全选"按钮                   |
| joinValues       | true             | 是否将选中值用分隔符合并成字符串提交 |
| delimiter        | ","              | 多选值拼接时的分隔符                 |
| defaultCheckAll  | false            | 是否默认全选所有选项                 |

`options` 数据项可以替换为[下拉框](#下拉框)的 `source` 配置

# 图片上传

```json
{
  "type": "input-image",
  "label": "图片上传",
  "name": "image",
  "autoUpload": true,
  "proxy": true,
  "uploadType": "fileReceptor",
  "imageClassName": "r w-full",
  "id": "u:c13985bb119e",
  "accept": ".jpeg, .jpg, .png, .gif",
  "multiple": false,
  "hideUploadButton": false,
  "bos": "default",
  "receiver": {
    "url": "https://demo.com/upload",
    "method": "get"
  },
  "limit": false,
  "crop": false
}
```

| 字段名           | 值                          | 含义                                      |
| ---------------- | --------------------------- | ----------------------------------------- |
| type             | "input-image"               | 组件类型（图片上传）                      |
| label            | "图片上传"                  | 上传组件标签名称                          |
| name             | "image"                     | 表单提交字段名                            |
| autoUpload       | true                        | 选择文件后是否自动上传                    |
| proxy            | true                        | 是否通过代理上传（解决跨域问题）          |
| uploadType       | "fileReceptor"              | 上传处理方式（文件接收器模式）            |
| imageClassName   | "r w-full"                  | 图片容器 CSS 类名（控制圆角和宽度样式）   |
| id               | "u:c13985bb119e"            | 组件唯一标识符                            |
| accept           | ".jpeg, .jpg, .png, .gif"   | 允许上传的图片格式                        |
| multiple         | false                       | 是否允许多文件上传                        |
| hideUploadButton | false                       | 是否隐藏上传按钮                          |
| receiver.url     | "<https://demo.com/upload>" | 文件上传接口地址                          |
| receiver.method  | "get"                       | 上传请求方法（通常应为 POST）             |
| limit            | false                       | 是否限制文件大小/数量（false 表示无限制） |
| crop             | false                       | 是否启用图片裁剪功能                      |

# 表格

```json
{
  "type": "crud2",
  "id": "u:e1c006c6e9a2",
  "mode": "table2",
  "dsType": "api",
  "syncLocation": true,
  "selectable": true,
  "multiple": true,
  "primaryField": "id",
  "loadType": "pagination",
  "filter": {
    "type": "form",
    "title": "条件查询",
    "mode": "inline",
    "columnCount": 3,
    "clearValueOnHidden": true,
    "behavior": ["SimpleQuery"],
    "body": [
      {
        "name": "name",
        "label": "姓名",
        "type": "input-text",
        "size": "full",
        "required": false,
        "behavior": "SimpleQuery",
        "id": "u:a6716f70d3b6"
      },
      {
        "name": "age",
        "label": "年龄",
        "type": "input-text",
        "size": "full",
        "required": false,
        "behavior": "SimpleQuery",
        "id": "u:c5ff5eb546c4"
      }
    ],
    "actions": [
      {
        "type": "reset",
        "label": "重置",
        "id": "u:ea2f0331c6ee"
      },
      {
        "type": "submit",
        "label": "查询",
        "level": "primary",
        "id": "u:de1ebe4a6581",
        "onEvent": {
          "click": {
            "weight": 0,
            "actions": [
              {
                "ignoreError": false,
                "outputVar": "responseResult",
                "actionType": "ajax",
                "options": {
                  "silent": true
                },
                "api": {
                  "url": "https://demo.com/query",
                  "method": "get"
                }
              }
            ]
          }
        }
      }
    ],
    "id": "u:41d8ad3c5075",
    "feat": "Insert",
    "submitOnChange": false,
    "debug": false
  },
  "headerToolbar": [
    {
      "type": "flex",
      "direction": "row",
      "justify": "flex-start",
      "alignItems": "stretch",
      "style": {
        "position": "static"
      },
      "items": [
        {
          "type": "container",
          "align": "left",
          "behavior": ["Insert", "BulkEdit", "BulkDelete"],
          "body": [
            {
              "type": "button",
              "label": "新增",
              "level": "primary",
              "className": "m-r-xs",
              "behavior": "Insert",
              "onEvent": {
                "click": {
                  "actions": [
                    {
                      "actionType": "dialog",
                      "dialog": {
                        "body": {
                          "id": "u:27ba2ab4f3da",
                          "type": "form",
                          "title": "新增数据",
                          "mode": "flex",
                          "labelAlign": "top",
                          "dsType": "api",
                          "feat": "Insert",
                          "body": [
                            {
                              "name": "name",
                              "label": "姓名",
                              "row": 0,
                              "type": "input-text"
                            },
                            {
                              "name": "age",
                              "label": "年龄",
                              "row": 1,
                              "type": "input-text"
                            }
                          ],
                          "resetAfterSubmit": true,
                          "actions": [
                            {
                              "type": "button",
                              "actionType": "cancel",
                              "label": "取消"
                            },
                            {
                              "type": "button",
                              "actionType": "submit",
                              "label": "提交",
                              "level": "primary"
                            }
                          ],
                          "onEvent": {
                            "submitSucc": {
                              "actions": [
                                {
                                  "actionType": "search",
                                  "groupType": "component",
                                  "componentId": "u:e1c006c6e9a2"
                                }
                              ]
                            }
                          }
                        },
                        "title": "新增数据",
                        "size": "md",
                        "actions": [
                          {
                            "type": "button",
                            "actionType": "cancel",
                            "label": "取消"
                          },
                          {
                            "type": "button",
                            "actionType": "submit",
                            "label": "提交",
                            "level": "primary"
                          }
                        ]
                      }
                    }
                  ]
                }
              },
              "id": "u:8de5f955ecb4"
            },
            {
              "type": "button",
              "label": "批量删除",
              "behavior": "BulkDelete",
              "level": "danger",
              "className": "m-r-xs",
              "confirmText": "确认要批量删除数据「${JOIN(ARRAYMAP(selectedItems, item => item.id), ',')}」",
              "disabledOn": "${selectedItems != null && selectedItems.length < 1}",
              "onEvent": {
                "click": {
                  "actions": [
                    {
                      "actionType": "ajax"
                    },
                    {
                      "actionType": "search",
                      "groupType": "component",
                      "componentId": "u:e1c006c6e9a2"
                    }
                  ]
                }
              },
              "id": "u:52aeab5b3c1e"
            }
          ],
          "wrapperBody": false,
          "style": {
            "flexGrow": 1,
            "flex": "1 1 auto",
            "position": "static",
            "display": "flex",
            "flexDirection": "row",
            "flexWrap": "nowrap",
            "alignItems": "stretch",
            "justifyContent": "flex-start"
          },
          "id": "u:eba19c824ac6",
          "isFixedHeight": false
        },
        {
          "type": "container",
          "align": "right",
          "behavior": ["FuzzyQuery"],
          "body": [],
          "wrapperBody": false,
          "style": {
            "flexGrow": 1,
            "flex": "1 1 auto",
            "position": "static",
            "display": "flex",
            "flexDirection": "row",
            "flexWrap": "nowrap",
            "alignItems": "stretch",
            "justifyContent": "flex-end"
          },
          "id": "u:aa6741cb5253",
          "isFixedHeight": false
        }
      ],
      "id": "u:74b4137788a0"
    }
  ],
  "footerToolbar": [
    {
      "type": "flex",
      "direction": "row",
      "justify": "flex-start",
      "alignItems": "stretch",
      "style": {
        "position": "static"
      },
      "items": [
        {
          "type": "container",
          "align": "left",
          "body": [],
          "wrapperBody": false,
          "style": {
            "flexGrow": 1,
            "flex": "1 1 auto",
            "position": "static",
            "display": "flex",
            "flexBasis": "auto",
            "flexDirection": "row",
            "flexWrap": "nowrap",
            "alignItems": "stretch",
            "justifyContent": "flex-start"
          },
          "id": "u:d72449ba1007"
        },
        {
          "type": "container",
          "align": "right",
          "body": [
            {
              "type": "pagination",
              "behavior": "Pagination",
              "layout": ["total", "perPage", "pager"],
              "perPage": 10,
              "perPageAvailable": [10, 20, 50, 100],
              "align": "right",
              "id": "u:aa10b84960f6"
            }
          ],
          "wrapperBody": false,
          "style": {
            "flexGrow": 1,
            "flex": "1 1 auto",
            "position": "static",
            "display": "flex",
            "flexBasis": "auto",
            "flexDirection": "row",
            "flexWrap": "nowrap",
            "alignItems": "stretch",
            "justifyContent": "flex-end"
          },
          "id": "u:f67ed4e7f645"
        }
      ],
      "id": "u:eaea85a6eb06"
    }
  ],
  "columns": [
    {
      "type": "tpl",
      "title": "姓名",
      "name": "name",
      "id": "u:814ac1e4a055",
      "placeholder": "-"
    },
    {
      "type": "tpl",
      "title": "年龄",
      "name": "age",
      "id": "u:427a619d3104",
      "placeholder": "-"
    },
    {
      "type": "operation",
      "title": "操作",
      "buttons": [
        {
          "type": "button",
          "label": "查看",
          "level": "link",
          "behavior": "View",
          "onEvent": {
            "click": {
              "actions": [
                {
                  "actionType": "dialog",
                  "dialog": {
                    "body": {
                      "id": "u:2ec562a811cb",
                      "type": "form",
                      "title": "查看数据",
                      "mode": "flex",
                      "labelAlign": "top",
                      "dsType": "api",
                      "feat": "View",
                      "body": [
                        {
                          "name": "name",
                          "label": "姓名",
                          "row": 0,
                          "type": "input-text"
                        },
                        {
                          "name": "age",
                          "label": "年龄",
                          "row": 1,
                          "type": "input-text"
                        }
                      ],
                      "static": true,
                      "actions": [
                        {
                          "type": "button",
                          "actionType": "cancel",
                          "label": "关闭"
                        }
                      ],
                      "onEvent": {
                        "submitSucc": {
                          "actions": [
                            {
                              "actionType": "search",
                              "groupType": "component",
                              "componentId": "u:e1c006c6e9a2"
                            }
                          ]
                        }
                      }
                    },
                    "title": "查看数据",
                    "size": "md",
                    "actions": [
                      {
                        "type": "button",
                        "actionType": "cancel",
                        "label": "关闭"
                      }
                    ]
                  }
                }
              ]
            }
          },
          "id": "u:11628ae97739"
        }
      ],
      "id": "u:f1a35e2e64b6"
    }
  ],
  "editorSetting": {
    "mock": {
      "enable": true,
      "maxDisplayRows": 5
    }
  }
}
```

## 基础配置

| 字段名       | 值               | 含义                              |
| ------------ | ---------------- | --------------------------------- |
| type         | "crud2"          | 组件类型，crud2 表示表格 2.0 组件 |
| id           | "u:e1c006c6e9a2" | 组件唯一标识                      |
| mode         | "table2"         | 表格模式，table2 表示表格 2.0     |
| dsType       | "api"            | 数据源类型，api 表示接口数据      |
| syncLocation | true             | 是否同步 URL 参数                 |
| selectable   | true             | 是否可选中行                      |
| multiple     | true             | 是否允许多选                      |
| primaryField | "id"             | 主键字段名                        |
| loadType     | "pagination"     | 加载类型，pagination 表示分页加载 |

## 过滤器(filter)

| 字段名             | 值                            | 含义                          |
| ------------------ | ----------------------------- | ----------------------------- |
| type               | "form"                        | 过滤器类型，form 表示表单形式 |
| title              | "条件查询"                    | 过滤器标题                    |
| mode               | "inline"                      | 显示模式，inline 表示行内显示 |
| columnCount        | 3                             | 每行显示的字段数量            |
| clearValueOnHidden | true                          | 隐藏时是否清空值              |
| behavior           | "SimpleQuery"                 | 过滤器行为                    |
| body               | 表单字段数组,其他基础组件组合 | 过滤条件字段配置              |
| actions            | 按钮数组，其他按钮组件的组合  | 操作按钮配置                  |
| id                 | "u:41d8ad3c5075"              | 过滤器唯一标识                |
| feat               | "Insert"                      | 功能标识                      |
| submitOnChange     | false                         | 值变化时是否自动提交          |
| debug              | false                         | 是否开启调试模式              |

## 顶部工具栏(headerToolbar)

| 字段名     | 值                               | 含义                        |
| ---------- | -------------------------------- | --------------------------- |
| type       | "flex"                           | 布局类型，flex 表示弹性布局 |
| direction  | "row"                            | 排列方向，row 表示水平排列  |
| justify    | "flex-start"                     | 主轴对齐方式                |
| alignItems | "stretch"                        | 交叉轴对齐方式              |
| items      | 工具栏项数组，其他基础组件的组合 | 工具栏内容                  |
| id         | "u:74b4137788a0"                 | 工具栏唯一标识              |

## 底部工具栏(footerToolbar)

| 字段名     | 值                               | 含义                        |
| ---------- | -------------------------------- | --------------------------- |
| type       | "flex"                           | 布局类型，flex 表示弹性布局 |
| direction  | "row"                            | 排列方向，row 表示水平排列  |
| justify    | "flex-start"                     | 主轴对齐方式                |
| alignItems | "stretch"                        | 交叉轴对齐方式              |
| items      | 工具栏项数组，其他基础组件的组合 | 工具栏内容                  |
| id         | "u:eaea85a6eb06"                 | 工具栏唯一标识              |

## 列配置(columns)

| 字段名      | 值               | 含义                   |
| ----------- | ---------------- | ---------------------- |
| type        | "tpl"            | 列类型，tpl 表示模板列 |
| title       | "姓名"           | 列标题                 |
| name        | "name"           | 字段名                 |
| id          | "u:814ac1e4a055" | 列唯一标识             |
| placeholder | "-"              | 空值占位符             |

| 字段名  | 值               | 含义                         |
| ------- | ---------------- | ---------------------------- |
| type    | "operation"      | 列类型，operation 表示操作列 |
| title   | "操作"           | 列标题                       |
| buttons | 按钮数组         | 操作按钮配置                 |
| id      | "u:f1a35e2e64b6" | 列唯一标识                   |

## 按钮组件(buttons)

### 通用配置

| 字段名                | 含义               | 示例值                         |
| --------------------- | ------------------ | ------------------------------ |
| type                  | 按钮类型           | "button", "submit", "reset"    |
| label                 | 按钮显示文本       | "查询", "新增"                 |
| id                    | 按钮唯一标识       | "u:8de5f955ecb4"               |
| level                 | 按钮级别           | "primary", "danger", "link"    |
| behavior              | 按钮行为           | "Insert", "BulkDelete", "View" |
| onEvent.click.actions | 点击事件触发的动作 | [动作数组]                     |

### 特殊配置

#### 重置按钮

- 来源: 过滤器
- type: "reset"
- 特殊说明: 用于重置表单输入

#### 提交按钮

- 来源: 过滤器
- type: "submit"
- level: "primary"
- 特殊说明: 用于提交表单查询

#### 新增按钮

- 来源: 顶部工具栏
- behavior: "Insert"
- 特殊说明: 打开新增数据对话框

#### 批量删除按钮

- 来源: 顶部工具栏
- level: "danger"
- behavior: "BulkDelete"
- confirmText: "确认要批量删除数据..."
- disabledOn: "${selectedItems != null && selectedItems.length < 1}"
- 特殊说明: 需要选中数据才能操作

#### 查看按钮

- 来源: 操作列
- level: "link"
- behavior: "View"
- 特殊说明: 查看单条数据详情

#### 对话框按钮

- 类型: 取消/提交/关闭
- actionType: "cancel"或"submit"
- 特殊说明: 用于对话框操作
