# 参数管理系统详细设计说明书

> **文档目的**：在 `function.md` 与 `table字段简介.md` 基础上，输出可落地、可按 **domain/限界上下文** 拆分为 **少量 OpenSpec**（建议 4～5 份，见 §15.1）的详细设计。  
> **规范依据**：`docs/backend-dev.md`（DDD、充血模型、分层、GaussDB 8.2 MySQL 兼容、MyBatis-Plus、REST、`ResponseObject`）、`docs/frontend-style.md` 及 Cursor Rules（`backend-spring.mdc` / `frontend-vue.mdc`）。

---

## 0. 导读

### 0.1 范围与读者

| 项 | 说明 |
|----|------|
| **范围** | 参数管理系统：命令与类型配置、主数据配置、参数主数据与变更说明；不含第一版「参数同步」（见 `function.md`）。 |
| **读者** | 架构/后端/前端/测试/运维；后续 OpenSpec 拆分责任人。 |

### 0.2 DDD 子域与文档章节映射

| 限界上下文 / 子域 | 对应 `function.md` | 本文章节 | 主要聚合与表 |
|--------------------|-------------------|---------|-------------|
| **产品主数据** | 2.0 | **§0.6** | `entity_basic_info`（产品配置页，顶栏产品下拉数据源） |
| **通用基础** | 全局规则 | **§1** | 约定、分页、权限、导入导出、**操作日志（`operation_log` 单表，见 §1.7）** |
| **命令管理** | 2.1.1～2.1.3 | **§2～§4** | 命令、自定义类型、版本区段 |
| **配置主数据** | 2.2.1～2.2.8 | **§5～§12** | 版本、分类、关键字、网元、NF、生效方式、特性、项目组 |
| **参数核心** | 2.3 | **§13** | `system_parameter`、`config_change_description`、`config_change_type` |
| **产品上下文** | 嵌入各页「按产品」 | 各章 | `entity_basic_info` |
| **工程目录** | 前后端仓库结构 | **§14** | 后端 `com.coretool.param.*`；前端 `param-web/src/*` |

### 0.3 后端分层与落地要求（摘要）

- **领域层（`domain`）**：聚合根、实体、值对象、领域服务、**仓储接口**；业务规则与不变量写在充血模型内；**禁止**领域层依赖 MyBatis-Plus 注解。
- **应用层（`application`）**：用例服务、事务边界、编排领域对象、DTO 组装；薄逻辑。
- **基础设施层（`infrastructure`）**：MyBatis-Plus Mapper、仓储实现、外部系统；**分页 SQL 使用 `LIMIT offset, size`**；禁止复杂原生 SQL、禁止存储过程/触发器/外键等（见 `backend-dev.md`）。
- **接口层（`ui`）**：`Controller` + `ResponseObject<T>`（`success` / `message` / `data`）；REST 资源名词、**`/api/v1`** 前缀。

### 0.4 表字段文档与需求对齐说明

- 页面字段以 **`docs/table字段简介.md`** 为准自动补齐；若与 `function.md` 表述冲突，以**表结构**为落库依据，需求差异在实现阶段走变更单。
- `config_change_source_keyword` 已含 **`owned_product_id`**，按产品隔离关键字；同一产品下 **`keyword_id`** 唯一；页面仅展示「**关键字正则**」「**原因（reason）**」，`keyword_id` 为后端/数据库内部标识，不在页面呈现（见 `table字段简介.md` §7）。
- **`system_parameter.parameter_id`** 与 **`config_change_description.parameter_id`** 外键类型须一致。`table字段简介.md` §15 将主表 `parameter_id` 标为 **Integer**、§16 将子表 `parameter_id` 标为 **String 255** 时存在歧义；**实现与 DDL 以主表类型为准**，子表外键与主表同型（若 DDL 统一为字符串型 ID，则全文按 DDL）。

### 0.5 产品主数据上下文（`entity_basic_info`）

各子域「按产品配置」依赖产品主数据；表 **`entity_basic_info`** 字段与 `table字段简介.md` §1 一致，页面若含产品维护则需全覆盖：

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `entity_name` | 产品名称 | String 255 | 是 | 支持中英文 |
| `product_form_id` | 产品形态 ID | String 50 | 是 | 系统生成，全局唯一 |
| `product_soft_param_type` | 产品参数类型 | Enum 50 | 是 | Single / Multi |
| `product_form` | 产品形态 | String 255 | 否 | 一产品多形态；表备注「主键」指业务语境，技术主键以 `product_form_id` 为准 |
| `product_id` | 产品 ID | String 50 | 是 | 关联外部产品系统；子域 **`owned_product_id`** 多与之对齐（见表关联说明） |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | 毫秒 |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |
| `owner_list` | 责任人 | String 255 | 是 | 英文逗号分隔 |
| `entity_status` | 状态 | Integer | 否 | |

### 0.6 产品配置（`entity_basic_info` 维护端）

- **定位**：`function.md` **§2.0**；前端在顶栏子模块中提供与「命令 / 配置 / 参数管理」**同级**的 **「产品配置」**，并置于**最前**；路由默认进入 **`/product`**。  
- **与顶栏强联动（冻结）**：该页**不单独维护「产品」概念**的录入：用户**必须先在顶栏选择产品**（`product_id`）；本页**仅**增删改当前产品下的**产品形态行**（`product_form` 及该行的 `product_soft_param_type`、`owner_list`）。新增形态时，由应用层将**当前产品 ID** 与**展示用产品名称**（与顶栏展示名/上下文一致，落库 `entity_name`）与形态字段一并写入。  
- **信息架构**：页面**不出现**与工具条重复的大标题/说明性段落；主区域直接展示表格列**产品形态、参数类型、责任人**（默认**只读**展示），**点击「编辑」**进入**行内编辑**；**新增**仅弹窗维护上述三业务字段。  
- **页内能力**：**增 / 行内改 / 删**（删为**逻辑删除**：`entity_status=0`）；列表请求 **必须**带 Query **`productId`** 与顶栏产品一致。  
- **唯一性（冻结，可验收）**：在**同一 `product_id`** 且**启用态**（`entity_status=1` 或 `NULL`）下，`product_form`（产品形态）名称在**该产品范围内**唯一，比较规则为 **`lower(btrim(产品形态))` 唯一**；与同一产品下其他启用行冲突时拒绝保存。  
- **与顶栏产品上下文的关系**：`GET /api/v1/entity-basic-infos/product-choices` 对启用态行按 `product_id` 聚合，供「产品：」下拉的 `productId` / 展示名使用（同 `product_id` 多形态时，取服务实现约定的一条作为展示名）。  
- **接口（摘要）**：
  - `GET /api/v1/entity-basic-infos`：分页，Query `page` / `size` / `keyword` / **`productId`（与顶栏必一致）**  
  - `POST /api/v1/entity-basic-infos`：新增（服务端生成 `product_form_id`；`product_id` 与 `entity_name` 与顶栏/上下文带齐）  
  - `PUT /api/v1/entity-basic-infos/{productFormId}`：修改（仅允许改形态行的业务字段，不允许换绑 `product_id`）  
  - `DELETE /api/v1/entity-basic-infos/{productFormId}`：逻辑删除
  - `GET /api/v1/entity-basic-infos/product-choices`：顶栏产品下拉

---

## 1. 【公共通用规范】

> 全模块复用；拆 OpenSpec 时作为 **`common`** 或 **`platform-conventions`** 独立 Spec。

### 1.1 统一响应与错误

- 业务接口返回 **`ResponseObject<T>`**：`success`、`message`、`data`（无 `meta` / `number`，见 `backend-dev.md`）。
- **HTTP 状态码**：业务校验/保存失败类错误 **统一返回 500**；同时在响应体中设置 `success=false` 并给出可读 `message`（前端直接提示）。

### 1.2 分页（后端统一）

| 项 | 约定 |
|----|------|
| **请求** | Query：`page`（从 1 开始）、`size`（默认 20，与 `function.md` 参数列表一致可调）、`sort`（可选，字段名 + `asc/desc`）。 |
| **响应** | `data` 内为分页包装：`records` / `list`、`total`、`page`、`size`（字段名在 OpenSpec 中冻结）。 |
| **SQL** | GaussDB 8.2 MySQL 兼容：**`LIMIT offset, size`**，`offset = (page - 1) * size`。 |
| **实现** | MyBatis-Plus 分页插件，禁止手写复杂分页 SQL。 |

### 1.3 产品上下文（归属产品）

- 绝大多数配置与参数均**按产品隔离**；关联字段 **`owned_product_id`** 与 `entity_basic_info.product_id` 对齐（见 `table字段简介.md` 表关联说明及各表「备注」列）。
- 前端：进入子功能前**必选当前产品**（或自宿主框架传入）；后端：**未传或越权跨产品 → 拒绝**。

### 1.4 状态字段

- 各业务表/字典表 `*_status`：按表注释 **1=启用，0=禁用**（若表为可选，以表为准）；删除策略：**逻辑禁用优先**，物理删除需评审。
- **页面展示约束（已落地到前端）**：
  - **不展示** `*_status` 字段（状态为内部字段）
  - 页面仅提供“删除”（逻辑禁用）能力：写入 `*_status = 0`
  - 列表默认 **过滤 `*_status = 0`** 的记录（删除后不再展示）
- **新增命中“已删除唯一键”的恢复策略（冻结，可验收）**：
  - 若用户在“新增”时输入的唯一字段（如名称）与同产品下 **已删除/未启用（`*_status=0`）** 的历史记录一致，则后端应**直接恢复该记录**（置 `*_status=1`）并更新字段；
  - 前端不应提示“同一产品下名称唯一/已存在”等重复名错误（该错误仅适用于与启用态 `*_status=1` 冲突的场景）。

### 1.4.1 页面不展示 ID/编码（已落地到前端）

- 页面（表格/表单/下拉）**不展示**各类技术标识：`*_id`、`*_code`、以及 `parameter_code` 等内部编码字段。
- 下拉框均以“名称”作为展示文本（例如命令显示命令名称，而非命令 ID）。
- 新建时如存在“编码/ID”字段，页面不要求填写，交由后端生成；导入/导出 Excel 的“ID 列”仅用于区分新增/修改（见 §1.6），不代表页面展示该 ID。

### 1.5 权限模型（建议）

| 权限点编码 | 说明 |
|------------|------|
| `param:product:read` | 查看产品与上下文 |
| `param:command:*` | 命令管理各接口（可再细分 read/write/import/export） |
| `param:config:*` | 配置管理各子模块 |
| `param:parameter:*` | 参数查询/编辑/导入导出/基线（按项目细化） |
| `param:project-team:admin` | 项目组与人员关联（2.2.8 深化时启用） |

具体与宿主统一认证对接；本章仅列**设计占位**，落地时替换为实际 IAM 编码。

### 1.6 导入 / 导出（通用）

| 项 | 约定 |
|----|------|
| **格式** | Excel `.xlsx` / `.xls`、CSV（见 `function.md`）。 |
| **流程** | 上传 → 异步/同步解析 → **逐行校验与逐行提交** → 汇总结果返回。 |
| **失败与成功（冻结）** | **不做整单回滚**：需明确返回 **成功条数/失败条数**；**失败明细**每条含 **行号 + 失败原因**；**成功明细**含已成功写入的 **行号**（或等价列表），便于用户对账与重试仅失败行。 |
| **模板** | 每模块提供**下载模板**接口；列顺序与表字段/页面列一致。 |
| **大数据量** | 导出限制最大行数或异步任务 + 下载链接（DFX）。 |

