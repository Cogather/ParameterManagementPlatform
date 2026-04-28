# 前端开发规范

## 一、项目概述

前端框架项目采用 Vue 3 + TypeScript + Vite 技术栈

## 二、技术栈

### 2.1 核心框架
- **Vue 3.4.25** - 核心前端框架
- **TypeScript 4.4.4** - 类型系统
- **Vite 3.2.11** - 构建工具

### 2.2 UI 组件库
- **Element Plus 2.13.5** - 主要 UI 组件库（用于基础表单、表格、对话框等）
- **@plmcsdk/common-ui 2.6.7** - 通用业务组件库

### 2.3 样式方案
- **WindiCSS** - 原子化 CSS 框架（通过 vite-plugin-windicss）
- **Less** - CSS 预处理器
- **SCSS** - CSS 预处理器（用于全局样式变量）

### 2.4 可视化
- **ECharts 5.6.0** - 数据可视化图表库
- **@antv/g6 4.0.1** - 图可视化引擎

## 三、样式架构

### 3.1 全局样式文件结构

```
public/assets/styles/
├── index.scss          # 样式入口文件
├── base.scss           # 基础样式（滚动条、字体、组件覆盖）
├── common.scss         # 公共样式（卡片、容器、页脚）
├── nprogress.scss      # 进度条样式
└── variables.scss      # 样式变量（主题色）
```

### 3.2 样式导入顺序

在 `index.scss` 中按以下顺序导入：
1. base - 基础样式
2. nprogress - 进度条样式
3. markdown - Markdown 样式
4common - 公共样式

## 四、设计规范

### 4.1 颜色系统

#### 主色调
```scss
// 主题色
$theme-color: #2080f0ff;  // Element Plus 默认蓝色

// 背景色
#edf2f8  // 主背景色（浅灰蓝）
#f5f9fe  // 次级背景色（淡蓝）
#ffffff  // 卡片/内容背景色
#f6f6f8  // 按钮背景色（浅灰）

// 文字色
#333     // 主文字色
#555     // 次级文字色
#666     // 辅助文字色
#888     // 弱文字色
#98a1a9  // 页脚文字色

// 边框色
#eee     // 浅边框
#ddd     // 中等边框
#e1e1e1  // 深边框
#e6edff  // 选中状态边框

// 功能色
#2b91f6  // 链接/激活色
#1c83ff  // 主色调变体
#0064db  // 深蓝色
```

#### 渐变色
```scss
// 提示框渐变
background: linear-gradient(90deg, #dbeafe, #cffafe);
```

### 4.2 字体规范

#### 字体家族
```scss
* {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
  'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}
```

#### WindiCSS 字体配置
```typescript
fontFamily: {
  sans: ['Open Sans', 'ui-sans-serif', 'system-ui'],
  serif: ['Montserrat', 'ui-serif', 'Georgia'],
  mono: ['Fira Sans', 'ui-monospace', 'SFMono-Regular'],
}
```

#### 字号规范
```scss
12px  // 小字号（辅助信息）
14px  // 基础字号（正文）
15px  // 中字号（标签、按钮）
16px  // 大字号（标题）
18px  // 特大字号（页面标题）
20px  // 超大字号（引导文案）
```

#### 字重规范
```scss
400  // 常规（默认）
500  // 中等（表单标签）
600  // 半粗（标题、标签页）
700  // 粗体（强调）
```

### 4.3 间距规范

#### 基础间距
```scss
5px   // 极小间距（图标与文字）
10px  // 小间距（元素之间）
15px  // 中间距（表单项）
20px  // 大间距（卡片内边距）
30px  // 超大间距（区块之间）
```

#### 应用场景
```scss
// 卡片内边距
padding: 20px;

// 表单项间距
margin-bottom: 20px;

// 按钮组间距
margin: 10px 0;

// Flex/Grid 间距
gap: 10px;
```

### 4.4 圆角规范

```scss
2px  // 小圆角（标签、徽章）
4px  // 基础圆角（卡片、按钮、输入框）
8px  // 大圆角（对话框）
28px // 超大圆角（圆形按钮）
```

### 4.5 阴影规范

```scss
// 轻阴影
box-shadow: 3px 3px 3px #eee;

// 基础阴影
box-shadow: 0px 1px 2px rgba(6, 70, 113, 0.08);

// 深阴影（按钮悬停）
box-shadow: 0 3px 6px rgba(0,0,0,0.16);

// 悬停阴影增强
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
```

### 4.6 滚动条样式

```scss
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-thumb {
  background-color: #0003;
  border-radius: 10px;
  transition: all .2s ease-in-out;
}

::-webkit-scrollbar-track {
  border-radius: 10px;
}
```

## 五、组件样式规范

### 5.1 卡片组件

#### 基础卡片
```less
.card-item {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  box-shadow: 0px 1px 2px rgba(6, 70, 113, 0.08);
}
```

#### 卡片标题
```less
.common-table-title {
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.title-top {
  margin-bottom: 15px;
}
```

