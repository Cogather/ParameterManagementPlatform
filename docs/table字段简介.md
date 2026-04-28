# 实体配置管理系统 - 数据库表结构文档

## 1. entity_basic_info 表（产品表）

### 表描述
实体基础信息表，用于存储系统中各类实体的基础配置信息。该表是整个系统的核心主表，定义了实体的唯一标识、名称、类型等关键属性。

### 关键处理规则
- **唯一性约束**：`product_form_id` 是该表的唯一标识，系统自动生成，全局唯一
- **产品形态名称唯一性（业务规则）**：在**同一 `product_id` 内**的**启用**记录上（`entity_status=1` 或为空视为可用），`product_form`（产品形态名称）须**唯一**；比较时建议按 `lower(btrim(product_form))` 去重。详细交互与接口见 `function.md` §2.0、详设 §0.6。
- **版本控制**：同一 `product_form_id` 下，同一版本只能存在一条记录
- **命名规范**：实体名称支持中英文，长度不超过255字符
- **责任人机制**：支持多人责任，多个责任人以英文逗号分隔
- **时间戳精度**：创建时间和更新时间精确到毫秒级
- **产品形态**：一个产品会有多个形态
- **关联**：通过 `product_id` 关联其他产品系统

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|--------|---------|---------|---------|----------|------|
| 1 | entity_name | 产品名称 | 产品名称 | String | 255 | 必填 | 支持中英文 |
| 2 | product_form_id | 产品形态ID | 唯一ID，系统自动生成 | String | 50 | 必填 | 系统自动生成，唯一 |
| 3 | product_soft_param_type | 产品参数类型 | 产品支持的参数类型 | Enum | 50 | 必填 | Single  / Multi  |
| 4 | product_form | 产品形态 | 一个产品会有多个形态 | String | 255 | 可选 | 主键 |
| 5 | product_id | 产品 ID |  ID | String | 50 | 必填 | 关联系统 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |
| 10 | owner_list | 责任人 | 责任人列表 | String | 255 | 必填 | 支持多选，英文逗号分隔 |
| 11 | entity_status | 状态 | 实体状态 | Integer | - | 可选 | |

---

## 2. entity_command_mapping 表（产品命令表）

### 表描述
实体命令映射表，用于建立实体与系统命令之间的关联关系。该表定义了实体支持的具体命令及其配置范围。

### 关键处理规则
- **关联约束**：通过 `owned_product_id` 关联到 `entity_basic_info.product_form_id` 表
- **命令唯一性**：同一实体下，命令名称必须唯一
- **类型约束**：命令类型必须在 `command_type_definition` 表中定义
- **版本支持**：支持按版本定义命令的生效范围

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 产品ID，关联产品表的产品 ID | String | 255 | 必填 | 关联 entity_basic_info.product_id |
| 2 | command_id | 命令 ID | 命令id，系统自动生成 | String | 255 | 必填 | 系统自动生成，唯一 |
| 3 | command_name | 命令 | 命令 | String | 255 | 必填 | |
| 4 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 5 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 6 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 7 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |
| 8 | owner_list | 责任人 | 责任人，支持多选，多个以英文逗号隔离 | String | 255 | 必填 | |
| 9 | command_status | 状态 | 命令状态 | Integer | - | 可选 | |

---

## 3. command_type_definition 表（命令支持的类型及类型范围）

### 表描述
命令类型定义表，用于定义系统支持的所有命令类型及其取值范围。该表是命令类型的字典表，确保命令类型的标准化和一致性。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **类型唯一性**：`command_type_code` 为唯一标识
- **范围约束**：定义了每种命令类型支持的最小和最大值
- **版本控制**：支持按版本定义类型的有效范围
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | owned_command_id | 归属命令 ID | 归属命令 ID | String | 255 | 必填 | |
| 3 | command_type_id | 自定义类型ID | 自定义类型ID | String | 255 | 必填 | 唯一标识 |
| 4 | command_type_name | 自定义类型名称 | 自定义类型名称 | String | 255 | 必填 | |
| 5 | command_type | 类型 | 类型 | Enum | 50 | 必填 | |
| 6 | min_value | 最小序号 | 最小序号 | Integer | - | 必填 | |
| 7 | max_value | 最大序号 | 最大序号 | Integer | - | 必填 | |
| 8 | occupied_serial_number | 占用序号 | 占用序号 | String | 512 | 可选 | |
| 9 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 10 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 11 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 12 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |
| 13 | command_type_status | 状态 | 状态 | Integer | - | 可选 | |

---

## 4. command_type_version_range 表（命令类型按版本区段划分）

