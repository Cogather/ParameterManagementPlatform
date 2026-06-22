## 1. 导出与模板表头

- [x] 1.1 重构 `parameterExportHeadersZh()`：按 design D1 顺序；移除隐藏/废弃 5 列；新增取值区间、治理字段、影响级别、内部功能描述；「参数单位」改为「单位（中文/英文）」
- [x] 1.2 重构 `parameterExportMainCells()` / `buildParameterExportRow()` 与表头一一对应
- [x] 1.3 更新 `ExcelInstructions.PARAMETER_IMPORT_EXPORT_HINT`（新列、取值区间 JSON、必填说明）

## 2. 导入列映射

- [x] 2.1 扩展 `ImportSheetColumns`：新增 colValueRangeSegments、colIsPublished、colNoPublishReason、colRelatedLicense、colProductFormId、colPlatformGeneration、colApplicationRegion、colImpactLevelCn、colImpactLevelEn、colInternalDesc
- [x] 2.2 `fromHeader` 增加列名别名（单位、取值区间、旧取值范围）
- [x] 2.3 `applyMainOptionalFieldsFromLine` 写入新字段；移除对已废弃列的写入（枚举/参数范围/隐藏三字段）
- [x] 2.4 导入收尾：优先 segments JSON，否则从 value_range 文本解析并 `ValueRangeSegmentsSupport.applyToParameter`

## 3. 导入校验对齐 API

- [x] 3.1 `importParameterRowCreate`：在 insert 前调用 `ParameterSaveValidation.assertCreate`
- [x] 3.2 `importParameterRowUpdate`：`mergeHiddenFields` + `assertUpdate`；BIT/黑名单/基线逻辑保持
- [x] 3.3 确认 `importReplaceChangeDescriptionIfPresent` 与中文-only 校验一致

## 4. 测试

- [x] 4.1 更新 `ParameterAppServiceTest` 表头常量与样例 workbook
- [x] 4.2 新增：导出含新列断言；导入旧 value_range 文本解析；导入缺必填失败；导入更新 merge 隐藏字段

## 5. 文档

- [x] 5.1 详设补充 §13.x 参数导入导出列清单（与 export 表头一致）
- [x] 5.2 `docs/table字段简介.md` §15 备注「Excel 列名」或单独导入导出列表
- [x] 5.3 `parameter-form-two-section-refactor/tasks.md` 4.1 可标记由本 change 承接（可选）

## 6. 验收

- [ ] 6.1 下载新模板 → 填基础信息 + 取值区间 JSON → 增量导入成功
- [ ] 6.2 页面保存的参数全量导出 → 再全量导入 → 新字段与 segments 不丢失
- [ ] 6.3 旧文件（仅取值范围文本、无新列）导入：成功或给出明确缺列提示
