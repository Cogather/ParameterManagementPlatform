## Context

- **现状**：`parameterExportHeadersZh()` 约 47 列，含已隐藏 UI 字段；不含 `value_range_segments` 及 6 个新治理列；`importParameterRowCreate` 不调用 `ParameterSaveValidation`。
- **目标表单权威**：`parameter-form-two-section-refactor` design + 详设 §13.2。
- **约束**：增量导入「有列才写」语义保留；全量导入仍先删后写；已基线行增量跳过逻辑不变。

## Goals / Non-Goals

**Goals:**

- 导出/模板列与页面可编辑字段一致（含新字段、取值区间 JSON）。
- 导入写入时生成/校验 `value_range_segments` + `value_range`。
- 导入新建/更新与 API 相同分阶段必填（通过 `ParameterSaveValidation`）。
- 旧模板兼容：`取值范围` 单列仍可导入（自动解析为 segments）。

**Non-Goals:**

- 多行变更说明导入（仍仅首条变更说明列组）。
- 修改导入 mode（FULL/INCREMENTAL）语义。
- 前端导入弹窗 UI 改造（除非需提示新模板）。

## Decisions

### D1：导出列顺序（冻结）

主表列顺序与详设 **基础信息 → 详细信息** 一致，变更说明列组置末：

1. 参数ID、归属命令、参数编码、序号（系统/匹配用）
2. 基础信息 12 项（名称中/英、取值区间 JSON、取值范围只读展示列、BIT、默认/推荐、引入版本、单位中/英）
3. 详细信息 29 项（含是否发布、不发布原因、产品形态 ID、平台代际、应用区域、影响级别、内部功能描述等）
4. 数据状态
5. 变更说明 7 列（类型、原因/影响中英文、export_delta、不导出原因）

**移除导出列**（页面已隐藏/移除，导入忽略未知列）：立即生效、变更来源、版本号、枚举值中/英、参数范围。

**双列取值**：

| 列名 | 内容 |
|------|------|
| 取值区间 | `value_range_segments` JSON |
| 取值范围 | 拼接串 `value_range`（导出只读，便于人眼核对） |

导入时：**优先**读「取值区间」JSON；若空则读「取值范围」并 `ValueRangeSegmentsSupport.parse` 兼容旧模板。

### D2：导入校验策略

| 场景 | 校验 |
|------|------|
| 新增行（无 parameter_id 匹配） | `ParameterSaveValidation.assertCreate` + 变更说明（若列有 payload） |
| 更新行 | `ParameterSaveValidation.assertUpdate` + mergeHiddenFields（隐藏三字段保留库值） |
| 变更说明 | `validateChangeDescriptions`（中文必填，英文可选） |

导入失败行进入 `BatchImportResult.failures`，不中断同批其它行（与现有 collector 一致）。

### D3：产品形态列

导出/导入列名 **「产品形态ID」**，值为 `product_form_id`；可选第二列「产品形态名称」只导出展示（导入以 ID 为准，名称列忽略）。

首版仅 **ID 列**，减少 ImportSheetColumns 宽度。

### D4：隐藏字段在导入更新时的 merge

与 API update 相同：`ParameterSaveValidation.mergeHiddenFields(incoming, matched)`，Excel 不再含这三列。

### D5：表头别名（兼容）

`ImportSheetColumns.fromHeader` 保留旧别名映射，避免历史文件立即失效：

- `参数单位（中）` / `单位（中）`
- `取值范围` / `value_range`
- `取值区间` / `value_range_segments`

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 旧 Excel 缺新必填列导致导入失败 | 文档说明 + 模板下载；错误信息指明缺哪列 |
| 列数增多 | 按段分组表头；说明行提示 |
| 全量导出→旧版导入工具 | 版本说明；保留 value_range 解析 |

## Migration Plan

1. 扩展 export/buildRow/ImportSheetColumns（可编译）
2. 导入路径接 Validation + segments 同步
3. 更新测试与 ExcelInstructions
4. 文档同步
5. 与 `parameter-form-two-section-refactor` 一并验收

## Open Questions

- （无；产品形态首版仅 ID 列已冻结。）