**交互补充（冻结，可验收）**：

- **导入弹框**：点击“导入”时弹出对话框：
  - 上部为文件选择（仅允许 `.xlsx`）
  - 下部为带下划线的“下载模板”链接（替代独立按钮）
- **模板/导出表头**：必须使用**页面一致的中文列名**（来源：各子域章节「页面字段与表字段映射」表格中的“中文名称”列），禁止使用数据库字段名。
- **模板/导出列约束（冻结）**：模板与导出 Excel 的列必须与**页面实际展示列**一致，**不得**包含内部字段：
  - `*_status` 状态字段
  - `creator_id/creation_timestamp/updater_id/update_timestamp` 等审计字段
  - 页面不展示的内部 ID/编码字段（如 `*_id`、`*_code`、`parameter_code` 等，除非该模块页面明确展示）
- **导出数据范围（冻结）**：导出 Excel **仅包含启用态（`*_status=1`）** 数据；已删除/未启用（`*_status=0`）的记录**不得出现在导出文件**中。
- **首行说明**：导出的 Excel 与模板的第一行固定为：
  - `说明：ID为空时新增，ID有值代表修改。`
  第二行为表头。
- **导入语义**：按首行说明执行：
  - **ID 为空**：新增；后端自动生成 ID（`IdGenerator`）并插入
  - **ID 有值**：修改；按 ID 定位并更新除主键外字段
  - **审计字段**（创建人/创建时间/修改人/修改时间）由后端自动填充获取，不在 Excel 展示、也不从 Excel 读取
  - **状态字段**（`*_status`）为内部字段：不在 Excel 展示、也不从 Excel 读取
  - **操作日志**：规则见 **§1.7.4.1**（**只记真实增/删/改**；**失败**行不记；**修改**行若与库**完全一致、无任何字段变化**则**整行不记**、**不**视为用户覆盖；有变更的**成功**行才按条记，同批有失败时**有变更的成功行**仍记）。
- **状态字段交互**：用户不直接编辑 `*_status`：
  - 新建默认为 `1`（启用）
  - “删除”操作改为逻辑禁用：置 `0`（未启用）
  - **不提供“启用/禁用切换”入口**（如需恢复，走数据库修复或后续另立需求）