### 表描述
命令类型版本区段表，用于定义不同版本下命令类型的支持范围。该表实现了命令类型的版本化管理。

### 关键处理规则
- **关联约束**：通过 `command_type_code` 关联到 `command_type_definition` 表
- **版本连续性**：同一命令类型的版本区段不能重叠
- **范围验证**：版本区段内的取值范围必须在命令类型定义的范围内
- **状态管理**：支持启用/禁用状态

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | owned_command_id | 归属命令 ID | 归属命令 ID | String | 255 | 必填 | |
| 3 | owned_type_id | 归属类型 | 归属类型 | String | 255 | 必填 | |
| 4 | version_range_id | 区段ID | 区段ID | String | 255 | 必填 | 唯一标识 |
| 5 | start_index | 起始序号 | 起始序号 | Integer | - | 必填 | |
| 6 | end_index | 结束序号 | 结束序号 | Integer | - | 必填 | |
| 7 | range_description | 说明 | 说明 | String | 512 | 可选 | |
| 8 | range_type | 区段划分类型 | 区段划分类型 | String | 50 | 可选 | |
| 9 | owned_version_or_business_id | 归属版本ID/归属业务类型ID | 归属版本ID/归属业务类型ID | String | 255 | 必填 | |
| 10 | range_status | 状态 | 状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 11 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 12 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 13 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 14 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 5. entity_version_info 表（产品版本表）

### 表描述
实体版本信息表，用于管理实体的版本信息。该表定义了实体的版本号、版本名称等关键属性，支持实体的版本化管理。

### 关键处理规则
- **关联约束**：通过 `owned_product_id` 关联到 `entity_basic_info.product_form_id` 表
- **版本唯一性**：同一 `owned_product_id` 下，`version_name` 必须唯一
- **版本标识**：`version_id` 是该表的唯一标识，系统自动生成
- **时间顺序**：创建时间必须早于更新时间

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 关联 entity_basic_info.product_id |
| 2 | version_id | 版本ID | 版本ID | String | 255 | 必填 | 系统自动生成，唯一 |
| 3 | version_name | 版本名称 | 版本名称 | String | 255 | 必填 | 同一实体下唯一 |
| 4 | version_type | 版本类型 | 版本类型 | Enum | 50 | 必填 | |
| 5 | version_description | 说明 | 说明 | String | 512 | 可选 | |
| 6 | baseline_version_id | 基线版本ID | 基线版本ID | String | 255 | 可选 | |
| 7 | baseline_version_name | 基线版本名称 | 基线版本名称 | String | 255 | 可选 | |
| 8 | version_desc | 版本描述 | 版本描述 | String | 1024 | 可选 | |
| 9 | approver | 审批人 | 审批人 | String | 50 | 可选 | |
| 10 | is_hidden | 是否隐藏版本 | 是否隐藏版本 | String | 50 | 可选 | |
| 11 | supported_version | 支持版本 | 支持版本 | String | 50 | 可选 | |
| 12 | version_status | 版本状态 | 版本状态 | Integer | - | 必填 | |
| 13 | introduced_product_id | 引入产品 ID | 引入产品 ID | String | 255 | 可选 | |
| 14 | owner_list | 责任人 | 责任人 | String | 255 | 必填 | |
| 15 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 16 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 17 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 18 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 6. entity_business_category 表（产品业务分类字典表）

### 表描述
实体业务分类字典表，用于定义实体的业务分类体系。该表是业务分类的字典表。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **分类唯一性**：`category_id` 为唯一标识
- **分类状态**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | category_id | 业务分类ID | 业务分类ID | String | 255 | 必填 | |
| 3 | category_name_cn | 分类名称（中文） | 分类名称（中文） | String | 255 | 必填 | |
| 4 | category_name_en | 分类名称（英文） | 分类名称（英文） | String | 512 | 必填 | |
| 5 | feature_range | 包含特性范围 | 包含特性范围 | String | 512 | 可选 | |
| 6 | category_type | 所属类别 | 所属类别 | String | 50 | 可选 | |
| 7 | category_status | 状态 | 分类状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 8 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 9 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 10 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 11 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 7. config_change_source_keyword 表（参数变更来源关键字）

