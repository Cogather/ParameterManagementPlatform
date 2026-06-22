## ADDED Requirements

### Requirement: 参数 Excel 导出列与页面主表对齐

系统 **必须** 在参数导出 XLSX 与导入模板中包含与页面两段式表单一致的列（详见 design D1），至少包括：

- `value_range_segments`（列名「取值区间」，JSON）
- `value_range`（列名「取值范围」，拼接展示串）
- `parameter_unit_cn` / `parameter_unit_en`（列名「单位（中文）」「单位（英文）」）
- `is_published`、`no_publish_reason`、`related_license`、`product_form_id`、`platform_generation`、`application_region`
- `impact_level_cn`、`impact_level_en`、`internal_description`

导出 **不得** 再包含页面已移除/隐藏的列：立即生效、变更来源、版本号、枚举值、参数范围。

#### Scenario: 导出含新治理字段

- **WHEN** 用户导出某版本下已填写「是否发布」「平台代际」的参数
- **THEN** XLSX 对应列 **必须** 带出非空值

#### Scenario: 导出含取值区间 JSON

- **WHEN** 参数已保存 `value_range_segments`
- **THEN** 导出「取值区间」列 **必须** 为 JSON，「取值范围」列 **必须** 为拼接串

### Requirement: 参数 Excel 导入与 API 校验一致

导入新建行 **必须** 调用 `ParameterSaveValidation.assertCreate`；导入更新行 **必须** 调用 `mergeHiddenFields` 后 `assertUpdate`。取值区间 **必须** 通过 `ValueRangeSegmentsSupport` 写入 segments 与 `value_range`。

旧模板仅含「取值范围」文本时 **必须** 可解析为 segments 并成功导入（在其它必填满足时）。

#### Scenario: 旧模板仅取值范围文本

- **WHEN** 导入文件「取值范围」为 `1-10,20-30` 且「取值区间」列为空
- **THEN** 系统 **必须** 解析并写入 `value_range_segments` 与 `value_range`

#### Scenario: 导入缺基础必填失败

- **WHEN** 新建导入行缺少「单位（中文）」或「参数默认值」
- **THEN** 该行 **必须** 失败并返回可读错误，其它行不受影响

### Requirement: 变更说明导入校验与页面一致

导入变更说明列组时 **必须** 仅强制 `change_reason_cn`、`change_impact_cn` 必填；英文两列 **不得** 强制。

#### Scenario: 导入变更说明英文为空

- **WHEN** 导入行含变更类型与中文原因/影响，英文为空
- **THEN** 该行变更说明 **必须** 写入成功

## MODIFIED Requirements

### Requirement: 参数导入模板下载

导入模板 **必须** 与导出使用同一套表头（`parameterExportHeadersZh` 或等价单一来源），且说明行 **必须** 提示：新建时「参数ID」留空、取值区间 JSON 格式、新列必填规则与页面新增一致。

#### Scenario: 下载模板表头与导出一致

- **WHEN** 用户下载 parameters-import-template.xlsx
- **THEN** 表头列集合 **必须** 与 export 接口一致