**导入结果 `data` 结构示例（OpenSpec 冻结字段名）**：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "totalRows": 100,
    "successCount": 96,
    "failureCount": 4,
    "successRowNumbers": [1, 2, 3, 5, 6],
    "failures": [
      { "rowNumber": 4, "reason": "参数 ID 与序号不一致" },
      { "rowNumber": 7, "reason": "BIT 占用冲突：BIT3" }
    ]
  }
}
```

### 1.7 操作日志与审计

#### 1.7.1 目标与既有能力的关系

- **目标**：提供**全系统统一**的「页面级/资源级」操作留痕：凡涉及**数据新增、修改、删除**的写操作，均写入**同一张**操作日志表；查询时按**业务表名**（及**产品、版本**、可选资源）筛选，用于各列表页**右侧「操作日志」**抽屉展示。
- **与 `config_change_description` 的边界**：参数域「变更说明」仍由 `config_change_description` 承载**业务含义**的变更说明；**本节操作日志**侧重**管理端字段级**审计：操作类型、**变更项**、原值、新值、操作人（**用户 ID 文本**）、操作时间。二者互补，不互相替代。
- **与外部审计系统**：不强制对接宿主/第三方审计；**以本服务 `operation_log` 为唯一落库**即可，无额外双写要求。

#### 1.7.2 单表集中存储与「表名」维度

| 项 | 约定 |
|----|------|
| **单表** | 全产品仅维护**一张**操作日志表（建议物理表名 `operation_log`；OpenSpec/DDL 中可最终命名，本文统称「操作日志表」）。**禁止**为每个业务表各建一张日志表。 |
| **表名区分页面** | 每条日志必带**业务主表物理表名**字段（如 `entity_command_mapping`、`entity_basic_info`），与 `table字段简介.md` / 持久化 PO 所绑定的表名一致。前端「操作日志」请求与当前页面对应的**同一张业务表**绑定，使**同一表**在**产品 + 版本**（及不同行数据）上的操作可统一检索。 |
| **一屏一表（冻结）** | 当前**不存在**单页面写入多主表的场景，设计**暂不**增加「一屏多表」路由；**后续演进仍保持一页面一主表**，操作日志的 `biz_table` 与页面主表一一对应即可。 |
| **产品与版本** | 日志须带 **`owned_product_id`**（顶栏/路径中的 `productId`），**必填**。另增加 **`owned_version_id`**，与 `entity_version_info.version_id` 对齐；**仅产品维度、无版本上下文的配置页**（如多数命令/配置主数据）该列填 **`NULL`**；**参数管理**等依赖版本子上下文的写操作**必须**写入当前**版本**。**查询**时与顶栏/页内**当前选中的产品 + 版本**一致（与 **§5 / §13** 的用法衔接）。 |
| **资源定位（可选但推荐）** | 为便于单条数据维度的筛序，可存 **`resource_id`**（业务主键，如 `command_id`）；**列表页「全页操作日志」**以 `biz_table + owned_product_id` + 可选 `owned_version_id` 为粗粒度即可，**行级详情**若后续需要可在扩展任务中单独立项。 |

#### 1.7.3 表结构（建议字段）

以下为逻辑字段，具体类型/长度在 DDL 中按 GaussDB 8.2 MySQL 兼容模式落库；**对外的表格列名与语义**以 **§1.7.7** 为准（如「变更项」列对应本表 `field_label_cn` 的展示语义）。

| 逻辑字段 | 说明 |
|----------|------|
| 主键 | 自增或雪花/UUID，由统一 `IdGenerator` 或 DB 自增二选一，团队冻结一种。 |
| `biz_table` | 业务主表**物理表名**（小写+下划线），用于区分各页面/模块的日志。 |
| `owned_product_id` | 当前产品 ID，与 **§1.3** 一致，**必填**。 |
| `owned_version_id` | 归属版本 ID，关联 `entity_version_info`；**无版本上下文的页面写操作**可为 `NULL`，**有版本上下文（如参数）**时**必填**。 |
| `resource_id` | 被操作资源业务主键（若适用）；**删除**时仍建议写入，便于对账。 |
| `operation_type` | 枚举：`CREATE` / `UPDATE` / `DELETE`（对外展示时映射为**新增** / **修改** / **删除**）。 |
| `field_label_cn` | 落库字段名可保留；**页面/接口展示列名**为 **「变更项」**，语义为对应页面列的中文名（与「页面字段与表字段映射」一致）。**删除**场景见 **§1.7.4**。 |
| `old_value` | 修改前/删除前的可展示值（文本化）；无则空串或 `NULL`。**新增**时原值为空。 |
| `new_value` | 修改后/新增后的可展示值；**删除**时按 **§1.7.4** 约定。 |
| `operator_id` | 操作人用户标识；**界面直接展示该 ID 字符串**，不强制解析为中文姓名。 |
| `operated_at` | 操作时间（建议毫秒时间戳）；**全站展示使用服务器时区**（与现有列表审计时间列一致）。 |

**索引建议（在 DDL 中体现）**：`(biz_table, owned_product_id, owned_version_id, operated_at DESC)` 作为列表查询主路径（`owned_version_id` 可为 NULL 时由优化器/执行计划按实际数据量评估）；视需要增加 `resource_id`。

#### 1.7.4 写日志规则：新增、修改、删除

| 操作类型 | 行数与内容 |
|----------|------------|
| **新增（CREATE）** | **仅「页面有展示列」且属于用户本次输入/编辑的字段**各记**一行**（**系统生成、默认回填、后端自动补全**等非用户显式输入项**不记**）。与 **§1.4.1** 一致：内部 `*_id`/`*_code` 等不在页面展示则不记。`operation_type=CREATE`；`old_value` 为空；`new_value` 为该字段**展示态**字符串；变更项列为对应中文名。若一次保存写入多字段，则多条记录共享同一**逻辑批次**（可选：`log_batch_id`，**OpenSpec 可选项**）。 |
| **修改（UPDATE）** | 仅对**实际发生变更、且页面有展示**的字段各记一行（diff）：`old_value` / `new_value` 为变更前后**展示态**（枚举、状态码需转成与页面一致的中文或可读文本）。未变化字段不记。 |
| **删除（DELETE）** | **仅记一行**（或按产品约定固定行数，不展开字段级）：`operation_type=DELETE`；**不必**为每个字段各一行。变更项固定为 **「删除对象」**（或等效统一文案）；`new_value` 填写**被删数据在页面上的主展示名称**（如命令名「命令1」、网元名等，与列表第一列/用户认知一致的「名称」字段）；`old_value` 可为空，或与 `new_value` 二选一存名称，**禁止**混用多义，在 OpenSpec 中冻结一种写法。 |
| **逻辑删除** | 与业务 **§1.4** 一致：若「删除」实为 `*_status=0` 的禁用，仍记为**删除**类操作，展示文案可统一为「删除」，**名称**取该行业务名称字段。 |

#### 1.7.4.1 导入（§1.6）与操作日志（冻结）

- **只记真实增、删、改（冻结）**：
  - **操作日志**仅覆盖**在业务上确实发生**的 **INSERT（新增条）/ UPDATE（有字段被改）/ DELETE（含逻辑删除条）**；**不因**用户执行了「导入」这一动作、或 Excel 与库**内容一致**的重复提交，**而**为「走流程」或「视为覆盖」**额外**记日志。
- **失败行不记**：
  - **导入失败**行（该校验/落库未成功、出现在 **§1.6** 失败明细、业务数据未按预期写入的整行）**不**写任何 `operation_log` 行。
- **「修改」行与库完全一致 → 整行 0 条日志（不视为用户覆盖）**：
  - 对 **ID 有值、语义为修改**的导入行：在**可比对范围内**（参与导入落库的**页面展示列**、与 **§1.7.4** 记日志范围一致）与**变更前**库中值逐字段比较后，若**无任一字段值实际变化**（**数据与库中一致、即无增删改效果**），则**本行不产生任何** `operation_log` 行——**不**将「导入时提交了相同内容」计为**用户覆盖**或**一次需留痕的修改**。
- **有实质变更的成功行须记、且按字段拆解**：
  - **导入成功**且**在上一款意义上确有增/删/改**的，**必须**按本小节与 **§1.7.4** 记 `operation_log`，**不得**因批量大而省略或降级为「仅一条汇总」。
  - **部分成功、部分失败**时：对**确有变更的**成功行照常记；**不得**因同批存在失败行而**略过**有变更成功行应记的日志；**与**因「全行无改」而 0 条不冲突。
- **行内记哪些“列/字段”**：在**已判定存在 `UPDATE` 意义**的修改行上，**是否**为某字段记一条操作日志，**以落库后该字段相对变更前是否实际变化为准**（且仍满足 **§1.7.4**「仅页面展示、仅用户可编辑值」等约束）。**库值未变**的列**不记**；若**全行均不变**，则**上一款已整行 0 条**。**新增（CREATE）** 成功行对**有展示、且为本次经 Excel 写入**的字段按 **§1.7.4 新增** 记（真插入，**不**属「与库无差异的伪修改」）。
- **与页面规则对齐**：对**有日志产生**的每一成功行，**与**同一业务表上「页面手工保存」**完全同一套规则**——即按 **§1.7.4** 的 **新增 / 修改 / 删除** 记法，**不**另设「导入专用」简记规则。
- **行语义**与 **§1.6** 首行说明一致（以各模块主键列为准）：
  - **ID 为空且成功插入**（真新增）→ 按 **§1.7.4 新增（CREATE）**：对**页面有展示、且本次由 Excel 写入**的各字段逐字段记行（**系统生成、自动补全**仍不记，与 **§1.7.4** 一致）。
  - **ID 有值且成功更新** → 先按**上一款**判定是否**全行无改**（无改则 0 条日志）；有改时按 **§1.7.4 修改（UPDATE）**：**仅**对**实际发生变更、且页面有展示**的字段记 diff（**未变化列不记**）。
  - 若某模块扩展导入能力导致**逻辑删除/物理删除**等写操作，则按 **§1.7.4 删除（DELETE）** 及**逻辑删除**子款记，与页面点「删除」一致。
- **操作人/时间/产品/版本**：`operator_id`、`operated_at` 为**执行导入的当前用户**与请求时刻；`owned_product_id` / `owned_version_id` 与**当次导入所带上下文**及 **§1.7.2** 一致（参数域须带当前版本等）。
- **可选 `log_batch_id`（同 §1.7.4）**：同一上传文件/同一导入请求内产生的多行日志，**宜**共享同一 `log_batch_id`（或等价请求标识），便于对账，**不**改变「每字段/每删除对象」的行级记法要求。

#### 1.7.5 必须记录操作日志的代码范围

- **只记写、不记读**：**仅**对**产生业务数据变更**的接口记操作日志；**纯查询、列表 GET、只读下载模板**等**不记**。导出 Excel（§1.6）**不**写入操作日志（属只读类能力）。
- **所有业务写接口**：凡执行 **INSERT / UPDATE**（含全量/部分更新、行内编辑保存）及 **DELETE**（含逻辑删除）的应用服务或聚合持久化路径，在**同一事务成功提交前或提交后**（以团队事务策略为准，须保证**成功才记**或**与业务一致**）调用公共「操作日志」写入能力，**不得遗漏**本需求范围内的模块：命令、类型与区段、各配置主数据、产品形态、参数主数据与关联资源等（与 `function.md` 各 2.x 子模块一一对应，OpenSpec 任务清单拆项）。
- **业务数据导入（§1.6）**：按 **§1.7.4.1** —— **只记真实增/删/改**；**全行与库无差异的「修改」成功行 0 条日志**、**不**算用户覆盖；**有变更**的成功落库**必须**记，与**页面**粒度及 **§1.7.4** 一致；**失败行不记**；**禁止**用「单请求一条 audit」替代字段级/删除单条等要求；**不**得误解为「成功落库但无改」仍记。
- **批量接口**（非导入的多条提交）：**禁止**因批量而完全不留痕；仍须按 **§1.7.4** 对每条资源变更可追溯到与手工操作等价的记录策略。
- **操作日志能力本身**（本节的查询抽屉）：**不提供**对操作日志的 **Excel/CSV 导入、导出**（不导出审计表、不从文件回灌日志行）。

> **实现注记（非代码，仅设计）**：建议通过 **应用层可复用「操作日志应用服务」** + 各用例在成功路径显式调用，或 **AOP/拦截器** 仅作补充手段且须覆盖 MyBatis-Plus 之外的路径；**以可验收「全增删改可查到」**为准。

#### 1.7.5.1 查询权限（冻结）

- **查看操作日志**与具体业务对象的读权限**解耦**：凡能进入**当前产品/版本上下文的已登录用户**均可打开各页「操作日志」抽屉，**不**单设 `param:audit:read` 等权限点（与 **§1.5** 占位说明并存：操作日志不增加额外 IAM 项）。

#### 1.7.6 查询接口（后端）

- **用途**：各列表/配置页从抽屉拉取**当前表**、**当前产品**、**当前版本（若该页有版本上下文）**下的操作日志，**分页**（参数与 **§1.2** 对齐：`page` / `size` / 可选 `sort`）。
- **建议路径（示例）**：`GET /api/v1/operation-logs`，Query 至少包括：`productId`（= `owned_product_id`）、`bizTable`（= 物理表名，与 1.7.2 一致）、**`versionId`（= `owned_version_id`，与当前页版本子上下文一致；无版本页的查询传空或不传则仅按 `productId + biz_table` 过滤 `owned_version_id IS NULL` 或与后端约定全量，OpenSpec 冻结）**、可选 `resourceId`、时间范围等。
- **响应**：`ResponseObject<分页包装>`，每条记录含 **1.7.3** 所示对外字段，便于直接渲染 **1.7.7** 表格列。

#### 1.7.7 前端交互（抽屉）

| 项 | 约定 |
|----|------|
| **入口** | 在**主数据/配置列表**表格**上方**工具区域**最右侧**提供 **文字按钮「操作日志」**（`el-button` text 类型，风格见 `frontend-style.md` 文字按钮规范）。 |
| **展现** | 点击后自右侧打开 **抽屉（Drawer）**，不跳转路由；内嵌**表格**或简洁列表。 |
| **列（冻结）** | **操作类型**（展示：新增/修改/删除）｜**变更项**（删除场景为 **§1.7.4** 固定文案如「删除对象」）｜**原值**｜**新值**（删除场景为被删对象**名称**）｜**修改人**（**直接展示 `operator_id` 文本**）｜**修改时间**（**服务器时区**下格式化，与全站统一）。 |
| **空态** | 无日志时与全局空状态规范一致；未选产品时与 **spec-04-integration**「请先选择产品」等一致。 |
| **排序** | 默认按**修改时间**倒序（新在上）。 |

#### 1.7.8 OpenSpec 与实现阶段

- 本章作为 **平台级横切** 需求：建议单独 **OpenSpec**（如 `common-operation-log`）含：DDL（含 `owned_product_id` / `owned_version_id`）、`operation_log` 仓储、写入门面、统一查询 `Controller`、前端公共抽屉组件及在各列表页的接入任务；**不**含操作日志的导入/导出能力。
- **验收要点**：(1) 单表、按 `biz_table` + 产品 + 版本（可空）区分；(2) 任一页面对应模块的增/改/删在抽屉中可查到**符合 §1.7.4 语义**的内容；(3) **导入**仅**有实质增/删/改**的成功行产生日志；**与库完全一致的无改「修改」行**不产日志、**不**计用户覆盖；有日志行的粒度**符合 §1.7.4.1**、与**页面手工**一致；(4) 删除类仅**名称**级一条（或等效单条）即可；(5) 展示列含 **变更项**、**修改人为 ID**、**仅写操作有日志**、**查看无额外权限**。

### 1.8 ID 生成（公共）

- 业务侧 ID 前缀：`command_`、`customType_`（与表 `command_type_id` 对齐）、`range_`、`version_`、`category_`、`keyword_`、`ne_`、`nf_`、`effective_`、`feature_`、`changeDes_` 等（见 `function.md`）；**统一 `IdGenerator` 领域服务**或基础设施适配器，保证唯一性（DB 唯一索引兜底）。

### 1.9 DFX（全局）

| 维度 | 要求 |
|------|------|
| **性能** | 列表分页；避免 N+1；大数据导入异步；热点配置缓存只读。 |
| **可靠性** | 单条参数/单条配置保存使用事务；**批量导入**按 **§1.6** 行级成功/失败汇总，**禁止**因部分行失败而整单回滚已成功的行。 |
| **可维护** | 配置类 CRUD 抽象通用应用服务模板（非领域规则抽象）。 |

### 1.10 安全（全局）

- 租户隔离：`tenant_id`（`system_parameter`）与产品维度双重校验（若启用多租户）。
- 导入文件：类型校验、大小限制、防 XXE（Excel）、CSV 注入防护。
- **变更来源黑名单**：正则关键字校验（见 §13）防绕过。

### 1.11 页面与数据库「是否」类字段取值（冻结）

- 凡业务含义为「是否」的字段（示例：`export_delta`、参数表 `take_effect_immediately` 等）：**页面与数据库统一仅允许中文「是」「否」**；**禁止** `Y/N`、`yes/no`、英文混用（历史数据迁移若存在混用，需单独变更单与清洗脚本）。

---

## 2. 子域：命令配置（对应 2.1.1）

**章节 ID**：`SPEC-CMD-CONFIG`  
**聚合**：`Command`（根）— 表 **`entity_command_mapping`**

### 2.1 功能说明

- 按产品维护命令；支持新增、行/列编辑、导入、导出；**同一产品下命令名唯一**；存储 **`command_id`** 格式 `command_xxxxx` 自动生成。

### 2.2 页面字段与表字段映射

| 页面含义 | 表字段 | 类型/长度 | 必填 | 备注 |
|----------|--------|-----------|------|------|
| 归属产品 | `owned_product_id` | String 255 | 是 | 关联 `entity_basic_info.product_id`（见 `table字段简介.md` §2） |
| 命令 ID | `command_id` | String 255 | 是 | 系统生成 `command_*` |
| 命令名 | `command_name` | String 255 | 是 | 产品内唯一 |
| 状态 | `command_status` | Integer | 否 | |
| 责任人 | `owner_list` | String 255 | 是 | 逗号分隔 |
| 创建/更新 | `creator_id`、`creation_timestamp`、`updater_id`、`update_timestamp` | | | |

> **说明**：`table字段简介.md` §2 **未**包含命令描述、最小/最大支持版本等扩展列；若产品需在界面展示此类信息，须走表结构变更或扩展表，不在本文档假定列内。

### 2.3 业务规则（领域）

- **不变量**：同一 `owned_product_id` 下 `command_name` 唯一（聚合内校验 + DB 唯一索引）。
- **command_id**：创建时生成，不可由用户随意改（若允许改需单独流程）。

### 2.4 接口设计（REST 示例）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/products/{productId}/commands` | 分页列表 |
| POST | `/api/v1/products/{productId}/commands` | 新增 |
| PUT | `/api/v1/products/{productId}/commands/{commandId}` | 全量更新 |
| DELETE | `/api/v1/products/{productId}/commands/{commandId}` | 删除/禁用策略按产品定 |
| POST | `/api/v1/products/{productId}/commands/import` | 导入 |
| GET | `/api/v1/products/{productId}/commands/export` | 导出 |

### 2.5 异常与错误码（示例）

| code / message | 场景 |
|----------------|------|
| `CMD_DUPLICATE_NAME` | 命令名重复 |
| `CMD_NOT_FOUND` | 命令不存在或不属于该产品 |

### 2.6 权限点

- `param:command:read`、`param:command:write`、`param:command:import`、`param:command:export`

### 2.7 实现方案（分层）

- **领域**：`Command` 实体校验名称唯一；`CommandRepository`。
- **应用**：`CommandApplicationService` 事务、编排、调用导入导出组件。
- **基础设施**：`CommandMapper`（MyBatis-Plus）。

### 2.8 测试方案与用例（摘要）

| 用例 ID | 场景 | 预期 |
|---------|------|------|
| TC-CMD-001 | 同产品重复命令名 | 失败并提示 |
| TC-CMD-002 | 正常新增 | `command_id` 前缀正确 |
| TC-CMD-003 | 导入模板列错位 | 行校验失败 |