### 表描述
配置变更来源关键字表，用于记录配置变更的来源关键字。该表支持变更来源的追溯和管理。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **关键字唯一性**：`keyword_id` 为唯一标识
- **产品关联**：通过 `owned_product_id` 关联产品表
- **状态管理**：支持启用/禁用状态

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 归属产品 ID | String | 255 | 必填 | 主键 |
| 2 | keyword_id | 关键字正则ID | 关键字正则ID | String | 255 | 必填 | 唯一标识 |
| 3 | keyword_regex | 关键字正则 | 关键字正则 | String | 512 | 必填 | |
| 4 | reason | 原因 | 原因 | String | 512 | 必填 | |
| 5 | keyword_status | 状态 | 状态 | Integer | - | 必填 | 1-启用，0-禁用 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 8. entity_applicable_ne_dict 表（产品适用网元字典表）

### 表描述
实体适用网元字典表，用于定义实体适用的网络元素类型。该表是网元类型的字典表，确保网元类型的标准化。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **网元唯一性**：`ne_type_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | ne_type_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | ne_type_name_cn | 适用网元名称 | 适用网元名称 | String | 50 | 必填 | |
| 4 | product_form | 产品形态 | 产品形态 | String | 255 | 可选 | 多选 |
| 5 | ne_type_status | 状态 | 网元类型状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 9. entity_nf_config_dict 表（产品nf配置字典表）

### 表描述
实体网络功能配置字典表，用于定义实体的网络功能配置类型。该表是网络功能配置的字典表。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **配置唯一性**：`nf_config_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | nf_config_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | nf_config_name_cn | nf名称 | 网络功能配置名称 | String | 255 | 必填 | |
| 4 | nf_config_status | 状态 | 网络功能配置状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 5 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 6 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 7 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 8 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 10. entity_effective_mode_dict 表（产品生效方式配置字典表）

### 表描述
实体生效方式配置字典表，用于定义实体的生效方式类型。该表是生效方式的字典表。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **方式唯一性**：`effective_mode_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | effective_mode_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | effective_mode_name_cn | 生效方式（中文） | 生效方式名称（中文） | String | 255 | 必填 | |
| 4 | effective_mode_name_en | 生效方式（英文） | 生效方式名称（英文） | String | 512 | 必填 | |
| 5 | effective_mode_status | 状态 | 生效方式状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 11. entity_effective_form_dict 表（产品生效形态配置字典表）

### 表描述
实体生效形态配置字典表，用于定义实体的生效形态类型。该表是生效形态的字典表。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **形态唯一性**：`effective_form_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | effective_form_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | effective_form_name_cn | 生效形态（中文） | 生效形态名称（中文） | String | 255 | 必填 | |
| 4 | effective_form_name_en | 生效形态（英文） | 生效形态名称（英文） | String | 512 | 必填 | |
| 5 | effective_form_status | 状态 | 生效形态状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 12. entity_module_dict 表（产品模块字典表）

### 表描述
产品模块字典表，用于定义系统中的产品模块信息。该表是模块的字典表，支持模块的标准化管理。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **模块唯一性**：`module_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | module_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | module_name_cn | 模块名称（中文） | 模块名称（中文） | String | 255 | 必填 | |
| 4 | module_name_en | 模块名称（英文） | 模块名称（英文） | String | 512 | 必填 | |
| 5 | module_status | 状态 | 模块状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 13. version_feature_dict 表（版本特性字典表）

### 表描述
版本特性字典表，用于定义不同版本支持的特性。该表实现了版本特性的标准化管理。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **特性唯一性**：`feature_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品/版本关联**：通过 `owned_product_id` 和 `owned_version_id` 关联产品和版本表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品 ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | owned_version_id | 归属版本ID | 归属版本ID，关联产品版本表的Version Id | String | 255 | 必填 | 主键 |
| 3 | feature_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 4 | feature_code | 特性编码 | 特性编码，后端生成（页面不展示） | String | 255 | 必填 | |
| 5 | feature_name_cn | 中文名称 | 特性名称（中文） | String | 512 | 必填 | |
| 6 | feature_name_en | 英文名称 | 特性名称（英文） | String | 1024 | 必填 | |
| 7 | introduce_type | 引入类型 | 引入类型 | String | 50 | 必填 | 版本新增/继承/引用/其他 |
| 8 | inherit_reference_version_id | 继承/引用版本ID | 继承/引用版本ID | String | 255 | 可选 | |
| 9 | feature_status | 状态 | 特性状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 10 | introduced_product_id | 引入产品 ID | 引入产品 ID | String | 255 | 可选 | |
| 11 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 12 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 13 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 14 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 14. project_team_dict 表（项目组字典表）

