-- 本地/开发环境初始化（GaussDB / openGauss，PostgreSQL 协议）
-- 注意：此文件使用 PostgreSQL/openGauss 方言；不包含 MySQL 的 ENGINE/CHARSET/COLLATE 语法

-- 产品主数据上下文（详设/表字段简介：entity_basic_info）
CREATE TABLE entity_basic_info (
  entity_name VARCHAR(255) NULL,
  product_form_id VARCHAR(50) NOT NULL,
  product_soft_param_type VARCHAR(50) NULL,
  product_form VARCHAR(255) NULL,
  product_id VARCHAR(50) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  owner_list VARCHAR(255) NULL,
  entity_status INT NULL,
  PRIMARY KEY (product_form_id)
);

-- 开发/联调用 Mock：产品与产品形态（顶栏 product_id 与 product_demo / product_alpha / product_beta 对齐）
INSERT INTO entity_basic_info (
  entity_name,
  product_form_id,
  product_soft_param_type,
  product_form,
  product_id,
  creator_id,
  creation_timestamp,
  updater_id,
  update_timestamp,
  owner_list,
  entity_status
) VALUES
  ('演示产品', 'pform_demo_std', 'Single', '标准版', 'product_demo', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'zhangsan,lisi', 1),
  ('演示产品', 'pform_demo_plus', 'Multi', '增强版', 'product_demo', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'zhangsan', 1),
  ('演示产品', 'pform_demo_lite', 'Single', '精简版', 'product_demo', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'wangwu', 1),
  ('Alpha 产品', 'pform_alpha_main', 'Single', '主形态', 'product_alpha', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'owner_a', 1),
  ('Alpha 产品', 'pform_alpha_ext', 'Multi', '扩展形态', 'product_alpha', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'owner_a,owner_b', 1),
  ('Beta 产品', 'pform_beta_main', 'Multi', '主形态', 'product_beta', 'mock', CURRENT_TIMESTAMP(3), 'mock', CURRENT_TIMESTAMP(3), 'owner_beta', 1)
;

CREATE TABLE entity_version_info (
  owned_product_id VARCHAR(255) NOT NULL,
  version_id VARCHAR(255) NOT NULL,
  version_name VARCHAR(255) NULL,
  version_number VARCHAR(128) NULL,
  version_description VARCHAR(1024) NULL,
  version_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (version_id)
);

CREATE TABLE entity_business_category (
  owned_product_id VARCHAR(255) NOT NULL,
  category_id VARCHAR(255) NOT NULL,
  category_name_cn VARCHAR(256) NULL,
  category_name_en VARCHAR(256) NULL,
  feature_range VARCHAR(1024) NULL,
  category_type VARCHAR(256) NULL,
  category_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (category_id)
);

CREATE TABLE config_change_source_keyword (
  owned_product_id VARCHAR(255) NOT NULL,
  keyword_id VARCHAR(255) NOT NULL,
  keyword_regex VARCHAR(512) NULL,
  reason VARCHAR(512) NULL,
  keyword_description VARCHAR(1024) NULL,
  keyword_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (keyword_id)
);

CREATE TABLE entity_applicable_ne_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  ne_type_id VARCHAR(255) NOT NULL,
  ne_type_name_cn VARCHAR(256) NULL,
  ne_type_description VARCHAR(1024) NULL,
  ne_type_status INT NULL,
  product_form VARCHAR(255) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (ne_type_id)
);

CREATE TABLE entity_nf_config_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  nf_config_id VARCHAR(255) NOT NULL,
  nf_config_name_cn VARCHAR(256) NULL,
  nf_config_description VARCHAR(1024) NULL,
  nf_config_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (nf_config_id)
);

CREATE TABLE entity_effective_mode_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  effective_mode_id VARCHAR(255) NOT NULL,
  effective_mode_name_cn VARCHAR(256) NULL,
  effective_mode_name_en VARCHAR(256) NULL,
  effective_mode_description VARCHAR(1024) NULL,
  effective_mode_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (effective_mode_id)
);

