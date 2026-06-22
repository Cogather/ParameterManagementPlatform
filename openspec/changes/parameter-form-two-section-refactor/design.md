## Context

- **现状**：`ParameterLayout.vue` 分 5 区块；`value_range` 为文本输入；`ParameterDefaults.applyForCreate` 对十余业务字段填占位；前端 `validateForm` 仅校验少量字段；变更说明中英四格后端均必填；`enumeration_values_*`、`parameter_range` 在折叠区；影响级别、内部功能描述有 PO 无 UI。
- **约束**：GaussDB + MyBatis-Plus；`ResponseObject`；详设 §13.2 为页面权威；`schema.sql` 现有列均为 NULL，支持 NULL 策略。
- **已冻结产品决策**：取值区间适用所有类型；变更说明中文必填英文可选；三隐藏字段表不变；产品形态来自 `entity_basic_info`；枚举/参数范围 UI 移除；新增详细信息 NULL、禁止服务端占位；区间段禁止重叠。

## Goals / Non-Goals

**Goals:**

- 两段式表单（基础信息 12 项 + 详细信息 29 项）+ 底部变更说明子表。
- 分阶段校验：create 仅基础信息；edit 叠加详细信息。
- 取值区间多段 min/max → JSON `value_range_segments` + 拼接 `value_range`。
- 新增 6 个业务列；产品形态存 `product_form_id`。
- 废除 `ParameterDefaults` 业务占位；仅保留 `tenant_id`、`data_status`、`introduce_type` 等系统默认。

**Non-Goals:**

- 删除 `take_effect_immediately`、`change_source`、`patch_version` 表列。
- 变更说明子表移除或新增时放宽「至少一行」规则。
- 自动翻译英文长文本（仍可选填/占位展示）。
- 历史数据回填新字段（迁移时保持 NULL，由用户编辑补齐）。

## Decisions

### D1：取值区间存储

**选择**：新增 `value_range_segments TEXT NULL` 存 JSON 数组 `[{"min":1,"max":10},...]`；保存时拼接 `value_range` = `min-max` 逗号连接（如 `1-10,20-30`）。

**规则**：至少 1 段；每段 `min`、`max` 整数且 `min ≤ max`；**段间禁止重叠**；`value_range` 总长 ≤255，超限拒绝保存。

**展示**：表单与列表可只读展示 `value_range`；编辑加载时从 `value_range_segments` 还原，无 JSON 时尝试解析 `value_range` 文本（兼容旧数据）。

### D2：BIT 位与取值区间职责分离

**选择**：所有参数类型均必填取值区间；`bit_usage` 仍由 `bit_count` 驱动，仅 `bit_count > 0` 时展示并必填。

**理由**：BIT 管序号内占用；区间管数值合法范围；与历史 function「取值范围=BIT 勾选」脱钩。

### D3：分阶段校验落点

| 层级 | create | edit |
|------|--------|------|
| 前端 `validateForm` | 基础信息 * + 变更说明中文 | 基础 + 详细信息 * + 变更说明中文 |
| 后端 `ParameterAppService` | 同前端矩阵 | 同前端矩阵 |
| `ParameterDefaults` | 仅系统字段 | 更新时不覆盖 NULL 业务字段 |

新增详细信息字段 **不校验、不写占位**，落库 NULL。

### D4：新字段落库

| 字段 | 列名 | 类型 | 枚举/关联 |
|------|------|------|-----------|
| 是否发布 | `is_published` | VARCHAR(10) | 是/否 |
| 不发布原因 | `no_publish_reason` | VARCHAR(1024) | `is_published=否` 时 edit 必填 |
| 关联 License | `related_license` | VARCHAR(255) | 可选 |
| 产品形态 | `product_form_id` | VARCHAR(50) | FK `entity_basic_info.product_form_id` |
| 平台代际 | `platform_generation` | VARCHAR(50) | 裸机形态 / 虚拟机形态 |
| 应用区域 | `application_region` | VARCHAR(50) | 海外 / 全球 |

### D5：隐藏字段策略

`take_effect_immediately`、`change_source`、`patch_version`：**页面不展示**；create 不传（NULL）；edit 保留库中原值（请求体不包含或后端 merge 时不覆盖）。

`change_source` 黑名单：**仅当用户曾填写非空白值时校验**；隐藏后新增为 NULL，不触发黑名单。

### D6：变更说明校验

**选择**：`change_reason_cn`、`change_impact_cn` 必填；`change_reason_en`、`change_impact_en` 可选（允许 NULL/空串）。`export_delta=否` 时 `no_export_reason` 仍必填。

### D7：引入版本只读

`introduce_type` 为继承/引用且存在 `inherit_reference_version_id` 时，`introduced_version` **只读**（同步自源）；版本新增参数可编辑，默认当前版本名/ID（与现有 `resetForm` 对齐）。

### D8：产品形态数据源

`GET /entity-basic-infos?productId=` 分页或专用 choices；下拉展示 `productForm`，落库 `product_form_id`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 旧数据 `value_range` 非标准格式 | 编辑时优先读 segments；无 JSON 则解析失败时提示用户重填区间 |
| 废除占位后导入/批量创建失败 | 导入路径同步分层校验；文档标明 create 仅需基础信息 |
| `value_range` 255 超限 | 保存前校验拼接长度 |
| 复制继承新字段 | `ParameterVersionCopyAppService` BeanUtils 自动复制新列 |
| 详设/function 双源漂移 | 本 change 同步更新详设 §13.2 与 `table字段简介.md` §15 |

## Migration Plan

1. DDL 增列（可空，无回填）。
2. 后端：PO、校验、Defaults 收缩、区间拼接。
3. 前端：表单两段式 + 区间 UI。
4. 导入导出、操作日志、复制路径。
5. 文档与 OpenSpec 归档合并。
6. **回滚**：前端切回旧表单；新列可保留；`ParameterDefaults` 可临时恢复（不推荐）。

## Open Questions

- （无阻塞项；区间段禁止重叠已冻结。）