#### 卡片标签
```less
.item-label {
  text-align: center;
  background-color: #edf2f8;
  line-height: 35px;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  font-size: 15px;
  font-weight: 600;
  color: #666;
}
```

### 5.2 按钮组件

#### 主按钮
```less
.el-button--primary {
  background-color: #2080f0;
  border-color: #2080f0;

  &:hover {
    background-color: #3391ff;
    border-color: #3391ff;
  }
}
```

#### 文字按钮
```less
.el-button--text {
  color: #2b91f6;

  &:hover {
    color: #0064db;
  }
}
```

#### 按钮悬停效果
```less
.btn-hover {
  transition: all 0.2s ease-in-out;
  cursor: pointer;

  &:hover {
    transform: scale(1.02);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}
```

#### 圆形按钮
```less
.btn-div {
  width: 272px;
  height: 56px;
  border-radius: 28px;
  background: #f0f7feff;
  border: 1px solid #e7f4fcff;
  margin: 12px 0;
  box-shadow: 0 3px 6px rgba(0,0,0,0.16);
}
```

### 5.3 表格组件

#### 表格容器
```vue
<el-table
  :data="tableData"
  max-height="300"
  header-row-class-name="headerClass"
  style="width: 100%; margin-bottom: 20px"
>
```

#### 表头样式
```less
::v-deep {
  .headerClass > th {
    background-color: #f5f7fa !important;
    color: #555;
  }

  .el-table__header th {
    background-color: #f5f7fa;
    color: #555;
  }
}
```

### 5.4 表单组件

#### 表单项样式
```less
::v-deep {
  .el-form-item__label {
    font-weight: 500;
    color: #333;
  }

  .el-input__inner {
    border-radius: 4px;
    border: 1px solid #dcdfe6;
  }

  .el-input__icon {
    height: inherit;
    display: flex;
    justify-content: center;
    align-items: center;
  }
}
```

### 5.5 标签页组件

#### 标签页样式
```less
::v-deep {
  .el-tabs__item {
    font-size: 16px;
    font-weight: 600 !important;
  }

  .page-tabs .el-tabs__nav-wrap {
    padding: 0 20px;
    box-sizing: border-box;
    margin-right: 45px;
    margin-left: 30px;
  }
}
```

### 5.6 对话框/抽屉组件

#### 对话框样式
```less
.detailDrawer > .el-drawer__header {
  font-weight: bold;
  color: #555;
}
```

#### 提示框样式
```less
.el-popper.is-customized {
  padding: 6px 12px;
  background: linear-gradient(90deg, #dbeafe, #cffafe);
  color: #333;
}

.el-popper.is-customized .el-popper__arrow::before {
  background: linear-gradient(45deg, #dbeafe, #cffafe);
  right: 0;
}
```

### 5.7 空状态组件

```less
::v-deep {
  .el-empty {
    --el-empty-padding: 0;
    --el-empty-image-width: 33px;
  }
}
```

## 六、布局规范

### 6.1 主容器布局

```vue
<el-container>
  <el-main class="common-main">
    <!-- 内容区域 -->
  </el-main>
  <el-footer class="common-footer" height="34px">
    <!-- 底部 -->
  </el-footer>
</el-container>
```

#### 主容器样式
```less
.common-main {
  max-height: calc(100vh - 78px);
  overflow-y: auto;
  padding: 0 !important;
  background-color: #edf2f8;
}
```

#### 6.1.1 param-web 子应用与宿主视觉对齐

- **颜色 / 字体 / 圆角 / 阴影**：与 §4.1～§4.5 及 §5.1 卡片一致；主区背景 **`#edf2f8`**，卡片 **`.card-item`** 内边距 **20px**、阴影 **`0px 1px 2px rgba(6, 70, 113, 0.08)`**。
- **业务主卡片**：**`.page-card.card-item`** 外边距 **20px**、内边距 **20px**（与「卡片内边距」同档）；**嵌入宿主 iframe**（`app-shell--embedded`）时在 `public/assets/styles/common.scss` 中另设较紧梯度。
- **主区高度**：宿主全框架（顶栏等）占位 **78px** 时用 **`calc(100vh - 78px)`**；**param-web** 仅含单行工具条时，工程内为 **`calc(100vh - 44px)`**（以 `common.scss` 为准）。
- **表格表头**：**`.common-main .el-table .el-table__header th`** 背景 **`#f5f7fa`**、文字色 **`#555`**，与 §5.3 一致。
- **无产品上下文**：列表以 **空表格 + `empty-text`** 提示「请先选择产品」等，与 **`openspec/spec-04-integration.md`** 一致。

#### 页脚样式
```less
.common-footer {
  width: 100%;
  position: absolute;
  bottom: 0;
  background-color: #273039;
  text-align: center;
  line-height: 34px;
  color: #98a1a9;
  font-size: 15px;
}
```

### 6.2 栅格布局

```vue
<el-row :gutter="20">
  <el-col :xs="18" :sm="18" :md="18" :lg="18" :xl="18">
    <!-- 内容 -->
  </el-col>
</el-row>
```