CREATE TABLE entity_effective_form_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  effective_form_id VARCHAR(255) NOT NULL,
  effective_form_name_cn VARCHAR(256) NULL,
  effective_form_name_en VARCHAR(512) NULL,
  effective_form_description VARCHAR(1024) NULL,
  effective_form_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (effective_form_id)
);

CREATE TABLE version_feature_dict (
  owned_product_pbi_id VARCHAR(255) NOT NULL,
  owned_version_id VARCHAR(255) NOT NULL,
  feature_id VARCHAR(255) NOT NULL,
  feature_code VARCHAR(256) NULL,
  feature_name_cn VARCHAR(512) NULL,
  feature_name_en VARCHAR(1024) NULL,
  introduce_type VARCHAR(128) NULL,
  inherit_reference_version_id VARCHAR(255) NULL,
  feature_status INT NULL,
  introduced_product_id VARCHAR(255) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (feature_id)
);

-- 类型枚举与 BIT 位数定义（仅数据库维护；前端下拉/参数位数依赖）
CREATE TABLE type_bit_dict (
  type_bit_id VARCHAR(128) NOT NULL,
  type_enum VARCHAR(64) NOT NULL,
  bit_count INT NOT NULL,
  PRIMARY KEY (type_bit_id)
);

-- 避免重复插入：同 type_enum 只保留一条
CREATE UNIQUE INDEX ux_type_bit_dict_enum ON type_bit_dict(type_enum);

INSERT INTO type_bit_dict(type_bit_id, type_enum, bit_count) VALUES
  ('tb_bit', 'BIT', 1),
  ('tb_byte', 'BYTE', 8),
  ('tb_dword', 'DWORD', 32),
  ('tb_string', 'STRING', 32),
  ('tb_int', 'INT', 0)
;

CREATE TABLE project_team_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  team_id VARCHAR(255) NOT NULL,
  team_name VARCHAR(512) NULL,
  team_description VARCHAR(1024) NULL,
  team_status INT NULL,
  owner_list VARCHAR(255) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (team_id)
);

-- 产品模块字典表（详设/表字段简介：entity_module_dict）
CREATE TABLE entity_module_dict (
  owned_product_id VARCHAR(255) NOT NULL,
  module_id VARCHAR(255) NOT NULL,
  module_name_cn VARCHAR(255) NULL,
  module_name_en VARCHAR(512) NULL,
  module_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (module_id)
);

-- 命令管理域（spec-01 / 表文档 §2～§4）
CREATE TABLE entity_command_mapping (
  owned_product_id VARCHAR(255) NOT NULL,
  command_id VARCHAR(255) NOT NULL,
  command_name VARCHAR(255) NOT NULL,
  command_description VARCHAR(500) NULL,
  min_version VARCHAR(50) NULL,
  max_version VARCHAR(50) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  owner_list VARCHAR(255) NULL,
  command_status INT NULL,
  PRIMARY KEY (command_id)
);

CREATE TABLE command_type_definition (
  owned_product_id VARCHAR(255) NOT NULL,
  owned_command_id VARCHAR(255) NOT NULL,
  command_type_id VARCHAR(255) NOT NULL,
  command_type_name VARCHAR(255) NULL,
  command_type VARCHAR(64) NULL,
  min_value INT NULL,
  max_value INT NULL,
  occupied_serial_number VARCHAR(512) NULL,
  command_type_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (command_type_id)
);

