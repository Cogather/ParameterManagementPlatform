## 1. 数据库与实体

- [x] 1.1 `schema.sql` 增列：`is_published`、`no_publish_reason`、`related_license`、`product_form_id`、`platform_generation`、`application_region`、`value_range_segments`
- [x] 1.2 更新 `SystemParameterPo` 及 MyBatis 映射
- [x] 1.3 编写区间拼接/重叠校验工具类（单测：拼接、重叠、255 长度）

## 2. 后端校验与默认值

- [x] 2.1 收缩 `ParameterDefaults`：仅保留 `tenant_id`、`data_status`、`introduce_type` 等系统默认
- [x] 2.2 `ParameterAppService`：create 校验基础信息 + 区间 + 变更说明中文；update 叠加详细信息必填
- [x] 2.3 `validateChangeDescriptions`：仅校验中文原因/影响；保留 export_delta 条件必填
- [x] 2.4 保存时写入 `value_range_segments` 并拼接 `value_range`
- [x] 2.5 `ParameterVersionCopyAppService` 复制路径覆盖新字段
- [x] 2.6 单元测试：create 详细信息 NULL、edit 详细信息必填、区间重叠拒绝

## 3. 前端表单重构

- [x] 3.1 `ParameterLayout.vue`：两段式分区与字段顺序（基础 12 + 详细 29）
- [x] 3.2 取值区间多段 UI（增删段、隐藏 `value_range` 输入）
- [x] 3.3 `validateForm` 按 `dialogMode` 分支；条件校验不发布原因
- [x] 3.4 新字段：是否发布、平台代际、应用区域、关联 License、影响级别、内部功能描述
- [x] 3.5 产品形态下拉：`fetchEntityBasicInfoPage({ productId })`，落库 `product_form_id`
- [x] 3.6 单位改名并移入基础信息；引入版本继承时只读
- [x] 3.7 移除枚举/参数范围；隐藏立即生效/变更来源/版本号
- [x] 3.8 变更说明：仅中文必填校验
- [x] 3.9 更新 `api/parameter.ts` 类型定义

## 4. 连带与文档

- [x] 4.1 导入导出列映射与 `OperationLogAppService` 字段标签（导入导出表头对齐见 change `parameter-import-export-align`）
- [x] 4.2 同步 `docs/parameter-management-detailed-design.md` §13.2
- [x] 4.3 同步 `docs/table字段简介.md` §15～§16
- [ ] 4.4 归档 change 后合并 delta 至 `openspec/specs/parameter-core`
- [ ] 4.5 手工验收：新增仅基础信息、编辑补详细信息、区间拼接、继承引入版本只读
