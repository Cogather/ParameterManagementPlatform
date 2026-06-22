## ADDED Requirements

### Requirement: 参数表单两段式分区

参数新增/编辑弹窗 **必须** 将主表单字段划分为两个区块，顺序固定：

1. **基础信息**（12 项，顺序）：参数名称（中文）、参数名称（英文）、归属命令、参数类型、序号、取值区间、使用 BIT 位、参数默认值、参数推荐值、引入版本、单位（中文）、单位（英文）。
2. **详细信息**（29 项，顺序）：取值说明（中文/英文）、应用场景（中文/英文）、适用网元、业务分类、生效方式（中文/英文）、项目组、归属模块、参数含义（中文/英文）、影响说明（中文/英文）、配置举例（中文/英文）、是否发布、不发布原因、关联参数描述（中文/英文）、所属特性、影响级别（中文/英文）、关联 License、内部功能描述、产品形态、平台代际、应用区域、备注。

变更说明子表 **必须** 位于两大区块**最下方**（详设 §13.2.B）。

#### Scenario: 新增弹窗展示两段式结构

- **WHEN** 用户在具体版本视图点击「新增」
- **THEN** 弹窗 **必须** 先展示「基础信息」分区，再展示「详细信息」分区，最后展示变更说明子表

### Requirement: 分阶段必填校验矩阵

系统 **必须** 按保存场景执行下列校验（前端与后端一致）：

| 字段组 | 新增保存 | 编辑保存 |
|--------|----------|----------|
| 基础信息标 * 字段 | 必填 | 必填 |
| 详细信息标 * 字段 | **不校验**（允许 NULL 落库） | 必填 |
| 详细信息可选字段 | 不校验 | 不校验 |
| 变更说明 `change_reason_cn`、`change_impact_cn` | 必填 | 必填 |
| 变更说明 `change_reason_en`、`change_impact_en` | **不校验** | **不校验** |
| `export_delta=否` 时 `no_export_reason` | 必填 | 必填 |
| `is_published=否` 时 `no_publish_reason` | 不校验 | 必填 |

**禁止** 服务端在创建时为业务详细信息字段填充 `待补充`、`TBD`、`默认` 等占位值；仅允许 `tenant_id`、`data_status`、`introduce_type` 等系统级默认值（见 design D3）。

#### Scenario: 新增仅填基础信息可保存

- **WHEN** 用户新增参数且仅填写基础信息必填项与变更说明中文，详细信息留空
- **THEN** 保存 **必须** 成功，详细信息列 **必须** 为 NULL

#### Scenario: 编辑未填详细信息必填项被拒绝

- **WHEN** 用户编辑参数且详细信息中标 * 字段为空
- **THEN** 保存 **必须** 失败并提示缺失字段

### Requirement: 取值区间多段结构化存储

**适用所有参数类型**。页面 **必须** 提供多段「最小值～最大值」录入（支持增删段），**不得** 提供 `value_range` 自由文本输入框。

保存时系统 **必须**：

1. 将段列表 JSON 序列化至 `value_range_segments`；
2. 拼接 `value_range` 为 `min-max` 段，段间英文逗号分隔（示例 `1-10,20-30`）；
3. 校验至少 1 段、每段 `min ≤ max`、段间**禁止重叠**、拼接后 `value_range` 长度 ≤255。

列表与详情 **可以** 只读展示 `value_range`；编辑 **必须** 从 `value_range_segments` 还原段列表。

#### Scenario: 两段区间拼接落库

- **WHEN** 用户录入段 `1-10` 与 `20-30` 并保存
- **THEN** `value_range` **必须** 为 `1-10,20-30`，`value_range_segments` **必须** 含对应 JSON 数组

#### Scenario: 重叠区间被拒绝

- **WHEN** 用户录入两段 `1-10` 与 `5-15`
- **THEN** 保存 **必须** 失败并提示区间重叠

### Requirement: 参数治理新字段

`system_parameter` **必须** 支持并列字段：

| 页面标签 | 列名 | 规则 |
|----------|------|------|
| 是否发布 | `is_published` | 下拉 是/否；编辑必填 |
| 不发布原因 | `no_publish_reason` | `is_published=否` 时编辑必填 |
| 关联 License | `related_license` | 可选文本 |
| 产品形态 | `product_form_id` | 下拉，数据源当前产品 `entity_basic_info`；可选 |
| 平台代际 | `platform_generation` | 下拉：裸机形态、虚拟机形态；编辑必填 |
| 应用区域 | `application_region` | 下拉：海外、全球；编辑必填 |

#### Scenario: 产品形态下拉来源

- **WHEN** 用户打开参数表单且已选择产品
- **THEN** 产品形态下拉 **必须** 仅展示该产品在 `entity_basic_info` 中配置的形态行

### Requirement: 页面隐藏与移除字段

下列字段 **必须** 不出现在参数新增/编辑表单中，但表列 **保留**：

- `take_effect_immediately`（立即生效）
- `change_source`（变更来源）
- `patch_version`（版本号）

下列字段 **必须** 从表单 **移除**（不再展示、不再写入）：

- `enumeration_values_cn` / `enumeration_values_en`
- `parameter_range`

「参数单位」标签 **必须** 改为「单位」，并位于基础信息区块。

#### Scenario: 新增不写入隐藏字段

- **WHEN** 用户新增参数保存
- **THEN** `take_effect_immediately`、`change_source`、`patch_version` **必须** 为 NULL（除非未来另行约定）

## MODIFIED Requirements

### Requirement: 变更说明子表校验规则

变更说明子表 **必须** 保留于表单最下方；每次保存（新增/修改）**至少 1 条**。

校验规则 **必须** 为：

- `change_type` 必填；新增表单仅可选「新增参数」；编辑表单可选字典允许的类型；
- `change_reason_cn`、`change_impact_cn` **必填**；
- `change_reason_en`、`change_impact_en` **可选**（允许空或 NULL）；
- `export_delta` 必填，仅允许「是」/「否」；为「否」时 `no_export_reason` 必填。

#### Scenario: 英文变更说明可为空

- **WHEN** 用户填写变更说明中文两格且英文留空
- **THEN** 保存 **必须** 成功

#### Scenario: 中文变更说明缺失被拒绝

- **WHEN** 用户未填写 `change_reason_cn` 或 `change_impact_cn`
- **THEN** 保存 **必须** 失败

### Requirement: 引入版本字段行为

`introduced_version` **必须** 在基础信息区块展示。当 `introduce_type` 为继承或引用且存在 `inherit_reference_version_id` 时，该字段 **必须** 只读并展示同步值；版本新增参数时 **必须** 可编辑，默认当前版本上下文。

#### Scenario: 继承参数引入版本只读

- **WHEN** 用户编辑由继承/同步产生的参数
- **THEN** 引入版本字段 **必须** 只读且展示源同步值

## REMOVED Requirements

### Requirement: 取值范围为自由文本输入

**Reason**: 改为多段 min/max 结构化录入，拼接写入 `value_range`。

**Migration**: 旧数据保留 `value_range` 文本；编辑时优先读 `value_range_segments`，无 JSON 时尝试解析或提示用户重填区间。

### Requirement: 创建参数时服务端业务字段占位默认值

**Reason**: 产品要求新增时详细信息由用户后续填写，禁止 `待补充`/`TBD` 等虚假数据。

**Migration**: 收缩 `ParameterDefaults` 至系统字段；`schema.sql` 列已为 NULL，无需 DDL 放宽。