-- 参数核心（与 SystemParameterPo、设计文档 §15 对齐）
CREATE TABLE system_parameter (
  parameter_id SERIAL PRIMARY KEY,
  parameter_code VARCHAR(50) NULL,
  owned_object_type VARCHAR(64) NULL,
  owned_object_code VARCHAR(64) NULL,
  tenant_id VARCHAR(32) NULL,
  domain_id INT NULL,
  data_status VARCHAR(50) NULL,
  owned_product_id VARCHAR(255) NULL,
  owned_version_id VARCHAR(255) NULL,
  owned_command_id VARCHAR(255) NULL,
  introduce_type VARCHAR(50) NULL,
  inherit_reference_version_id VARCHAR(255) NULL,
  parameter_name_cn VARCHAR(255) NULL,
  parameter_name_en VARCHAR(512) NULL,
  bit_usage VARCHAR(255) NULL,
  parameter_sequence INT NULL,
  value_range VARCHAR(255) NULL,
  value_description_cn VARCHAR(1024) NULL,
  value_description_en VARCHAR(2048) NULL,
  application_scenario_cn VARCHAR(1024) NULL,
  application_scenario_en VARCHAR(2048) NULL,
  parameter_default_value VARCHAR(255) NULL,
  parameter_recommended_value VARCHAR(255) NULL,
  applicable_ne VARCHAR(255) NULL,
  feature VARCHAR(512) NULL,
  feature_id VARCHAR(255) NULL,
  business_classification VARCHAR(255) NULL,
  category_id VARCHAR(255) NULL,
  take_effect_immediately VARCHAR(50) NULL,
  effective_mode_cn VARCHAR(255) NULL,
  effective_mode_en VARCHAR(512) NULL,
  project_team VARCHAR(255) NULL,
  belonging_module VARCHAR(255) NULL,
  patch_version VARCHAR(255) NULL,
  introduced_version VARCHAR(255) NULL,
  parameter_description_cn VARCHAR(1024) NULL,
  parameter_description_en VARCHAR(2048) NULL,
  impact_description_cn VARCHAR(1024) NULL,
  impact_description_en VARCHAR(2048) NULL,
  configuration_example_cn TEXT NULL,
  configuration_example_en TEXT NULL,
  help_document_in_database VARCHAR(50) NULL,
  related_parameter_cn VARCHAR(255) NULL,
  related_parameter_en VARCHAR(1024) NULL,
  related_parameter_usage VARCHAR(1024) NULL,
  related_parameter_description_cn VARCHAR(1024) NULL,
  related_parameter_description_en VARCHAR(2048) NULL,
  remark VARCHAR(1024) NULL,
  parameter_bit VARCHAR(255) NULL,
  enumeration_values_cn VARCHAR(512) NULL,
  enumeration_values_en VARCHAR(1024) NULL,
  applicable_logical_entity_cn VARCHAR(255) NULL,
  applicable_logical_entity_en VARCHAR(512) NULL,
  related_feature_cn VARCHAR(512) NULL,
  related_feature_en VARCHAR(1024) NULL,
  change_factors VARCHAR(100) NULL,
  change_related_number VARCHAR(255) NULL,
  change_source VARCHAR(512) NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  parameter_unit_cn VARCHAR(50) NULL,
  parameter_unit_en VARCHAR(50) NULL,
  parameter_range VARCHAR(100) NULL,
  effective_form_cn VARCHAR(255) NULL,
  effective_form_en VARCHAR(512) NULL,
  implementation_principle_cn VARCHAR(255) NULL,
  implementation_principle_en VARCHAR(512) NULL,
  impact_level_cn VARCHAR(255) NULL,
  impact_level_en VARCHAR(512) NULL,
  figure_example_cn VARCHAR(1024) NULL,
  figure_example_en VARCHAR(2048) NULL,
  internal_description VARCHAR(1024) NULL
);

CREATE TABLE config_change_description (
  change_description_id VARCHAR(255) NOT NULL PRIMARY KEY,
  parameter_id INT NULL,
  change_type VARCHAR(255) NULL,
  change_reason_cn VARCHAR(1024) NULL,
  change_impact_cn VARCHAR(1024) NULL,
  change_reason_en VARCHAR(1024) NULL,
  change_impact_en VARCHAR(1024) NULL,
  export_delta VARCHAR(50) NULL,
  no_export_reason VARCHAR(1024) NULL,
  entity_unique_id VARCHAR(255) NULL,
  version_id VARCHAR(255) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL
);