---

## 3. 子域：类型及范围配置（对应 2.1.2）

**章节 ID**：`SPEC-CMD-TYPE-DEF`  
**聚合**：`CommandTypeDefinition` — 表 **`command_type_definition`**

### 3.1 功能说明

- 配置命令对应的**参数类型**及**序号范围**；通过 **`owned_command_id`** 关联命令；支持导入导出与编辑。
- “类型枚举”选项为全局只读字典：来源 `type_bit_dict`（接口 GET `/api/v1/type-bits`）；前端下拉仅展示枚举文本，不展示内部 ID。
- **规则**：**自定义类型 ID（`command_type_id`）** 全局唯一；**`command_type_name` 在同一产品+命令维度下唯一**（与业务确认后落唯一索引）；存储 ID 形态可与需求 `customType_*` 对齐（实现时在字段映射说明中定义）。

### 3.2 页面字段与表字段映射（`command_type_definition` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | 主键语境之一 |
| `owned_command_id` | 归属命令 ID | String 255 | 是 | 关联 `entity_command_mapping.command_id` |
| `command_type_id` | 自定义类型 ID | String 255 | 是 | 唯一标识；对应需求 `customType_*` |
| `command_type_name` | 自定义类型名称 | String 255 | 是 | |
| `command_type` | 类型枚举 | Enum 50 | 是 | 来自 `type_bit_dict.type_enum`，默认：BIT/BYTE/DWORD/STRING/INT |
| `min_value` | 最小序号 | Integer | 是 | 全局序号下界 |
| `max_value` | 最大序号 | Integer | 是 | 全局序号上界 |
| `occupied_serial_number` | 占用序号 | String 512 | 否 | IT/占用展示 |
| `command_type_status` | 状态 | Integer | 否 | |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

**说明**：与命令的关联以 **`owned_product_id` + `owned_command_id`** 表达；版本维度的进一步约束见 §4。

### 3.3 业务规则

- 自定义类型名称唯一性（建议：**产品 + 命令** 维度，与表「归属」一致）。
- 序号区间与后续「版本区段」校验衔接（§4）。

### 3.4 接口设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/products/{productId}/command-types` | 分页；可筛命令（`owned_command_id` / `commandId`） |
| POST | `/api/v1/products/{productId}/command-types` | 新增 |
| PUT | `/api/v1/products/{productId}/command-types/{commandTypeId}` | 更新（路径变量为 `command_type_id`） |

### 3.5 异常 / 权限 / 实现 / 测试

- **异常**：`TYPE_NAME_DUPLICATE`、`TYPE_ID_CONFLICT`（`command_type_id` 冲突）
- **权限**：`param:command-type:*`
- **实现**：充血模型校验区间合法（值对象 `ValueRange`）；仓储持久化。
- **测试**：同名冲突、区间边界。

---

## 4. 子域：类型按版本区段划分（对应 2.1.3）

**章节 ID**：`SPEC-CMD-TYPE-RANGE`  
**聚合**：`CommandTypeVersionRange` — 表 **`command_type_version_range`**

### 4.1 功能说明

- 产品-命令-类型维度下，按**版本区段**限制**序号**范围；支持版本差异对比视图（前端）。
- **规则**：区段序号范围须在 §3 全局 `min_value`～`max_value` 内；同一产品+命令+类型+业务/版本维度下**区段不重叠**（细则与索引以 DDL 为准）；`range_xxxxx` / 表字段 `version_range_id`。
- **关联**：通过 `owned_type_id` 关联 §3 的 **`command_type_id`**；`owned_version_or_business_id` 承载归属版本或业务类型 ID（见表备注）。

### 4.2 页面字段与表字段映射（`command_type_version_range` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `owned_command_id` | 归属命令 ID | String 255 | 是 | |
| `owned_type_id` | 归属类型 | String 255 | 是 | 对应 §3 `command_type_id` |
| `version_range_id` | 区段 ID | String 255 | 是 | 唯一 `range_*` |
| `start_index` | 起始序号 | Integer | 是 | 区段内序号下界 |
| `end_index` | 结束序号 | Integer | 是 | 区段内序号上界 |
| `range_description` | 说明 | String 512 | 否 | |
| `range_type` | 区段划分类型 | String 50 | 否 | |
| `owned_version_or_business_id` | 归属版本 ID / 归属业务类型 ID | String 255 | 是 | 表字段二义合一，见 `table字段简介.md` §4 |
| `range_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 4.3 接口设计

| 方法 | 路径 |
|------|------|
| GET | `/api/v1/products/{productId}/command-type-version-ranges` |
| POST | `/api/v1/products/{productId}/command-type-version-ranges` |
| PUT | `/api/v1/.../command-type-version-ranges/{rangeId}` |

### 4.4 异常 / 测试

- **异常**：`RANGE_OUT_OF_TYPE_BOUNDS`、`RANGE_OVERLAP`
- **测试**：与 §3 区间嵌套、版本重叠检测。

---

## 5. 子域：产品版本配置（对应 2.2.1）

**章节 ID**：`SPEC-VERSION`  
**聚合**：`ProductVersion` — 表 **`entity_version_info`**

### 5.1 功能说明

- 版本增删改查；**新增版本**、**拉取分支**（选基线版本，可选复制参数）。
- **规则**：同一产品下 `version_name` 唯一；`version_id` = `version_xxxxx`。

### 5.2 页面字段与表字段映射（`entity_version_info` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | 关联 `entity_basic_info.product_id` |
| `version_id` | 版本 ID | String 255 | 是 | 唯一 `version_*` |
| `version_name` | 版本名称 | String 255 | 是 | 同一产品下唯一 |
| `version_type` | 版本类型 | Enum 50 | 是 | |
| `version_description` | 说明 | String 512 | 否 | |
| `baseline_version_id` | 基线版本 ID | String 255 | 否 | |
| `baseline_version_name` | 基线版本名称 | String 255 | 否 | |
| `version_desc` | 版本描述 | String 1024 | 否 | 长描述 |
| `approver` | 审批人 | String 50 | 否 | |
| `is_hidden` | 是否隐藏版本 | String 50 | 否 | |
| `supported_version` | 支持版本 | String 50 | 否 | |
| `version_status` | 版本状态 | Integer | 是 | |
| `introduced_product_id` | 引入产品 ID | String 255 | 否 | |
| `owner_list` | 责任人 | String 255 | 是 | |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 5.3 接口设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/products/{productId}/versions` | 列表 |
| POST | `/api/v1/products/{productId}/versions` | 新增 |
| POST | `/api/v1/products/{productId}/versions/branch` | 拉分支 + 是否复制参数 |
| PUT | `/api/v1/products/{productId}/versions/{versionId}` | 更新 |

### 5.4 异常 / 权限 / 测试

- **异常**：`VERSION_NAME_DUPLICATE`
- **测试**：分支复制后参数条数一致（若选同步）。

---

## 6. 子域：参数业务分类配置（对应 2.2.2）

**章节 ID**：`SPEC-BIZ-CATEGORY`  
**表**：`entity_business_category`

### 6.1 功能说明

- 分类 CRUD、导入导出；同一产品下**分类名称（中文）唯一**；`category_xxxxx`。

### 6.2 页面字段与表字段映射（`entity_business_category` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | 主键语境 |
| `category_id` | 业务分类 ID | String 255 | 是 | 唯一标识 `category_*` |
| `category_name_cn` | 分类名称（中文） | String 255 | 是 | 建议同一产品下唯一（与索引一致） |
| `category_name_en` | 分类名称（英文） | String 512 | 是 | |
| `feature_range` | 包含特性范围 | String 512 | 否 | |
| `category_type` | 所属类别 | String 50 | 否 | |
| `category_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 6.3 接口 / 异常 / 测试

- REST：`/api/v1/products/{productId}/business-categories` 资源集合。
- **异常**：`CATEGORY_NAME_DUPLICATE`（建议以 **`category_name_cn`** 为唯一约束口径）

---

## 7. 子域：参数变更来源关键字（对应 2.2.3）

**章节 ID**：`SPEC-CHANGE-KEYWORD`  
**表**：`config_change_source_keyword`

### 7.1 功能说明

- 关键字 CRUD、导入导出；**正则合法性校验**；关键字业务 ID 形如 `keyword_xxxxx`（表字段为 **`keyword_id`**）。

### 7.2 页面字段与表字段映射（`config_change_source_keyword` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | 关联 `entity_basic_info` |
| `keyword_id` | 关键字正则 ID | String 255 | 是 | 同一产品下唯一，可 `keyword_*` |
| `keyword_regex` | 关键字正则 | String 512 | 是 | 黑名单校验用正则 |
| `reason` | 原因 | String 512 | 是 | |
| `keyword_status` | 状态 | Integer | 是 | 1 启用 / 0 禁用 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

与 **`docs/table字段简介.md` §7** 一致；按产品路径访问时 `{productId}` 与 `owned_product_id` 对齐。

### 7.3 业务规则

- 保存前 `Pattern.compile(regex)` 校验；用于 §13 变更来源**黑名单**校验。

### 7.4 接口 / 测试

- `/api/v1/products/{productId}/change-source-keywords`
- **测试**：非法正则、命中黑名单拒绝保存参数

---

## 8. 子域：参数适用网元配置（对应 2.2.4）

**章节 ID**：`SPEC-NE`  
**表**：`entity_applicable_ne_dict`

### 8.1 功能说明

- 网元类型 CRUD、导入导出；同一产品 **网元名称唯一**；`ne_xxxxx` / `ne_type_id`。

### 8.2 页面字段与表字段映射（`entity_applicable_ne_dict` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `ne_type_id` | ID | String 255 | 是 | 唯一 `ne_*` |
| `ne_type_name_cn` | 适用网元名称 | String 50 | 是 | 产品内唯一 |
| `ne_type_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `product_form` | 产品形态 | String 255 | 否 | 多选 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 8.3 接口 / 异常

- `/api/v1/products/{productId}/applicable-nes`
- **异常**：`NE_NAME_DUPLICATE`

---

## 9. 子域：参数 NF 配置（对应 2.2.5）

**章节 ID**：`SPEC-NF`  
**表**：`entity_nf_config_dict`

### 9.1 功能说明

- NF CRUD、导入导出；产品内 **NF 名称唯一**；`nf_xxxxx`。

### 9.2 页面字段与表字段映射（`entity_nf_config_dict` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `nf_config_id` | ID | String 255 | 是 | 唯一 `nf_*` |
| `nf_config_name_cn` | nf 名称 | String 255 | 是 | 产品内唯一 |
| `nf_config_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 9.3 接口

- `/api/v1/products/{productId}/nf-configs`

---

## 10. 子域：参数生效方式配置（对应 2.2.6）

**章节 ID**：`SPEC-EFFECTIVE-MODE`  
**表**：`entity_effective_mode_dict`（及需求若含「形态」：`entity_effective_form_dict`）

### 10.1 功能说明

- 生效方式 CRUD、导入导出；`effective_xxxxx`。

### 10.2 页面字段与表字段映射

**`entity_effective_mode_dict`（生效方式）全量**

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `effective_mode_id` | ID | String 255 | 是 | 唯一 `effective_*` |
| `effective_mode_name_cn` | 生效方式（中文） | String 255 | 是 | |
| `effective_mode_name_en` | 生效方式（英文） | String 512 | 是 | |
| `effective_mode_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