### 表描述
项目组字典表，用于定义系统中的项目组信息。该表是项目组的字典表，支持项目组的标准化管理。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护
- **项目组唯一性**：`team_id` 为唯一标识
- **状态管理**：支持启用/禁用状态
- **产品关联**：通过 `owned_product_id` 关联产品表

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | owned_product_id | 归属产品ID | 产品ID，关联产品表的Product Id | String | 255 | 必填 | 主键 |
| 2 | team_id | ID | 字典ID，自动生成 | String | 255 | 必填 | 唯一 |
| 3 | team_name | 项目组名称 | 项目组名称 | String | 512 | 必填 | |
| 4 | team_status | 状态 | 项目组状态 | Integer | - | 可选 | 1-启用，0-禁用 |
| 5 | owner_list | 责任人 | 责任人 | String | 255 | 必填 | |
| 6 | creator_id | 创建人 | 创建者标识 | String | 50 | 必填 | |
| 7 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 必填 | 精确到毫秒 |
| 8 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 | |
| 9 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒 |

---

## 15. system_parameter 表（OM系统参数）

### 表描述
系统参数表，用于存储系统参数的完整配置信息。该表是系统参数的核心表，包含了参数的所有详细属性，支持多语言、版本管理、变更追溯等功能。

### 关键处理规则
- **唯一性约束**：`parameter_id` 是该表的唯一标识，系统自动生成，全局唯一
- **参数唯一性**：同一 `owned_product_id` 下，`parameter_name` 必须唯一
- **关联约束**：
    - 通过 `owned_product_id` 关联到 `entity_basic_info` 表
    - 通过 `owned_version_id` 关联到 `entity_version_info` 表
    - 通过 `owned_command_id` 关联到 `entity_command_mapping` 表