CREATE TABLE config_change_type (
  change_type_id SERIAL PRIMARY KEY,
  change_type_name VARCHAR(100) NULL,
  change_type_name_cn VARCHAR(100) NULL,
  change_type_name_en VARCHAR(100) NULL,
  change_sequence INT NULL
);

-- 系统初始化默认数据（详设/表字段简介：共 17 条，且无 4）
INSERT INTO config_change_type(change_type_id, change_type_name, change_type_name_cn, change_type_name_en, change_sequence) VALUES
  (1,  '新增参数',         '新增参数',         'parameter added',                                   1),
  (2,  '删除参数',         '删除参数',         'parameter deleted',                                 2),
  (3,  '修改参数含义',     '修改参数含义',     'parameter function modified',                       3),
  (5,  '修改参数取值范围', '修改参数取值范围', 'Value range modified',                              5),
  (6,  '修改关联参数',     '修改关联参数',     'Associated parameter modified',                     6),
  (7,  '修改参数默认值',   '修改参数默认值',   'Default value modified',                            7),
  (8,  '修改参数推荐值',   '修改参数推荐值',   'Recommended value modified',                        8),
  (9,  '修改适用网元',     '修改适用网元',     'Applicable NEs modified',                           9),
  (10, '新增',             '新增',             'New',                                               12),
  (11, '修改',             '修改',             'Modified',                                          13),
  (12, '删除',             '删除',             'Deleted',                                           14),
  (13, '修改参数是否可见', '修改参数是否可见', 'Modify whether the soft parameter is visible',      15),
  (14, '修改是否可见',     '修改是否可见',     'Whether the modification is visible',               16),
  (15, '修改应用场景',     '修改应用场景',     'Modified application scenarios.',                   10),
  (16, '修改生效方式',     '修改生效方式',     'Modified the effective mode.',                      11),
  (17, '修改参数取值说明', '修改参数取值说明', 'Value Description modified',                         4)
;

CREATE TABLE command_type_version_range (
  version_range_id VARCHAR(255) NOT NULL,
  owned_product_id VARCHAR(255) NULL,
  owned_command_id VARCHAR(255) NULL,
  owned_type_id VARCHAR(255) NULL,
  start_index INT NULL,
  end_index INT NULL,
  range_description VARCHAR(512) NULL,
  range_type VARCHAR(50) NULL,
  owned_version_or_business_id VARCHAR(255) NULL,
  range_status INT NULL,
  creator_id VARCHAR(50) NULL,
  creation_timestamp TIMESTAMP(3) NULL,
  updater_id VARCHAR(50) NULL,
  update_timestamp TIMESTAMP(3) NULL,
  PRIMARY KEY (version_range_id)
);

-- 操作审计（详设 parameter-management §1.7，单表）
CREATE TABLE operation_log (
  log_id VARCHAR(64) NOT NULL,
  biz_table VARCHAR(128) NOT NULL,
  owned_product_id VARCHAR(255) NOT NULL,
  owned_version_id VARCHAR(255) NULL,
  resource_id VARCHAR(255) NULL,
  resource_name VARCHAR(512) NULL,
  operation_type VARCHAR(32) NOT NULL,
  field_label_cn VARCHAR(256) NULL,
  old_value TEXT NULL,
  new_value TEXT NULL,
  operator_id VARCHAR(128) NULL,
  operated_at TIMESTAMP(3) NOT NULL,
  log_batch_id VARCHAR(64) NULL,
  PRIMARY KEY (log_id)
);
CREATE INDEX idx_operation_log_scope_time
  ON operation_log (biz_table, owned_product_id, owned_version_id, operated_at DESC);