**`entity_effective_form_dict`（生效形态，表结构存在且可并行维护）**：`effective_form_id`、`effective_form_name_cn`、`effective_form_name_en`、`effective_form_status` 及标准审计字段；路径示例 `/api/v1/products/{productId}/effective-forms`。

### 10.3 接口

- `/api/v1/products/{productId}/effective-modes`

---

## 11. 子域：版本特性配置（对应 2.2.7）

**章节 ID**：`SPEC-VERSION-FEATURE`  
**表**：`version_feature_dict`

### 11.1 功能说明

- 选择**版本**后维护特性；表格上模糊查特性名；**特性中文名唯一标识**（产品+版本范围内约定）；`feature_xxxxx`。

### 11.2 页面字段与表字段映射（`version_feature_dict` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `owned_version_id` | 归属版本 ID | String 255 | 是 | 关联 `entity_version_info` |
| `feature_id` | ID | String 255 | 是 | 唯一 `feature_*` |
| `feature_code` | 特性编码 | String 255 | 是 | **后端生成**；页面不展示、不要求输入 |
| `feature_name_cn` | 中文名称 | String 512 | 是 | 唯一标识（业务规则） |
| `feature_name_en` | 英文名称 | String 1024 | 是 | |
| `introduce_type` | 引入类型 | String 50 | 是 | 版本新增/继承/引用/其他 |
| `inherit_reference_version_id` | 继承/引用版本 ID | String 255 | 否 | |
| `feature_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用；页面不展示，删除写 `0` 且列表过滤 `0` |
| `introduced_product_id` | 引入产品 ID | String 255 | 否 | |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 11.3 接口

- GET `/api/v1/products/{productId}/versions/{versionId}/features?keyword=`

### 11.4 异常

- `FEATURE_NAME_DUPLICATE`

---

## 12. 子域：项目组配置（对应 2.2.8）

**章节 ID**：`SPEC-PROJECT-TEAM`  
**表**：`project_team_dict`

### 12.1 功能说明

- 项目组 CRUD；**人员与项目组关联**、**项目组权限** — 表结构仅含项目组与 `owner_list`；**人员关联/权限**需扩展表或对接 IAM，建议单独 OpenSpec「项目组与成员关系」。

### 12.2 页面字段与表字段映射（`project_team_dict` 全量）

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `owned_product_id` | 归属产品 ID | String 255 | 是 | |
| `team_id` | ID | String 255 | 是 | 唯一 |
| `team_name` | 项目组名称 | String 512 | 是 | |
| `team_status` | 状态 | Integer | 否 | 1 启用 / 0 禁用 |
| `owner_list` | 责任人 | String 255 | 是 | |
| `creator_id` | 创建人 | String 50 | 是 | |
| `creation_timestamp` | 创建时间 | DateTime | 是 | |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |

### 12.3 接口

- `/api/v1/products/{productId}/project-teams`

---

## 13. 子域：参数管理（核心）（对应 2.3）

**章节 ID**：`SPEC-PARAMETER-CORE`  
**聚合**：`Parameter`（根）、`ParameterChangeDescription`（实体）  
**表**：`system_parameter`、`config_change_description`、`config_change_type`

### 13.1 功能说明

- 左侧树：命令 → 类型；版本选择器；右侧参数表：分页 20、工具栏新增/导入/导出/同步(暂缓)/基线。
- **版本视图补充（冻结，可验收）**：
  - 版本下拉需包含一个 **ALL（全产品）** 选项。
  - 选择具体版本：右侧列表仅展示该版本参数（`owned_version_id = versionId`）。
  - 选择 **ALL**：右侧列表展示该产品下所有版本的参数；列表需额外展示 **所属版本（`owned_version_id`）** 列，便于定位与后续编辑。
  - **ALL 视图限制**：由于保存/导入/导出均依赖具体 `{versionId}` 路径，ALL 视图下应禁用或隐藏这些入口；行级操作若保留（编辑/删除/基线）必须使用行内 `owned_version_id` 作为目标版本。
- **参数同步**：第一版不实现。
- **基线**：基线动作仅修改 `data_status`（枚举新增 **已基线**）；基线后页面「修改/删除/导入」等入口置灰，后端拒绝写操作。

### 13.2 页面字段与 `system_parameter` 表映射（严格按表补齐）

本节分三部分：**(A)** 与 `function.md` **§2.3.4 新增表单字段展示规则**一致的**页面交互**（只读、悬停提示、控件类型等），避免仅映射表字段而丢失前端实现要求；**(B)** 字段与表列对照摘要；**(C)** 表结构权威引用与关联子表。

#### 13.2.A 新增/编辑表单：页面交互与落库字段（对齐 `function.md`）

> 样式与组件库遵循 `docs/frontend-style.md`（Element Plus / 通用 UI）；**悬停提示**可用 `el-tooltip` / `el-popover`；**英文默认不手工填**可由自动翻译服务填充，前端展示为只读或占位文案。

| # | 需求描述（function） | 主要落库字段 | 页面实现要点 |
|---|----------------------|--------------|--------------|
| 1 | 参数名称（中文） | `parameter_name_cn` | 框内**默认提示**为「参数标题」语义；**悬停**弹出提示框，内容为**示例**（样例1 / 样例2，多行） |
| 2 | 参数名称（英文） | `parameter_name_en` | **不要求用户手填**；展示自动结果或占位；若可编辑则仅作校对场景 |
| 3 | 参数编码（内部） | `parameter_code` | 存储格式：`类型_序号`（如 `BIT_8`、`DWORD_1`）。**后端生成**：由页面选择的“类型枚举 + 序号”拼装；页面不展示、不允许手工输入。序号合法范围来源于「命令-类型-版本区段」。 |
| 4 | 取值范围 | `value_range` | 文本输入；**不做格式校验**（按业务约定填写）。 |
| 5 | 使用 BIT 位 | `bit_usage` | **由 `type_bit_dict.bit_count` 驱动**：仅当当前类型 `bit_count > 0` 时展示 BIT 勾选（范围 `1..bit_count`）并要求填写；当 `bit_count = 0`（如 INT）时不展示且不校验。存储为英文逗号分隔（如 `1,2,3`）；**存储内容不做去重规范化**（业务上同一行不出现重复序号）。页面标签展示为 BIT1、BIT2…。同一**序号**若存在多行参数：各行所选 BIT 集合**两两不相交**（跨行不得重复占用同一 BIT）；单行内亦不得重复。 |
| 6 | 取值说明（中文） | `value_description_cn` | **悬停**提示框给样例 |
| 7 | 取值说明（英文） | `value_description_en` | **不手填英文** + **悬停**样例（同 function） |
| 8 | 应用场景（中文） | `application_scenario_cn` | **悬停**样例 |
| 9 | 应用场景（英文） | `application_scenario_en` | **不手填** + **悬停**样例 |
| 10 | 参数默认值 | `parameter_default_value` | **仅允许数字**（int） |
| 11 | 参数推荐值 | `parameter_recommended_value` | **仅允许数字**（int） |
| 12 | 适用网元 | `applicable_ne` | **下拉**，数据源：当前产品 §8 网元配置；存库可为顿号分隔文本（与表一致） |
| 13 | 所属特性 | `feature` | **下拉**，数据源：当前产品 + **当前版本** §11 特性 |
| 14 | 所属特性 ID（内部） | `feature_id` | **禁止编辑**，随特性选择**自动带出**；页面不展示 |
| 15 | 业务分类 | `business_classification`、`category_id` | **下拉**，数据源：当前产品 §6 |
| 16 | 参数是否立即生效 | `take_effect_immediately` | **下拉**：是/否 |
| 17 | 生效方式（中文） | `effective_mode_cn` | **下拉**，数据源：当前产品 §10 |
| 18 | 生效方式（英文） | `effective_mode_en` | **禁止编辑**，随中文**自动带出** |
| 19 | 项目组 | `project_team` | **下拉**，数据源：当前产品 §12 |
| 20 | 归属模块 | `belonging_module` | **文本输入**，用户自填 |
| 21 | 变更来源 | `change_source` | **文本输入**；**不对内容做 trim 后再保存**（除非业务另行约定）。**黑名单触发条件**：当且仅当字符串中存在**至少一个非空白字符**时才校验；若 **Java `String.isBlank()` 为 true**（整串仅空白），视为无效输入——**不触发黑名单校验**，落库建议 **`null` 或 `""`**（OpenSpec 冻结其一）。当存在非空白字符时，**整串原样持久化**（**保留首尾空格与串内空格**）。校验时拉取启用 `keyword_regex`；**任一命中即拒绝**；响应携带**命中的 `keyword_regex` 原文**（见 §13.3）。 |
| 22 | 版本号 | `patch_version` 或与版本字段映射以表为准 | 用户填写；**默认带出当前版本**等规则由产品确认；格式 function 写为 `xxx` |
| 23 | 引入版本 | `introduced_version` | function 标 **TODO**，落地前需与表字段、版本域统一 |
| 24 | 参数含义（中文） | `parameter_description_cn` | 多行；**悬停**样例 |
| 25 | 参数含义（英文） | `parameter_description_en` | 多行；**不手填英文**（默认/翻译） |
| 26 | 影响说明（中文） | `impact_description_cn` | 多行；**悬停**样例 |
| 27 | 影响说明（英文） | `impact_description_en` | 多行输入 |
| 28 | 配置举例（中文） | `configuration_example_cn` | 多行；**悬停**样例 |
| 29 | 配置举例（英文） | `configuration_example_en` | 多行 |
| 30～31 | 关联参数描述 | `related_parameter_description_cn` / `en` 等 | 多行 |
| 32 | 备注 | `remark` | 文本 |

**序号与 BIT 交互（function 补充要求）**

- **可用序号**：后端返回当前类型在区段范围内的全部序号，并标识每个序号「完全可用 / 部分可用」（部分可用指该序号下仍存在未占用 BIT 位）。
- **BIT 勾选 UI（冻结）**：按类型展示 BIT1…（BIT：1 个；BYTE：1～8；DWORD/STRING：1～32）。布局上**每行排列 4～6 个**复选框为宜。对某一序号：**已被占用的 BIT** 在界面上表现为**勾选且置灰（不可改）**；**未占用且仍可用的 BIT** 为**未勾选、可点击**。后端对该序号仅需返回 **「当前仍可用的 BIT 序号列表」**（例如 `[1,5,6]`），前端据此与全量 BIT 列表合并渲染；**不必**在接口中返回已占用列表（可用「全集 − 可用」推导置灰态，若需减轻前端计算也可额外返回 `occupiedBits`，属实现可选）。

#### 13.2.B 表单下方「变更说明」子表（页面 + 落库）

- **位置**：表单最下方；**可编辑表格**，**双击**单元格编辑（见 `function.md`）。
- **落库**：`config_change_description` 多条记录关联 `parameter_id`；每条 `change_description_id` = `changeDes_xxxxx` 系统生成；**`entity_unique_id`、`version_id`** 按当前产品/版本上下文**由后端自动写入**（与 `table字段简介.md` §16 一致，通常不对用户单独展示列）。
- **列与交互**：

| 列（function） | 落库字段 | 页面规则 |
|----------------|----------|----------|
| 变更类型 | `change_type` | **下拉**；**新增**表单仅可选「新增参数」；**编辑**表单可选：新增参数、修改参数含义、修改参数取值范围、修改关联参数、修改默认值、修改推荐值、修改适用网元、修改生效方式、修改参数取值说明 等（与字典 `config_change_type` 对齐） |
| 变更原因（中/英）、变更影响（中/英） | `change_reason_*`、`change_impact_*` | 文本；四格均需按表结构校验长度 |
| 是否导出 | `export_delta` | 页面与数据库**仅允许**中文 **「是」/「否」**（见 §1.11）。选「否」时 **不导出原因** 必填 |
| 不导出原因 | `no_export_reason` | 条件必填 |

- **校验**：每次保存（新增/修改）**至少一行**变更说明；各列必填规则见 function。

#### 13.2.C 字段与表对照摘要（便于检索）

| function 表单项 | 表字段 | 备注 |
|-----------------|--------|------|
| 参数名称（中文） | `parameter_name_cn` | 交互见 §13.2.A 行 1 |
| 参数名称（英文） | `parameter_name_en` | 交互见 §13.2.A 行 2 |
| 参数 ID | `parameter_code` | `类型_序号`，序号来自区段范围；交互见 §13.2.A 行 3 |
| 取值范围 | `value_range` | 不做格式校验；交互见 §13.2.A 行 4 |
| 使用 BIT 位 | `bit_usage` | 逗号分隔 BIT 位序号；交互见 §13.2.A 行 5 |
| 取值/场景/含义/影响/配置举例等中英文 | `value_description_*`、`application_scenario_*`、`parameter_description_*`、`impact_description_*`、`configuration_example_*` | 悬停样例见 §13.2.A |
| 默认/推荐值 | `parameter_default_value`、`parameter_recommended_value` | 仅数字 |
| 适用网元 | `applicable_ne` | 下拉；顿号分隔落库 |
| 所属特性 / ID | `feature`、`feature_id` | 特性 ID 只读自动填 |
| 业务分类 / ID | `business_classification`、`category_id` | §6 |
| 立即生效 | `take_effect_immediately` | 下拉 |
| 生效方式中/英 | `effective_mode_cn`、`effective_mode_en` | 英文只读自动填 |
| 项目组 | `project_team` | §12 |
| 归属模块 | `belonging_module` | 手输 |
| 变更来源 | `change_source` | 黑名单 |
| 补丁版本 / 引入版本 | `patch_version`、`introduced_version` | 引入版本 TODO |
| 关联参数与描述 | `related_parameter_*`、`related_parameter_description_*` | |
| 备注 | `remark` | |
| 枚举/单位/范围等 | `enumeration_values_*`、`parameter_unit_*`、`parameter_range` 等 | 表预留 |
| 数据状态 | `data_status` | Draft/Inwork/… |
| 租户/空间 | `tenant_id`、`domain_id` | |
| 引入类型 | `introduce_type`、`inherit_reference_version_id` | |

#### 13.2.D `system_parameter` 表字段全量

**共 74 个业务字段（含审计）**，定义以 **`docs/table字段简介.md` §15「表 15」** 为唯一权威来源（字段序号 1～74）；实施 DDL、MyBatis-Plus 实体与 OpenSpec **逐列对齐**，本文不重复誊写以避免漂移。

#### 13.2.E `config_change_description` 表字段全量

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `parameter_id` | 参数 ID | 与主表一致 | 是 | 关联 `system_parameter.parameter_id`（类型以 §0.4、`table字段简介.md` §15～§16 与 DDL 为准） |
| `change_description_id` | 变更说明 ID | String 255 | 是 | 唯一 `changeDes_*` |
| `change_type` | 变更类型 | String 255 | 是 | 关联 `config_change_type` |
| `change_reason_cn` | 变更原因（中文） | String 1024 | 是 | |
| `change_impact_cn` | 变更影响（中文） | String 1024 | 是 | |
| `change_reason_en` | 变更原因（英文） | String 1024 | 是 | |
| `change_impact_en` | 变更影响（英文） | String 1024 | 是 | |
| `export_delta` | 是否导出 delta | String 50 | 是 | 仅 **「是」/「否」**，与 UI 一致 |
| `no_export_reason` | 不导出原因 | String 1024 | 否 | 否导出时必填 |
| `updater_id` | 修改人 | String 50 | 否 | |
| `update_timestamp` | 修改时间 | DateTime | 否 | |
| `entity_unique_id` | 归属实体唯一标识 | String 255 | 是 | 关联 `entity_basic_info`（与 `table字段简介.md` §16 一致） |
| `version_id` | 归属版本标识 | String 255 | 是 | 关联 `entity_version_info` |

#### 13.2.F `config_change_type` 表字段全量

| 字段名 | 中文名称 | 类型/长度 | 必填 | 备注 |
|--------|----------|-----------|------|------|
| `change_type_id` | ID | Integer | 是 | 唯一 |
| `change_type_name` | ChangeType | String 100 | 是 | |
| `change_type_name_cn` | DeltaType_CN | String 100 | 是 | |
| `change_type_name_en` | DeltaType_EN | String 100 | 是 | |
| `change_sequence` | DeltaSeq | Integer | 是 | 排序 |

**系统初始化默认数据（与 `table字段简介.md` §17 一致）**

字典表在**系统初始化时自动插入**以下 **17** 条记录（`change_type_id` **无 4**，与表文档保持一致）；后续可由管理员维护，但**不得**随意改动 ID 以免与已落库的 `config_change_description.change_type` 关联断裂（若需调整须迁移脚本）。

| `change_type_id` | `change_type_name` | `change_type_name_cn` | `change_type_name_en` | `change_sequence` |
|------------------|--------------------|-------------------------|------------------------|---------------------|
| 1 | 新增参数 | 新增参数 | parameter added | 1 |
| 2 | 删除参数 | 删除参数 | parameter deleted | 2 |
| 3 | 修改参数含义 | 修改参数含义 | parameter function modified | 3 |
| 5 | 修改参数取值范围 | 修改参数取值范围 | Value range modified | 5 |
| 6 | 修改关联参数 | 修改关联参数 | Associated parameter modified | 6 |
| 7 | 修改参数默认值 | 修改参数默认值 | Default value modified | 7 |
| 8 | 修改参数推荐值 | 修改参数推荐值 | Recommended value modified | 8 |
| 9 | 修改适用网元 | 修改适用网元 | Applicable NEs modified | 9 |
| 10 | 新增 | 新增 | New | 12 |
| 11 | 修改 | 修改 | Modified | 13 |
| 12 | 删除 | 删除 | Deleted | 14 |
| 13 | 修改参数是否可见 | 修改参数是否可见 | Modify whether the soft parameter is visible | 15 |
| 14 | 修改是否可见 | 修改是否可见 | Whether the modification is visible | 16 |
| 15 | 修改应用场景 | 修改应用场景 | Modified application scenarios. | 10 |
| 16 | 修改生效方式 | 修改生效方式 | Modified the effective mode. | 11 |
| 17 | 修改参数取值说明 | 修改参数取值说明 | Value Description modified | 4 |

### 13.3 业务规则（领域核心）

- **参数 ID 与 BIT 占用**：按 `function.md` 2.3.4（BIT/BYTE/DWORD/STRING 不重复与占用规则）；领域服务 **`ParameterAllocationService`** 计算可用序号与 BIT 占用；保存前加载**当前版本下全部参数**做校验。
- **`parameter_code` 不变量（冻结）**：`parameter_code` 必须为 `类型_序号`，且其中 **序号等于用户选择的序号**；保存时拒绝「所选序号」与 `parameter_code` 不一致的提交。
- **`bit_usage`（冻结）**：英文逗号分隔 BIT 序号；**不做存储侧去重**（业务保证单行不重复）。**同一序号多行**时，各行 BIT 集合**互不相交**；单行内 BIT 不重复。
- **变更说明**：每次保存至少一条；子表 `config_change_description`，`change_description_id` = `changeDes_xxxxx`；变更类型来自 `config_change_type`。
- **变更说明表字段**：`change_type`、`change_reason_cn`、`change_impact_cn`、`change_reason_en`、`change_impact_en`、`export_delta`、`no_export_reason`（与表 `export_delta`、`no_export_reason` 对应）；通过 `parameter_id` 关联参数主表；并写入 **`entity_unique_id`、`version_id`**（见 §13.2.E）。
- **变更来源黑名单（冻结规则）**：
  - **触发条件**：当 `change_source` **含至少一个非空白字符**时执行校验；当 **`String.isBlank(changeSource)==true`** 时**不校验**（视为无有效内容），落库 **`null` 或 `""`**（OpenSpec 冻结其一）。
  - **持久化**：只要存在非空白字符，**不对字符串做 trim**，**整串原样入库**（含首尾空格、含中间空格）。
  - **匹配**：对当前产品下 `keyword_status=1` 的每条 `keyword_regex` 使用包含匹配（Java：`Pattern.compile(keywordRegex).matcher(changeSource).find()`）；**任意一条命中即失败**。
  - **响应（冻结）**：`success=false` 且 HTTP **500**；`message` 为可读提示，且**必须包含命中规则对应的 `keyword_regex` 原文**（可直接拼接进提示文案）；同时在 `data` 给出结构化字段（示例）`{ "violatedKeywordRegex": "<正则原文>" }`（OpenSpec 冻结字段名），前端优先展示该原文以便用户对照修改。

### 13.4 接口设计（示例）

| 方法 | 路径 |
|------|------|
| GET | `/api/v1/products/{productId}/versions/{versionId}/parameters` | 分页 + 树筛选 |
| GET | `/api/v1/products/{productId}/parameters` | **ALL（全产品）**：分页 + 树筛选（不按版本过滤） |
| GET | `/api/v1/products/{productId}/versions/{versionId}/parameters/available-sequences` | 当前类型下可选序号及「完全/部分可用」标记 |
| GET | `/api/v1/products/{productId}/versions/{versionId}/parameters/available-bits` | Query：`commandId`、`commandTypeId`（对应 `command_type_id`）、`sequence`（序号）；返回**当前仍可选 BIT 序号列表**（见 §13.2.A） |
| POST | `/api/v1/products/{productId}/versions/{versionId}/parameters` | 新增 |
| PUT | `/api/v1/products/{productId}/versions/{versionId}/parameters/{parameterId}` | 更新 |
| DELETE | `.../parameters/{parameterId}` | |
| POST | `.../parameters/import`（Query 见 **§13.4.2**）、`GET .../export` | 导入结果结构遵循 **§1.6**；**参数导入语义**以 **§13.4.2** 为准 |
| POST | `.../parameters/{parameterId}/baseline` | 基线 |
| GET | `/api/v1/products/{productId}/versions/{versionId}/parameters/baseline-count` | 版本维度已基线数量 |
| GET | `/api/v1/products/{productId}/parameters/baseline-count` | 产品维度已基线数量（ALL 视图） |

#### 13.4.1 `available-bits` 响应示例（字段名在 OpenSpec 冻结）

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "sequence": 1,
    "availableBitIndexes": [3, 4, 5, 9]
  }
}
```