- **编码唯一性**：`parameter_code` 是系统自动生成的公司级统一编码，在版本继承时保持不变
- **时间戳精度**：创建时间和更新时间精确到毫秒级
- **数据状态管理**：支持草稿、工作中、基线评审、过程评审、已发布、废弃等多种状态
- **多语言支持**：参数名称、描述、取值说明、应用场景、影响说明、配置举例等字段都支持中英文

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注                                                                             |
|---------|--------|---------|---------|---------|---------|----------|--------------------------------------------------------------------------------|
| 1 | parameter_id | ID | 参数唯一标识 | Integer | 50 | 必填 | 系统自动生成，全局唯一                                                                    |
| 2 | parameter_code | 编码 | 参数编码 | String | 50 | 必填 | 系统自动生成的统一编码，版本继承时保持不变                                                          |
| 3 | owned_object_type | 归属对象类型 | 归属对象类型 | String | 64 | 可选 |                                                                                |
| 4 | owned_object_code | 归属对象编码 | 归属对象编码 | String | 64 | 可选 |                                                                                |
| 5 | tenant_id | 租户id | 租户ID | String | 32 | 必填 |                                                                                |
| 6 | domain_id | 空间ID | 空间ID | Integer | - | 可选 |                                                                                |
| 7 | data_status | 数据状态 | 数据状态 | String | 50 | 必填 | Draft草稿、Inwork工作中、Baseline Review基线评审、Process Review过程评审、Publish已发布、Obsolete废弃 |
| 8 | owned_product_id | 归属产品ID | 归属产品唯一标识符 | String | 255 | 必填 | 关联 entity_basic_info                                                           |
| 9 | owned_version_id | 归属版本ID | 归属版本标识符 | String | 255 | 必填 | 关联 entity_version_info                                                         |
| 10 | owned_command_id | 归属命令ID | 归属命令标识符 | String | 255 | 必填 | 关联 entity_command_mapping                                                      |
| 11 | introduce_type | 引入类型 | 引入类型 | String | 50 | 必填 | 版本新增Version additions、继承Inherit、引用Reference、其他Others                           |
| 12 | inherit_reference_version_id | 继承/引用版本ID | 继承/引用版本ID | String | 255 | 可选 |                                                                                |
| 13 | parameter_name_cn | 参数名称（中文） | 参数名称（中文） | String | 255 | 必填 | 参数标题，参数ID+参数功能描述                                                               |
| 14 | parameter_name_en | 参数名称（英文） | 参数名称（英文） | String | 512 | 必填 |                                                                                |
| 15 | bit_usage | 使用BIT位 | 使用BIT位 | String | 255 | 可选 | 逗号分隔，列出当前序号占用的BIT位，IT管理字段                                                      |
| 16 | parameter_sequence | 参数序号 | 参数序号 | Integer | - | 可选 |                                                                                |
| 17 | value_range | 使用情况（取值范围） | 使用情况（取值范围） | String | 255 | 必填 |                                                                                |
| 18 | value_description_cn | 取值说明（中文） | 取值说明（中文） | String | 1024 | 必填 |                                                                                |
| 19 | value_description_en | 取值说明（英文） | 取值说明（英文） | String | 2048 | 必填 |                                                                                |
| 20 | application_scenario_cn | 应用场景（中文） | 应用场景（中文） | String | 1024 | 必填 |                                                                                |
| 21 | application_scenario_en | 应用场景（英文） | 应用场景（英文） | String | 2048 | 必填 |                                                                                |
| 22 | parameter_default_value | 参数默认值 | 参数默认值 | String | 255 | 必填 | 不支持中文字符                                                                        |
| 23 | parameter_recommended_value | 参数推荐值 | 参数推荐值 | String | 255 | 可选 |                                                                                |
| 24 | applicable_ne | 适用网元 | 适用网元 | String | 255 | 必填 | 以顿号分隔                                                                          |
| 25 | feature | 所属特性 | 所属特性 | String | 512 | 可选 |                                                                                |
| 26 | feature_id | 所属特性ID | 所属特性ID | String | 255 | 可选 |                                                                                |
| 27 | business_classification | 业务分类 | 业务分类 | String | 255 | 必填 |                                                                                |
| 28 | category_id | 业务分类ID | 业务分类ID | String | 255 | 必填 |                                                                                |
| 29 | take_effect_immediately | 参数是否立即生效 | 参数是否立即生效 | String | 50 | 必填 | 是/否、yes/no                                                                     |
| 30 | effective_mode_cn | 生效方式（中文） | 生效方式（中文） | String | 255 | 可选 |                                                                                |
| 31 | effective_mode_en | 生效方式（英文） | 生效方式（英文） | String | 512 | 可选 |                                                                                |
| 32 | project_team | 项目组 | 项目组 | String | 255 | 必填 |                                                                                |
| 33 | belonging_module | 归属模块 | 归属模块 | String | 255 | 可选 |                                                                                |
| 34 | patch_version | 补丁版本号 | 补丁版本号 | String | 255 | 可选 |                                                                                |
| 35 | introduced_version | 引入版本 | 引入版本 | String | 255 | 必填 | 支持配置                                                                           |
| 36 | parameter_description_cn | 参数含义（中文） | 参数含义（中文） | String | 1024 | 必填 |                                                                                |
| 37 | parameter_description_en | 参数含义（英文） | 参数含义（英文） | String | 2048 | 必填 |                                                                                |
| 38 | impact_description_cn | 影响说明（中文） | 影响说明（中文） | String | 1024 | 必填 |                                                                                |
| 39 | impact_description_en | 影响说明（英文） | 影响说明（英文） | String | 2048 | 必填 |                                                                                |
| 40 | configuration_example_cn | 配置举例（中文） | 配置举例（中文） | Text | - | 可选 |                                                                                |
| 41 | configuration_example_en | 配置举例（英文） | 配置举例（英文） | Text | - | 可选 |                                                                                |
| 42 | help_document_in_database | 帮助是否入库 | 帮助是否入库 | String | 50 | 必填 | Y/N                                                                            |
| 43 | related_parameter_cn | 关联参数（中文） | 关联参数（中文） | String | 255 | 必填 | 支持配置                                                                           |
| 44 | related_parameter_en | 关联参数（英文） | 关联参数（英文） | String | 1024 | 必填 | 支持配置                                                                           |
| 45 | related_parameter_usage | 关联参数使用情况 | 关联参数使用情况 | String | 1024 | 可选 |                                                                                |
| 46 | related_parameter_description_cn | 关联参数描述（中文） | 关联参数描述（中文） | String | 1024 | 可选 |                                                                                |
| 47 | related_parameter_description_en | 关联参数描述（英文） | 关联参数描述（英文） | String | 2048 | 可选 |                                                                                |
| 48 | remark | 备注 | 备注 | String | 1024 | 可选 |                                                                                |
| 49 | parameter_bit | 参数Bit位 | 参数Bit位 | String | 255 | 可选 | 不支持中文字符                                                                        |
| 50 | enumeration_values_cn | 参数枚举值（中文） | 参数枚举值（中文） | String | 512 | 可选 | 说明每个枚举的用途                                                                      |
| 51 | enumeration_values_en | 参数枚举值（英文） | 参数枚举值（英文） | String | 1024 | 可选 |                                                                                |
| 52 | applicable_logical_entity_cn | 适用逻辑实体（中文） | 适用逻辑实体（中文） | String | 255 | 可选 |                                                                                |
| 53 | applicable_logical_entity_en | 适用逻辑实体（英文） | 适用逻辑实体（英文） | String | 512 | 可选 |                                                                                |
| 54 | related_feature_cn | 关联特性（中文） | 关联特性（中文） | String | 512 | 可选 | 从特性库查询                                                                         |
| 55 | related_feature_en | 关联特性（英文） | 关联特性（英文） | String | 1024 | 必填 | 从特性库查询                                                                         |
| 56 | change_factors | 变更因素 | 变更因素 | String | 100 | 必填 | IT管理字段，AR/DTS                                                                  |
| 57 | change_related_number | 变更关联单号 | 变更关联单号 | String | 255 | 必填 | IT管理字段，AR编号或者DTS编号                                                             |
| 58 | change_source | 变更来源 | 变更来源 | String | 512 | 可选 |                                                                                |
| 59 | creator_id | 创建人 | 创建者标识 | String | 50 | 可选 |                                                                                |
| 60 | creation_timestamp | 创建时间 | 创建时间戳 | DateTime | - | 可选 | 精确到毫秒                                                                          |
| 61 | updater_id | 修改人 | 更新者标识 | String | 50 | 可选 |                                                                                |
| 62 | update_timestamp | 修改时间 | 更新时间戳 | DateTime | - | 可选 | 精确到毫秒                                                                          |
| 63 | parameter_unit_cn | 单位（中文） | 参数单位（中文） | String | 50 | 可选 |                                                                                |
| 64 | parameter_unit_en | 单位（英文） | 参数单位（英文） | String | 50 | 可选 |                                                                                |
| 65 | parameter_range | 范围 | 参数范围 | String | 100 | 可选 |                                                                                |
| 66 | effective_form_cn | 生效形态（中文） | 生效形态（中文） | String | 255 | 可选 |                                                                                |
| 67 | effective_form_en | 生效形态（英文） | 生效形态（英文） | String | 512 | 可选 |                                                                                |
| 68 | implementation_principle_cn | 实现原理（中文） | 实现原理（中文） | String | 512 | 可选 |                                                                                |
| 69 | implementation_principle_en | 实现原理（英文） | 实现原理（英文） | String | 1024 | 可选 |                                                                                |
| 70 | impact_level_cn | 影响级别（中文） | 影响级别（中文） | String | 50 | 可选 |                                                                                |
| 71 | impact_level_en | 影响级别（英文） | 影响级别（英文） | String | 50 | 可选 |                                                                                |
| 72 | figure_example_cn | 图形示例（中文） | 图形示例（中文） | String | 1024 | 可选 |                                                                                |
| 73 | figure_example_en | 图形示例（英文） | 图形示例（英文） | String | 2048 | 可选 |                                                                                |
| 74 | internal_description | 内部功能描述 | 内部功能描述 | String | 1024 | 可选 |                                                                                |
---