### 6.3 Flexbox 布局

#### 居中对齐
```less
.flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}
```

#### 两端对齐
```less
.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
```

#### 垂直布局
```less
.flex-column {
  display: flex;
  flex-direction: column;
}
```

### 6.4 响应式设计

#### 响应式断点
```less
// 使用 Element Plus 栅格系统
:xs="12" :sm="12" :md="12" :lg="12" :xl="12"

// 媒体查询
@media (max-width: 768px) {
  .desk-entrance {
    width: 100%;
    margin: 5px 0;
  }
}

// 最小宽度限制
.header {
  min-width: 1500px;
  margin: -35px 0 0;
}
```

## 七、特殊组件样式

### 7.1 首页入口卡片

```less
.desk-entrance {
  border-radius: 4px;
  height: 65px;
  padding: 16px;
  box-shadow: 3px 3px 3px #eee;
  cursor: pointer;
  background-color: #f6f6f8ff;
  border: 1px solid #f5f9fe;
  display: flex;
  justify-content: space-between;

  &:hover {
    box-shadow: 3px 3px 3px #ddd;
    border: 1px solid #ddd;
    transform: scale(1.02);
    transition-duration: 0.2s;
  }
}
```

### 7.2 流程项样式

```less
.process-item {
  display: inline-block;
  padding: 0;
  line-height: 45px;
  font-size: 15px;
  text-align: center;
  user-select: none;
  border: 1px solid #eee;
  box-shadow: 3px 3px 3px #eee;
  cursor: pointer;
  margin-bottom: 10px;
  background-color: #fff;
  border-radius: var(--el-border-radius-base);

  &:hover {
    border: 1px solid #2b91f6 !important;
    box-shadow: none;
  }
}
```

### 7.3 底部项目样式

```less
.bottom-item {
  display: inline-block;
  padding: 0 10px;
  line-height: 35px;
  height: 35px;
  font-size: 15px;
  text-align: center;
  user-select: none;
  border: 1px solid #e6edff;
  margin: 10px;
  border-radius: var(--el-border-radius-base);
  width: 135px;
  background-color: #e6edff;
  cursor: pointer;
}
```

## 八、样式最佳实践

### 8.1 样式穿透

在 Vue 组件中使用深度选择器：

```less
::v-deep {
  .el-empty {
    --el-empty-padding: 0;
  }

  .el-card__body {
    padding: 15px;
  }
}

// 或使用 /deep/
/deep/ .el-tabs__item {
  font-size: 16px;
  font-weight: 600 !important;
}
```

### 8.2 文本省略

#### 单行省略
```less
.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

#### 多行省略
```less
.text-ellipsis-multiline {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

### 8.3 过渡动画

```less
// 悬停过渡
transition: all 0.2s ease-in-out;

// 滚动条过渡
transition: all .2s ease-in-out;

// 变换过渡
&:hover {
  transform: scale(1.02);
  transition-duration: 0.2s;
}
```

### 8.4 用户选择

```less
user-select: none;  // 禁止选择文本
```

### 8.5 SVG 样式

```scss
svg {
  display: inline-block;
}
```

## 九、开发建议

### 9.1 新服务开发样式指南

当开发新的前端服务时，遵循以下步骤确保与框架风格一致：

#### 1. 引入框架样式
```typescript
// 在 main.ts 中引入
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/display.css'
```

#### 2. 使用框架颜色变量
```scss
// 使用框架定义的颜色
background-color: #edf2f8;  // 主背景色
color: #333;  // 主文字色
border: 1px solid #eee;  // 浅边框
```

#### 3. 遵循间距规范
```scss
padding: 20px;  // 卡片内边距
margin: 10px 0;  // 元素间距
gap: 10px;  // Flex/Grid 间距
```

#### 4. 使用框架组件样式
```vue
<!-- 使用 Element Plus 组件 -->
<el-card class="card-item">
  <div class="common-table-title">标题</div>
  <!-- 内容 -->
</el-card>
```

#### 5. 应用悬停效果
```less
.my-component {
  transition: all 0.2s ease-in-out;

  &:hover {
    transform: scale(1.02);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}
```

### 9.2 样式文件组织

```
src/styles/
├── variables.scss    # 样式变量
├── mixins.scss       # Mixins 工具类
├── components.scss   # 组件样式
└── layout.scss       # 布局样式
```

### 9.3 代码风格

- 使用 2 空格缩进
- 使用单引号
- 样式属性按字母顺序排列
- 使用语义化的 class 命名
- 避免使用 !important（除非必要）

## 十、总结

核心设计特点包括：

1. **统一的设计系统**：明确的颜色、字体、间距、圆角、阴影规范
2. **组件化样式**：卡片、按钮、表格、表单等组件有统一样式
3. **响应式设计**：支持多端适配

新服务开发时，直接使用框架定义的颜色、间距、组件样式，即可与框架保持一致的视觉风格。