说明：`availableBitIndexes` 为**仍允许新参数选用的 BIT 序号**（1～32 范围内，按类型裁剪）；前端与 BIT1…BITn 对齐渲染；已占用 BIT 由「类型全集 − available」推导为置灰勾选。

#### 13.4.2 参数导入 / 模板（与当前实现对齐）

本节描述**参数管理**导入在代码中的实际行为，作为 §1.6 通用约定在本子域的落地说明；与 **导出列、新增/编辑主表字段** 对齐（中文表头，见 `ParameterAppService.parameterExportHeadersZh()`）。

| 项 | 约定（实现） |
|----|----------------|
| **HTTP** | `POST` `.../versions/{versionId}/parameters/import`，`Content-Type: multipart/form-data`，表单字段名 **`file`**（Excel 首行表头、次行起数据，与下载模板一致）。 |
| **Query 参数** | **`mode`**：必填，`FULL`（全量）或 `INCREMENTAL`（增量）。<br>**`commandId`**：必填，表示导入落库到**该命令**（`system_parameter.owned_command_id`）。<br>**`commandTypeCode`**：可选；若传入，**作用域**在「当前产品 + 当前版本 + 该命令」基础上再限制为**参数编码**以 `{commandTypeCode}_` 为前缀（与左侧树选中**类型**一致）；全量删除、增量冲突校验均在此范围内进行。 |
| **与「已基线数量」** | 导入能力**不**再依赖「当前版本已基线参数数 = 0」；用户可在弹窗选择 **全量 / 增量** 模式（前端 `ParameterImportMode`）。 |
| **FULL 全量** | 在**上述作用域**内先**删除**已有参数行（写删除日志、删 `config_change_description` 子表再删主表），再按文件**逐行**处理；因库内已无旧行，文件每行在效果上为**新增**（仍逐行做 BIT 冲突等校验）。 |
| **INCREMENTAL 增量** | **已基线**（`data_status` 为已基线锁定）的匹配行：**跳过修改**，在结果中记为失败/跳过类提示（不覆盖）。未匹配到已有行的：**插入**；匹配到的：**更新**（非基线）。 |
| **行匹配键** | 以 **`parameter_code`（参数编码列）** 为业务主键；若同一 `commandId` 下存在**多行同码**，优先按 **`bit_usage`（BIT 占用列）** 与库中记录对齐，否者取同码首条。 |
| **主表列写入** | 从表头列解析主表各字段；**「数据状态」列**：表头存在且该单元格**非空**时写入 `data_status`。<br>**更新**路径：先读库中完整行，再**仅对 Excel 里非空单元格**覆盖对应字段，**避免**对未出现在表中的列使用「创建占位默认值」把库中已有数据冲掉。 |
| **新增**路径 | 对来自 Excel 的草稿行执行与创建页一致的**服务端占位**（`ParameterDefaults.applyForCreate`），再按上款写入 `data_status` 等。 |
| **变更说明子表** | 若本行在「**变更类型**、变更四格（中/英）、**导出 delta**、**不导出原因**」中**任一格**非空，则视用户提交变更说明：校验规则与**单条保存**一致（含变更类型允许集、`export_delta` 为「否」时 `no_export_reason` 必填、四格必填等）。校验通过后，对该 **`parameter_id`** **删除**原 `config_change_description` 行并**插入一条**新行（**不做**多行子表在 Excel 中的往返；与导出取**单条/首条**展示为同一套「单行承载」策略）。若上述列**全空**则**不改**子表。 |
| **其它校验** | 变更来源**黑名单**、序号与 `parameter_code` 一致、`bit_usage` 与版本+命令下其它参数**不冲突**等，与保存路径一致。 |
| **模板** | `GET` `/api/v1/products/{productId}/versions/{versionId}/parameters/import-templates` 下载；列名与导出一致。 |