## 16. config_change_description 表（参数变更说明表）

### 表描述
配置变更说明表，用于记录配置变更的详细信息。该表支持变更历史的追溯和管理。

### 关键处理规则
- **唯一性约束**：`change_description_id` 是该表的唯一标识
- **关联约束**：
    - 通过 `entity_unique_id` 关联到 `entity_basic_info` 表
    - 通过 `version_id` 关联到 `entity_version_info` 表
    - 通过 `parameter_id` 关联到 `system_parameter` 表
- **参数关联**：通过 `parameter_id` 关联到 `system_parameter` 表
- **多语言支持**：支持中英文变更原因和影响描述
- **时间戳精度**：更新时间精确到毫秒级

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
  | 1 | parameter_id | 参数ID | 参数ID，关联OM系统参数ID | String | 255 | 必填 | 关联 system_parameter.parameter_id |
| 2 | change_description_id | 变更说明ID | 变更说明ID | String | 255 | 必填 | 系统自动生成，唯一 |
| 3 | change_type | 变更类型 | 变更类型 | String | 255 | 必填 | 关联 config_change_type |
| 4 | change_reason_cn | 变更原因（中文） | 变更原因（中文） | String | 1024 | 必填 | |
| 5 | change_impact_cn | 变更影响（中文） | 变更影响（中文） | String | 1024 | 必填 | |
| 6 | change_reason_en | 变更原因（英文） | 变更原因（英文） | String | 1024 | 必填 | |
| 7 | change_impact_en | 变更影响（英文） | 变更影响（英文） | String | 1024 | 必填 | |
| 8 | export_delta | 是否导出delta | 是否导出delta | String | 50 | 必填 | |
| 9 | no_export_reason | 不导出原因 | 不导出原因 | String | 1024 | 可选 | |
| 10 | updater_id | 修改人 | 修改人 | String | 50 | 可选 | |
| 11 | update_timestamp | 修改时间 | 修改时间 | DateTime | - | 可选 | 精确到毫秒 |
| 12 | entity_unique_id | 归属实体唯一标识 | 归属实体唯一标识符 | String | 255 | 必填 | 关联 entity_basic_info |
| 13 | version_id | 归属版本标识 | 归属版本标识符 | String | 255 | 必填 | 关联 entity_version_info |

