## Why

参数新增/编辑表单已完成两段式重构（`parameter-form-two-section-refactor`）：取值区间结构化、6 个治理新字段、分阶段必填、页面隐藏字段等。但 **Excel 导入/导出仍沿用旧表头与宽松校验**，导致：

- 新字段无法通过 Excel 维护，导出再导入会丢失数据；
- 导入路径不调用 `ParameterSaveValidation`，与页面/API 保存规则不一致；
- `value_range` 与 `value_range_segments` 仅单向兼容，易产生脏数据。

需单独一轮改造，使 **导入/导出/模板与页面主表 + 变更说明规则对齐**。

## What Changes

- **导出表头与行数据**：对齐 §13.2 两段式字段；新增 `取值区间`（JSON）、是否发布、平台代际、应用区域等列；隐藏列（立即生效/变更来源/版本号/枚举/参数范围）从模板与导出中**移除或标记废弃**（实现策略见 design）。
- **导入列映射**：扩展 `ImportSheetColumns`；`取值范围` 与 `取值区间` 双列兼容；导入时同步生成 `value_range_segments`。
- **导入校验**：新建/更新行调用与 API 一致的 `ParameterSaveValidation`（新建=基础信息；更新=全量必填）；变更说明仍中文必填。
- **模板与说明**：更新 `ExcelInstructions.PARAMETER_IMPORT_EXPORT_HINT`；单元测试表头与样例行同步。
- **文档**：更新详设导入导出小节、`table字段简介.md` 导入导出列说明（若存在）。

## Capabilities

### New Capabilities

- （无独立 capability。）

### Modified Capabilities

- `parameter-core`：§ 参数 Excel 导入/导出与 `system_parameter` 主表字段、变更说明子表校验对齐。

## Impact

- **后端**：`ParameterAppService`（export/import/template/ImportSheetColumns/buildParameterExportRow/importParameterRow*）
- **测试**：`ParameterAppServiceTest` 及新增 import/export 对齐用例
- **前端**：无 API 变更；下载模板自动获得新表头
- **依赖**：建议在 `parameter-form-two-section-refactor` DDL 已部署后实施