### 13.5 异常（示例）

| 编码 | 场景 |
|------|------|
| `PARAM_BIT_CONFLICT` | BIT 占用冲突 |
| `PARAM_SEQUENCE_INVALID` | 超出区段或类型范围 |
| `PARAM_CHANGE_DESC_REQUIRED` | 未填变更说明 |
| `PARAM_CHANGE_SOURCE_FORBIDDEN` | 命中关键字黑名单（`message`/`data.violatedKeywordRegex` 含命中正则原文） |
| `PARAM_BASELINE_LOCKED` | 基线后编辑 |

### 13.6 权限点

- `param:parameter:read`、`param:parameter:write`、`param:parameter:import`、`param:parameter:export`、`param:parameter:baseline`

### 13.7 实现方案

- **领域**：`Parameter` 充血模型封装 BIT 占用校验；`ParameterChangeRecord` 值对象列表；领域事件（可选）用于审计。
- **应用**：`ParameterApplicationService` 协调聚合、变更说明持久化、导入导出。
- **基础设施**：`SystemParameterMapper`、`ConfigChangeDescriptionMapper`；禁止复杂 SQL。

### 13.8 DFX

- 列表分页；`available-sequences` 可缓存短 TTL；大表导出异步。

### 13.9 安全性

- 变更来源黑名单；基线权限单独控制。

### 13.10 测试方案与用例（扩展）

| 用例 ID | 场景 |
|---------|------|
| TC-PAR-001 | BYTE 同序号不同 BIT 段共存 |
| TC-PAR-002 | 变更说明缺失拒绝保存 |
| TC-PAR-003 | 黑名单拒绝且响应含命中 `keyword_regex` 原文 |
| TC-PAR-004 | 基线后编辑拒绝 |
| TC-PAR-005 | 导入错误行报告 |

---

## 14. 工程目录架构（最终约定）

> 与 `docs/backend-dev.md` 分层、`docs/frontend-style.md` 技术栈一致；根包名 **`com.coretool.param`**（可按组织规范调整为实际域名倒置包名）。

### 14.1 后端目录架构（Spring Boot 3 + Java 21 + DDD + 充血模型）

**原则**：`domain` 仅含领域模型与仓储**接口**；MyBatis-Plus `Mapper` / `PO` 仅在 `infrastructure`；`Controller` / `VO` 仅在 `ui`；应用服务在 `application` 做用例编排与事务。**为何如此划分（Controller / Mapper 不进 domain、`model` 与 `repository` 含义）见 §16。**

```
param-service/                              # Maven 模块名示例：param-service
├── pom.xml
├── src/main/java/com/coretool/param/
│   ├── ParamApplication.java               # Spring Boot 启动类
│   │
│   ├── domain/                             # 【领域层】充血模型、不变量、仓储接口
│   │   ├── shared/                         # 共享内核（可选）
│   │   │   ├── identifier/               # 业务 ID 生成策略接口 IdGenerator
│   │   │   └── exception/                # 领域异常基类
│   │   ├── command/                      # 命令管理子域（对应 §2～§4）
│   │   │   ├── model/                    # Command、CommandType、VersionRange 聚合/实体/值对象
│   │   │   ├── service/                  # 领域服务（跨实体规则）
│   │   │   └── repository/               # CommandRepository、CommandTypeRepository…
│   │   ├── configdict/                   # 配置主数据子域（对应 §5～§12，可按表再分子包）
│   │   │   ├── model/
│   │   │   ├── service/
│   │   │   └── repository/
│   │   └── parameter/                    # 参数核心子域（对应 §13）
│   │       ├── model/                    # Parameter 聚合、ParameterChangeDescription
│   │       ├── service/                  # ParameterAllocationService 等
│   │       └── repository/
│   │
│   ├── application/                        # 【应用层】用例、编排、事务边界
│   │   ├── service/
│   │   │   ├── command/                  # CommandApplicationService…
│   │   │   ├── configdict/
│   │   │   └── parameter/                # ParameterApplicationService
│   │   ├── dto/                            # 应用层入参/出参（与 VO 区分时可放此处）
│   │   └── assembler/                    # 领域对象 ↔ DTO 组装（可选）
│   │
│   ├── infrastructure/                   # 【基础设施层】持久化与技术细节
│   │   ├── persistence/
│   │   │   ├── po/                         # 与 GaussDB 表一一对应的持久化对象
│   │   │   ├── mapper/                   # MyBatis-Plus BaseMapper
│   │   │   └── repository/               # 仓储接口实现（调用 Mapper）
│   │   ├── idgen/                          # IdGenerator 实现（command_、version_ 等前缀）
│   │   ├── excel/                        # 导入导出适配（EasyPOI / 项目选型）
│   │   └── config/                       # MyBatisPlus、数据源、分页插件
│   │
│   ├── ui/                                 # 【用户接口层】REST + 统一 ResponseObject
│   │   ├── controller/
│   │   │   ├── command/
│   │   │   ├── configdict/
│   │   │   └── parameter/
│   │   ├── vo/                             # 请求体、Query 封装
│   │   ├── response/                     # ResponseObject、全局异常转译
│   │   └── exception/                    # @ControllerAdvice（若与全局包拆分）
│   │
│   ├── constants/                          # 通用常量
│   │   └── enums/
│   ├── configuration/                    # Spring @Configuration（非业务）
│   ├── utils/                              # 与领域无关的工具
│   └── exception/                          # 业务异常类（*Exception / *Error）
│   │
│   └── resources/
│       ├── application.yml
│       └── mapper/                         # XML 映射（尽量仅用 MP 注解，XML 保持极少）
│
└── src/test/java/                          # 单元测试 / 集成测试（测试类 *Test 后缀）
```

**说明**：

- **`domain` 子包命名**可按聚合再拆（上表为推荐映射）；若团队偏好按**限界上下文**分包，也可使用 `domain.commandmgmt`、`domain.config`、`domain.parameter` 等，但须**单聚合单仓储边界清晰**。
- **`infrastructure.persistence.po`** 与 `table字段简介.md` 表名对齐；禁止在 `domain` 引用 `Mapper`。
- **公共横切**：分页、操作日志、租户/产品校验可在 `application` 基类或 `ui` 拦截器中实现，规范见 **§1**。

---

### 14.2 前端目录架构（Vue 3 + TypeScript + Vite）

**原则**：按**业务模块**划分子目录，与详细设计 §2～§13 对应；API、类型、页面同模块就近；全局样式遵循 `frontend-style.md`（`public/assets/styles` + `src/styles`）。

```
param-web/                                  # 前端工程根目录示例
├── index.html
├── vite.config.ts
├── package.json
├── public/
│   └── assets/
│       └── styles/                         # 全局样式入口（见 frontend-style.md）
│           ├── index.scss
│           ├── base.scss
│           ├── common.scss
│           ├── nprogress.scss
│           └── variables.scss
│
├── src/
│   ├── main.ts
│   ├── App.vue
│   │
│   ├── assets/                             # 静态资源（图片、字体）
│   │
│   ├── styles/                             # 工程内样式（variables、mixins、layout）
│   │   ├── variables.scss
│   │   ├── mixins.scss
│   │   ├── components.scss
│   │   └── layout.scss
│   │
│   ├── router/
│   │   └── index.ts                        # 路由；按模块懒加载 views/command、config、parameter
│   │
│   ├── stores/                             # Pinia：当前产品、当前版本、用户上下文等
│   │   ├── productContext.ts
│   │   └── versionContext.ts
│   │
│   ├── api/                                # REST 封装，与后端 /api/v1 对齐
│   │   ├── http.ts                         # axios 实例、拦截器、ResponseObject 解包
│   │   ├── command/                        # 命令、类型、区段
│   │   ├── config/                         # 版本、分类、关键字、网元、NF、生效方式、特性、项目组
│   │   └── parameter/                      # 参数核心、导入导出
│   │
│   ├── types/                              # TypeScript 类型（与 VO、后端字段对齐）
│   │   ├── api-response.ts
│   │   └── models/
│   │
│   ├── components/                         # 跨模块复用组件
│   │   ├── common/                         # 表格封装、分页、导入导出对话框
│   │   └── business/                       # 强业务弱复用
│   │
│   ├── composables/                        # 组合式函数（usePagination、useProductScope）
│   │
│   ├── utils/
│   │   ├── format.ts
│   │   └── validate.ts
│   │
│   └── views/                              # 页面（与菜单/页签对应；嵌入宿主时仍为独立路由模块）
│       ├── command/                        # §2～§4
│       │   ├── CommandList.vue
│       │   ├── CommandTypeList.vue
│       │   └── VersionRangeList.vue
│       ├── config/                         # §5～§12
│       │   ├── VersionList.vue
│       │   ├── CategoryList.vue
│       │   ├── KeywordList.vue
│       │   ├── NeList.vue
│       │   ├── NfList.vue
│       │   ├── EffectiveModeList.vue
│       │   ├── FeatureList.vue
│       │   └── ProjectTeamList.vue
│       └── parameter/                      # §13
│           ├── ParameterLayout.vue         # 左树右表布局
│           └── components/                 # 参数表单子组件、BIT 选择器等
│
└── env/                                    # 环境变量示例（可选）
```