---

## 17. config_change_type 表（参数变更类型）

### 表描述
配置变更类型表，用于定义系统中支持的配置变更类型。该表是变更类型的字典表，确保变更类型的标准化。

### 关键处理规则
- **字典表性质**：该表为系统字典表，由管理员维护，默认包含17条数据
- **类型唯一性**：`change_type_id` 为唯一标识
- **多语言支持**：支持中英文变更类型名称
- **排序支持**：通过 `change_sequence` 字段支持排序
- **默认值数据**：系统初始化时自动插入以下17条记录

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | change_type_id | ID | 变更类型ID | Integer | - | 必填 | 唯一标识 |
| 2 | change_type_name | ChangeType | 变更类型名称 | String | 100 | 必填 | |
| 3 | change_type_name_cn | DeltaType_CN | 变更类型名称（中文） | String | 100 | 必填 | |
| 4 | change_type_name_en | DeltaType_EN | 变更类型名称（英文） | String | 100 | 必填 | |
| 5 | change_sequence | DeltaSeq | 变更类型排序 | Integer | - | 必填 | 用于排序显示 |

### 默认值数据（系统初始化时自动插入）

| change_type_id | change_type_name | change_type_name_cn | change_type_name_en | change_sequence |
|----------------|------------------|---------------------|---------------------|-----------------|
| 1 | 新增参数             | 新增参数                | parameter added | 1 |
| 2 | 删除参数             | 删除参数                | parameter deleted | 2 |
| 3 | 修改参数含义           | 修改参数含义              | parameter function modified | 3 |
| 5 | 修改参数取值范围         | 修改参数取值范围            | Value range modified | 5 |
| 6 | 修改关联参数           | 修改关联参数              | Associated parameter modified | 6 |
| 7 | 修改参数默认值          | 修改参数默认值             | Default value modified | 7 |
| 8 | 修改参数推荐值          | 修改参数推荐值             | Recommended value modified | 8 |
| 9 | 修改适用网元           | 修改适用网元              | Applicable NEs modified | 9 |
| 10 | 新增               | 新增                  | New | 12 |
| 11 | 修改               | 修改                  | Modified | 13 |
| 12 | 删除               | 删除                  | Deleted | 14 |
| 13 | 修改参数是否可见         | 修改参数是否可见            | Modify whether the soft parameter is visible | 15 |
| 14 | 修改是否可见           | 修改是否可见              | Whether the modification is visible | 16 |
| 15 | 修改应用场景           | 修改应用场景              | Modified application scenarios. | 10 |
| 16 | 修改生效方式           | 修改生效方式              | Modified the effective mode. | 11 |
| 17 | 修改参数取值说明         | 修改参数取值说明            | Value Description modified | 4 |

---

## 18. type_bit_dict 表（类型枚举与 BIT 位数字典表）

### 表描述
类型枚举与 BIT 位数映射表，用于统一管理“类型枚举”的可选项以及每种类型对应的 BIT 位数上限。**仅支持数据库维护**（页面不提供维护入口）。

### 关键处理规则
- **只读字典表**：后端提供只读查询接口，前端下拉与 BIT 位数计算均依赖该表。
- **类型唯一性**：同一 `type_enum` 仅允许一条记录（建议唯一索引约束）。
- **bit_count 语义**：
  - `bit_count > 0`：参数管理页展示 BIT 勾选，勾选范围为 `1..bit_count`
  - `bit_count = 0`：不展示 BIT 勾选，也不要求填写 `bit_usage`

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 数据长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | type_bit_id | ID | 主键ID | String | 128 | 必填 | 唯一 |
| 2 | type_enum | 类型枚举 | 类型枚举值 | String | 64 | 必填 | 唯一；示例：BIT/BYTE/DWORD/STRING/INT |
| 3 | bit_count | BIT 位数 | BIT 位数上限 | Integer | - | 必填 | 示例：BIT=1、BYTE=8、DWORD=32、STRING=32、INT=0 |

---

## 19. operation_log 表（操作审计日志）

