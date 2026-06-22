## Why

参数新增/编辑表单当前按 5 个区块平铺展示，字段必填规则与业务填报节奏不匹配：新增时应先完成基础标识即可落库，详细信息可后续补齐；同时取值范围仍为自由文本、缺少发布治理字段（是否发布、平台代际、应用区域等），与运营规范脱节。需要将表单重构为「基础信息 + 详细信息」两段式，并统一前后端分阶段校验与取值范围结构化存储。

## What Changes

- **表单分区**：新增/编辑弹窗重组为 **基础信息**（12 项）与 **详细信息**（29 项）两大区块，字段顺序与必填/可选按产品冻结清单对齐。
- **分阶段校验**：**新增**保存仅校验基础信息必填 + 变更说明中文必填；**编辑**保存叠加详细信息必填校验。
- **取值范围重构**：页面以多段 **最小值～最大值** 录入（适用**所有参数类型**），结构化存 `value_range_segments`（JSON），拼接写入 `value_range`（如 `1-10,20-30`）；页面**不展示、不允许直接编辑** `value_range` 文本框。
- **新字段（DDL）**：`is_published`、`no_publish_reason`、`related_license`、`product_form_id`、`platform_generation`、`application_region`、`value_range_segments`。
- **字段调整**：「参数单位」改名为「单位」并移入基础信息；产品形态下拉取自产品配置（`entity_basic_info`）；补全影响级别、内部功能描述 UI。
- **页面移除**：枚举值、参数范围（`parameter_range`）折叠区；**隐藏**（表保留）立即生效、变更来源、版本号三字段。
- **服务端占位废除**：`ParameterDefaults` 不再为业务字段填充 `待补充`/`TBD`/`默认`；新增时详细信息允许 **NULL**。
- **变更说明**：子表保留；**中文**原因/影响必填，**英文**不强制校验。
- **BREAKING**：取值范围交互由文本输入改为多段 min/max；创建参数时详细信息不再由服务端自动占位。

## Capabilities

### New Capabilities

- （无独立新 capability；新字段与表单规则落在 `parameter-core` delta。）

### Modified Capabilities

- `parameter-core`：§3 页面字段与校验规则全面更新——两段式分区、分阶段必填矩阵、取值范围结构化、6 个新字段、变更说明中文-only 校验、隐藏字段策略。

## Impact

- **后端**：`schema.sql` / DDL 迁移；`SystemParameterPo`；`ParameterDefaults` 收缩；`ParameterAppService` create/update 分层校验；`validateChangeDescriptions` 放宽英文；取值区间拼接服务；`ParameterVersionCopyAppService` 新字段复制；导入导出列映射；`OperationLogAppService` 字段标签。
- **前端**：`ParameterLayout.vue` 表单重排与校验分支；取值区间 UI；`entityBasicInfo` 产品形态下拉；`api/parameter.ts` 类型扩展。
- **文档**：`docs/parameter-management-detailed-design.md` §13.2；`docs/table字段简介.md` §15～§16；归档时合并 `openspec/specs/parameter-core`。