**说明**：

- **`@plmcsdk/common-ui` / Element Plus**：在 `components/common` 或各 `views` 中按需引入；业务表格样式遵循 `frontend-style.md` 设计令牌。
- **嵌入宿主框架**时：`router` 可作为子路由挂载；`stores` 中的产品/版本从宿主注入或 URL Query 同步（实现细节见集成 Spec）。
- **子应用壳层与留白（`spec-04` 落地）**：`App.vue` 使用**单行上下文工具条**（`el-radio-group`：**命令管理 / 配置管理 / 参数管理** ↔ `/command`、`/config`、`/parameter`）+ **产品选择器**；**不**再使用独立 `el-header` + 横向 `el-menu` 作为第二套顶栏。紧凑模式：`VITE_EMBEDDED` 或 Query `embed=1`（`src/utils/embed.ts`），根节点 **`app-shell--embedded`**。主业务区：主卡片 **`page-card` + `card-item`**，全局留白与 `border-card` Tab 内容区、`.common-main` 最大高度等在 **`public/assets/styles/common.scss`** 收紧；命令/配置/参数页**不再重复**与工具条同义的模块大标题。详见 **`openspec/spec-04-integration.md`** §3.1～§3.2。
- **API 分层**：`api/**` 仅负责 HTTP 与类型，不包含业务规则；复杂校验与 §13 BIT 逻辑以**后端为准**，前端做辅助提示。

---

## 15. 附录

### 15.1 OpenSpec 拆分建议（与 domain 对齐，避免「一页一 Spec」）

后端工程在 **`domain`** 下按子域划分为 **命令管理、配置主数据、参数核心** 三类业务包（另含 **`shared` 共享内核**，见 §14.1）；与之一一对应拆 Spec，**粒度以「限界上下文 / domain 包」为准**，**不建议**按「一个页面一个 Spec」拆成十余份（评审与版本同步成本高、且与 DDD 边界不一致）。

**推荐做法**：**少量 Spec 文件 + 文件内二级标题对应本设计 §2～§13**。

**文档落盘位置**：项目根目录 **`openspec/`**（与 `openspec init` 生成的 `config.yaml` 同级）；索引与变更提案见 `openspec/README.md`、`openspec/PROPOSAL.md`。详细设计、表结构说明仍在 **`docs/`**。

| 建议 Spec 文件（示例名） | 对应 `domain` 包（§14.1） | 纳入的本设计章节 | 内含页面/功能（分节即可） |
|--------------------------|---------------------------|-------------------|---------------------------|
| `spec-00-platform.md` | （非业务 domain：`shared` 接口 + 横切） | §0 导读要点、**§1 公共通用**、**§14 工程目录** | 分页、权限占位、导入导出约定、前后端目录 |
| `spec-01-command-domain.md` | `domain/command/` | **§2～§4** | 命令配置、类型及范围、版本区段（三节） |
| `spec-02-config-masterdata.md` | `domain/configdict/` | **§5～§12** | 版本、分类、关键字、网元、NF、生效方式、特性、项目组（八节） |
| `spec-03-parameter-core.md` | `domain/parameter/` | **§13** | 参数管理核心（左树右表、BIT 规则、变更说明等） |
| `spec-04-integration.md`（可选） | `ui` + 宿主适配 | **§15.3** 扩展 | 嵌入宿主、菜单、页签、上下文注入 |

**若必须压缩为「恰好 4 个 Spec 文件」**：可将 **`spec-00-platform`** 中与目录无关的条文并入 `spec-01` 前言，或把 **集成** 并入 `spec-03` 附录，仅保留 **平台+公共、命令、配置、参数** 四份——由项目组在「文件数量」与「公共规范独立成册」之间权衡。

**不推荐**：为 §2～§13 各建独立 Spec（共 12+ 份），除非单页有独立发版/独立外包边界。

### 15.2 全局错误码表（占位）

各模块在实现阶段汇总到 `ErrorCode` 枚举（后缀 `Exception`），并在此附录补全编号段。

### 15.3 与宿主框架集成（菜单）

- 见 `function.md` 第 6 点：本系统以**页签**嵌入；建议**一级：参数管理**，二级：**命令 / 配置 / 参数** 扁平化，避免过深；具体菜单 JSON 由前端 Spec 补充。
- **宿主零改动时的子应用侧（与 `openspec/spec-04-integration.md` 一致）**：
  - **模块切换**：由 `param-web` 内路由完成；工具条文案为 **命令管理 / 配置管理 / 参数管理**，不依赖宿主再挂三个子菜单。
    - **视觉尺寸**：模块切换按钮（`el-radio-button`）需与页面主体表格/Tab 的视觉重量匹配，避免因过小导致“上轻下重”；可通过增大按钮内边距与字号实现，嵌入模式（`app-shell--embedded`）下允许略收紧。
  - **上下文**：`productId` / `versionId` / 可选 `productName` 与 **Query `embed`** 由子应用解析；产品、版本变更时可回写当前路由 Query，便于宿主页签或外链联调。
  - **紧凑模式**：`VITE_EMBEDDED=true` 或 `?embed=1` 时收紧工具条与主区留白；**不要求宿主改代码**。
  - **主内容区**：主卡片 `page-card`、全局样式见 **`public/assets/styles/common.scss`**；页面内**不再与工具条重复**模块级大标题；参数域版本/ALL 仅在参数页展示。
  - **命令域工具条行（实现约束）**：
    - 命令域三个 Tab（命令/类型/区段）顶部工具条采用同一行布局：**左侧为操作按钮（新增/刷新/导出/导入）**，**右侧为查询控件（输入/下拉 + 查询按钮）**，两组控件**紧挨排列**（不做 `space-between` 两端拉开）。
    - 查询区去掉「关键字/类型」等 label 文案，仅保留 placeholder；操作按钮与查询控件需**上下居中**对齐。
    - 操作按钮之间的间距需收紧（例如使用 flex `gap`，并消除 Element Plus 相邻按钮默认 `margin-left` 与 `gap` 叠加造成的过大间距）。

---

## 16. 分层设计说明（DDD：为何 Controller / Mapper 不在 `domain` 下）

> 本章回答实施常见疑问：**为什么各 `domain` 包下没有 Controller？** **Mapper 与数据库逻辑是否也放在领域外？** **`domain/.../model` 与 `domain/.../repository` 分别承担什么职责？**  
> 与 **`docs/backend-dev.md`** 中的 DDD、充血模型、分层架构一致；**§14.1** 的目录结构按此原则编排。

### 16.1 为何 `domain` 下不包含 Controller？

- 在 DDD 与经典分层中，**领域层（`domain`）只表达业务知识**，不应依赖 **Web 技术**（Spring MVC、`@RestController`、HTTP、Servlet 等）。
- **Controller** 属于**用户接口 / 接入层**（本文档中的 **`ui`**，亦可称为 Adapter、Driving Adapter）：负责将 HTTP 请求参数转换为对**应用层**的调用，并把结果封装为统一响应（如 `ResponseObject`）。
- 因此 **Controller 放在 `ui/controller`，不放在 `domain/**`** 是**刻意边界划分**，不是遗漏。这样领域模型可脱离 Web 单独测试与演进，并符合**依赖方向朝内**（领域不依赖外层框架）。

**典型调用链**：

`Controller（ui）` → `ApplicationService（application）` → `领域对象与领域服务（domain）` → `Repository 接口（domain）` → `Repository 实现 + Mapper（infrastructure）`

**与「按业务分包」的关系**：也可采用**按限界上下文竖切**的目录（例如 `command/web`、`command/domain` 并列），但仍是 **Web 与 Domain 分属不同子目录**，**不会**把 Controller 放进 `domain`。

---

### 16.2 Mapper、数据库逻辑应放在哪里？`model` 与 `repository` 在领域层各做什么？

#### 16.2.1 Mapper 与持久化实现的位置

| 层次 | 内容 |
|------|------|
| **`infrastructure`（基础设施层）** | MyBatis-Plus **`Mapper`**、与表结构对应的 **PO**、**Repository 实现类**（内部调用 `mapper`）、数据源与 ORM 配置、导入导出等技术适配。 |
| **`domain`（领域层）** | **不出现** Mapper、**不出现** 具体 SQL、**不依赖** MyBatis/JDBC API。 |

**原因**：持久化技术是**可替换的实现细节**；领域层若直接依赖 Mapper，则换库、换 ORM、做纯领域单元测试都会受阻。故 **「数据库访问实现」放在领域外（基础设施）** 与 **「Controller 放在领域外」** 同理，都是 **保护领域模型的纯粹性**。

#### 16.2.2 `domain/.../model` 的职责（充血模型载体）

- 表示**领域模型**：**实体**、**值对象**、**聚合根**等。
- 承载**业务规则与不变量**（如参数 BIT 占用、基线后不可改、变更说明条数等），优先以**对象行为**表达（充血模型），而非在应用层堆叠 `if` + 直接调 Mapper。
- **不是**「仅与表字段一一对应的贫血 PO」；与表映射的对象一般放在 **`infrastructure/persistence/po`**，在仓储实现中完成 **PO ↔ 领域对象** 的转换。

#### 16.2.3 `domain/.../repository` 的职责（仓储接口）

- 此处仅为 **接口（Port）**，声明领域需要的持久化能力，例如：`save(聚合)`、`findById`、`findByVersion(...)`。
- **不包含任何实现与 SQL**；实现类位于 **`infrastructure/.../repository`**，内部注入 **Mapper** 完成落库。
- 依赖方向：**领域定义接口，基础设施实现接口**（依赖倒置），使领域保持稳定，持久化细节变更不影响业务规则表达方式。

#### 16.2.4 小结

| 概念 | 放在哪 | 说明 |
|------|--------|------|
| 业务规则 / 状态变更合法与否 | **`domain`（model / domain service）** | 核心业务逻辑 |
| 对外暴露的持久化契约 | **`domain`（repository 接口）** | 无技术细节 |
| HTTP 与 DTO 适配 | **`ui`（Controller）** | |
| SQL、Mapper、PO、仓储实现 | **`infrastructure`** | 技术细节 |

---

**文档结束。** 修订时请同步更新 `docs/function.md` / `table字段简介.md` 变更说明，并回写本章。