### 表描述
按业务主表（`biz_table`）+ 产品（`owned_product_id`）+ 可选版本（`owned_version_id`）+ 资源（`resource_id`）记录字段级 CREATE/UPDATE/DELETE；一行对应一个展示字段的变更。详设见 `parameter-management-detailed-design.md` §1.7。

### 关键处理规则
- **biz_table**：物理表名，如 `entity_command_mapping`、`command_type_definition`、`command_type_version_range`。
- **版本维度**：无版本页场景查询时**不传** `owned_version_id`，接口层对应 `WHERE owned_version_id IS NULL`。
- **导入批次**：同一次导入请求内多行成功写入可共享 `log_batch_id`（由应用层 `beginImportBatch` 生成）。
- **删除展示**：DELETE 通常写单行，`field_label_cn` 可为「删除对象」等固定文案，`new_value` 存展示用名称。

### 表字段结构

| 字段序号 | 字段名 | 中文名称 | 字段说明 | 数据类型 | 字段长度 | 必填/可选 | 备注 |
|---------|--------|---------|---------|---------|---------|----------|------|
| 1 | log_id | 日志主键 | 唯一 ID | String | 64 | 必填 | 主键 |
| 2 | biz_table | 业务主表 | 物理表名 | String | 128 | 必填 | 与查询筛选一致 |
| 3 | owned_product_id | 产品 ID | 产品形态 ID | String | 255 | 必填 | 关联 entity_basic_info |
| 4 | owned_version_id | 版本 ID | 可选版本范围 | String | 255 | 可选 | 无版本页为 NULL |
| 5 | resource_id | 资源 ID | 命令/类型/区段等业务主键 | String | 255 | 可选 | |
| 6 | operation_type | 操作类型 | CREATE/UPDATE/DELETE | String | 32 | 必填 | |
| 7 | field_label_cn | 变更项 | 中文列名/固定标签 | String | 256 | 可选 | |
| 8 | old_value | 原值 | 修改/删除前 | Text | - | 可选 | |
| 9 | new_value | 新值 | 修改/新增后 | Text | - | 可选 | |
| 10 | operator_id | 操作人 | 用户或 system | String | 128 | 可选 | |
| 11 | operated_at | 操作时间 | 记录时间 | DateTime | - | 必填 | 毫秒精度建议 |
| 12 | log_batch_id | 导入批次 | 同次导入关联 | String | 64 | 可选 | |

---

## 表关联关系说明

### 主要关联关系：
1. **entity_basic_info（产品表）** 是核心主表，所有其他表都通过 `owned_product_id` 或 `product_id` 与之关联
2. **entity_version_info（产品版本表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
3. **entity_command_mapping（产品命令表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
4. **command_type_definition（命令支持的类型及类型范围）** 通过 `owned_product_id` 关联到 **entity_basic_info**
5. **command_type_version_range（命令类型按版本区段划分）** 通过 `command_type_code` 关联到 **command_type_definition**，通过 `owned_product_id` 关联到 **entity_basic_info**
6. **system_parameter（OM系统参数）** 通过 `owned_product_id` 关联到 **entity_basic_info**，通过 `owned_version_id` 关联到 **entity_version_info**，通过 `owned_command_id` 关联到 **entity_command_mapping**
7. **config_change_description（参数变更说明表）** 通过 `parameter_id` 关联到 **system_parameter**，通过 `entity_unique_id` 关联到 **entity_basic_info**，通过 `version_id` 关联到 **entity_version_info**
8. **config_change_description** 通过 `change_type` 关联到 **config_change_type**
9. **entity_applicable_ne_dict（产品适用网元字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
10. **entity_nf_config_dict（产品nf配置字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
11. **entity_effective_mode_dict（产品生效方式配置字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
12. **entity_effective_form_dict（产品生效形态配置字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
13. **entity_module_dict（产品模块字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
14. **version_feature_dict（版本特性字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**，通过 `owned_version_id` 关联到 **entity_version_info**
15. **project_team_dict（项目组字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
16. **entity_business_category（产品业务分类字典表）** 通过 `owned_product_id` 关联到 **entity_basic_info**
17. **type_bit_dict（类型枚举与 BIT 位数字典表）** 为全局只读字典表（不关联产品），供前端下拉与 BIT 位数计算使用

### 字段命名一致性：
- 所有表的产品关联字段统一命名为 `owned_product_id` 或 `owned_product_id`
- 所有表的版本标识字段统一命名为 `owned_version_id` 或 `version_id`
- 所有表的时间戳字段统一命名为 `creation_timestamp` 和 `update_timestamp`
- 所有表的创建者/更新者字段统一命名为 `creator_id` 和 `updater_id`
