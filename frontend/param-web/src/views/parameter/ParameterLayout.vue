<template>
  <el-main class="common-main page-main">
    <el-alert
      v-if="!selectedProductId"
      title="请先选择产品"
      type="warning"
      show-icon
      style="margin-bottom: 8px"
    />

    <el-form :inline="true" class="filter-row">
          <el-form-item label="版本" required>
            <el-select
              v-model="versionId"
              placeholder="请选择版本或 ALL"
              style="width: 280px"
              clearable
              filterable
              :disabled="!selectedProductId"
              @change="onVersionChange"
            >
              <el-option v-for="v in displayVersionOptions" :key="v.versionId" :label="v.versionName" :value="v.versionId" />
            </el-select>
          </el-form-item>
        </el-form>

        <el-container class="param-split">
            <el-aside width="280px" class="param-aside">
              <div class="aside-title">命令 / 类型</div>
              <el-tree
                ref="treeRef"
                v-loading="treeLoading"
                :data="treeData"
                node-key="id"
                highlight-current
                default-expand-all
                :props="{ label: 'label', children: 'children' }"
                @node-click="onTreeNodeClick"
              />
            </el-aside>
            <el-main class="param-main-inner">
              <div class="param-toolbar param-toolbar-with-oplog">
                <div class="param-toolbar-main">
                <el-button type="primary" :disabled="!canQuery || isAllVersionsView" @click="openCreate">新增</el-button>
                <el-button :disabled="!canImport" @click="importVisible = true">导入</el-button>
                <el-button :disabled="!canQuery || isAllVersionsView" @click="openExport">导出</el-button>
                <el-tooltip content="暂缓" placement="top">
                  <span class="sync-wrap">
                    <el-button disabled>参数同步</el-button>
                  </span>
                </el-tooltip>
                <el-tag v-if="canQuery" type="info" effect="plain" class="baseline-tag">
                  已基线参数：{{ baselineCount }}{{ isAllVersionsView ? '（全产品合计）' : '' }}
                </el-tag>
                <el-button text type="primary" :disabled="!canQuery" @click="loadRows">刷新列表</el-button>
                </div>
                <el-button
                  v-if="selectedProductId && versionId"
                  link
                  type="primary"
                  class="op-log-link"
                  @click="opLogOpen = true"
                >
                  操作日志
                </el-button>
              </div>
              <operation-log-drawer
                v-model="opLogOpen"
                :product-id="selectedProductId"
                biz-table="system_parameter"
                :version-id="paramOpLogVersionId"
                :ignore-version-filter="paramOpLogIgnoreAllVersions"
                :resource-display-map="opLogResourceMap"
                group-by-operation
              />

              <el-table
                v-loading="tableLoading"
                :data="rows"
                border
                style="width: 100%; margin-bottom: 8px"
                max-height="480"
                :empty-text="tableEmptyText"
              >
                <el-table-column type="expand" width="42">
                  <template #default="{ row }">
                    <div class="param-expand">
                      <div class="param-expand-grid">
                        <div class="kv"><span class="k">参数编码</span><span class="v">{{ row.parameterCode || '—' }}</span></div>
                        <div class="kv"><span class="k">名称（中）</span><span class="v">{{ row.parameterNameCn || '—' }}</span></div>
                        <div class="kv"><span class="k">名称（英）</span><span class="v">{{ row.parameterNameEn || '—' }}</span></div>
                        <div class="kv"><span class="k">归属命令</span><span class="v">{{ commandNameForTable(String(row.ownedCommandId || '')) }}</span></div>
                        <div class="kv"><span class="k">所属版本ID</span><span class="v">{{ row.ownedVersionId || '—' }}</span></div>
                        <div class="kv"><span class="k">引入类型</span><span class="v">{{ row.introduceType || '—' }}</span></div>
                        <div class="kv"><span class="k">继承/引用版本</span><span class="v">{{ row.inheritReferenceVersionId || '—' }}</span></div>
                        <div class="kv"><span class="k">取值范围</span><span class="v">{{ row.valueRange || '—' }}</span></div>
                        <div class="kv"><span class="k">参数默认值</span><span class="v">{{ row.parameterDefaultValue || '—' }}</span></div>
                        <div class="kv"><span class="k">参数推荐值</span><span class="v">{{ row.parameterRecommendedValue || '—' }}</span></div>
                        <div class="kv"><span class="k">适用网元</span><span class="v">{{ row.applicableNe || '—' }}</span></div>
                        <div class="kv"><span class="k">所属特性</span><span class="v">{{ row.feature || row.featureId || '—' }}</span></div>
                        <div class="kv"><span class="k">业务分类</span><span class="v">{{ row.businessClassification || row.categoryId || '—' }}</span></div>
                        <div class="kv"><span class="k">立即生效</span><span class="v">{{ row.takeEffectImmediately || '—' }}</span></div>
                        <div class="kv"><span class="k">生效方式（中）</span><span class="v">{{ row.effectiveModeCn || '—' }}</span></div>
                        <div class="kv"><span class="k">生效方式（英）</span><span class="v">{{ row.effectiveModeEn || '—' }}</span></div>
                        <div class="kv"><span class="k">生效形态（中）</span><span class="v">{{ row.effectiveFormCn || '—' }}</span></div>
                        <div class="kv"><span class="k">生效形态（英）</span><span class="v">{{ row.effectiveFormEn || '—' }}</span></div>
                        <div class="kv"><span class="k">项目组</span><span class="v">{{ row.projectTeam || '—' }}</span></div>
                        <div class="kv"><span class="k">归属模块</span><span class="v">{{ row.belongingModule || '—' }}</span></div>
                        <div class="kv"><span class="k">版本号</span><span class="v">{{ row.patchVersion || '—' }}</span></div>
                        <div class="kv"><span class="k">引入版本</span><span class="v">{{ row.introducedVersion || '—' }}</span></div>
                        <div class="kv"><span class="k">参数单位（中）</span><span class="v">{{ row.parameterUnitCn || '—' }}</span></div>
                        <div class="kv"><span class="k">参数单位（英）</span><span class="v">{{ row.parameterUnitEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">参数含义（中）</span><span class="v">{{ row.parameterDescriptionCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">参数含义（英）</span><span class="v">{{ row.parameterDescriptionEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">取值说明（中）</span><span class="v">{{ row.valueDescriptionCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">取值说明（英）</span><span class="v">{{ row.valueDescriptionEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">应用场景（中）</span><span class="v">{{ row.applicationScenarioCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">应用场景（英）</span><span class="v">{{ row.applicationScenarioEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">影响说明（中）</span><span class="v">{{ row.impactDescriptionCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">影响说明（英）</span><span class="v">{{ row.impactDescriptionEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">配置举例（中）</span><span class="v">{{ row.configurationExampleCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">配置举例（英）</span><span class="v">{{ row.configurationExampleEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">关联参数描述（中）</span><span class="v">{{ row.relatedParameterDescriptionCn || '—' }}</span></div>
                        <div class="kv full"><span class="k">关联参数描述（英）</span><span class="v">{{ row.relatedParameterDescriptionEn || '—' }}</span></div>
                        <div class="kv full"><span class="k">备注</span><span class="v">{{ row.remark || '—' }}</span></div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column v-if="isAllVersionsView" label="所属版本" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ versionNameForTable(String(row.ownedVersionId || '')) }}
                  </template>
                </el-table-column>
                <el-table-column prop="parameterCode" label="参数编码" min-width="140" show-overflow-tooltip />
                <el-table-column prop="parameterNameCn" label="名称（中）" min-width="140" show-overflow-tooltip />
                <el-table-column prop="parameterNameEn" label="名称（英）" min-width="140" show-overflow-tooltip />
                <el-table-column label="归属命令" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ commandNameForTable(String(row.ownedCommandId || '')) }}
                  </template>
                </el-table-column>
                <el-table-column prop="parameterSequence" label="序号" width="72" />
                <el-table-column prop="bitUsage" label="BIT 占用" width="110" show-overflow-tooltip />
                <el-table-column prop="dataStatus" label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag v-if="isBaseline(row)" type="info" size="small">已基线</el-tag>
                    <span v-else>{{ row.dataStatus }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="effectiveModeCn" label="生效方式" min-width="120" show-overflow-tooltip />
                <el-table-column prop="projectTeam" label="项目组" min-width="120" show-overflow-tooltip />
                <el-table-column prop="changeSource" label="变更来源" min-width="120" show-overflow-tooltip />
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" :disabled="!canQuery || isBaseline(row)" @click="openEdit(row)">
                      编辑
                    </el-button>
                    <el-button link type="danger" :disabled="!canQuery || isBaseline(row)" @click="removeRow(row)">
                      删除
                    </el-button>
                    <el-button v-if="!isBaseline(row)" link type="warning" :disabled="!canQuery" @click="doBaseline(row)">基线</el-button>
                    <el-button v-else link type="success" :disabled="!canQuery" @click="doUnbaseline(row)">解锁</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-pagination
                v-model:current-page="page"
                v-model:page-size="size"
                :total="total"
                :page-sizes="[20, 50, 100]"
                layout="total, sizes, prev, pager, next"
                :disabled="!canQuery"
                @current-change="loadRows"
                @size-change="loadRows"
              />
            </el-main>
          </el-container>

        <el-dialog
          v-model="dialogVisible"
          :title="dialogTitle"
          width="1100px"
          destroy-on-close
          class="param-dialog"
          @closed="onDialogClosed"
        >
          <div v-loading="masterdataLoading" class="param-form-scroll">
            <el-form label-width="150px" @submit.prevent>
              <el-divider content-position="left">1. 基础标识（spec-03 §3.1）</el-divider>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item required>
                    <template #label>
                      <span>参数名称（中）</span>
                      <el-tooltip content="语义为「参数标题」；示例文案见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.parameterNameCn" placeholder="参数标题" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="参数名称（英）">
                    <el-input v-model="formMain.parameterNameEn" placeholder="自动翻译或占位，可校对" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="归属命令" required>
                <el-select
                  v-model="formMain.ownedCommandId"
                  filterable
                  placeholder="请选择命令"
                  style="width: 100%"
                  :disabled="dialogMode === 'edit'"
                  :loading="commandSelectLoading"
                >
                  <el-option
                    v-for="c in commandSelectOptions"
                    :key="c.commandId"
                    :label="c.commandName"
                    :value="c.commandId"
                  />
                </el-select>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="参数类型" required>
                    <el-select
                      v-model="formTypeCode"
                      style="width: 100%"
                      :disabled="createTypeLocked"
                      @change="onTypeChange"
                    >
                      <el-option v-for="t in typeOptionsForDialog" :key="t" :label="t" :value="t" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="序号" required>
                    <el-input-number v-model="formSequence" :min="1" :max="9999" :step="1" style="width: 100%" @change="onTypeOrSeqChange" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label=" ">
                    <el-button :disabled="!canLoadAllocation" @click="loadSeqOptions">可用序号</el-button>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item v-if="seqOptions.length" label="可用序号列表">
                <el-select v-model="formSequence" placeholder="FULL / PARTIAL" style="width: 100%" @change="onSequencePick">
                  <el-option
                    v-for="s in seqOptions"
                    :key="s.sequence"
                    :label="`序号 ${s.sequence}（${s.availability === 'FULL' ? '完全可用' : '部分可用'}）`"
                    :value="s.sequence"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="取值范围">
                <el-input v-model="formMain.valueRange" placeholder="文本，不做格式校验" />
              </el-form-item>
              <el-form-item v-if="maxBitsForCurrentType > 0" label="使用 BIT 位" required>
                <div class="bit-check-grid">
                  <el-checkbox
                    v-for="n in bitCheckboxRange"
                    :key="n"
                    :model-value="selectedBits.includes(n)"
                    :disabled="isBitCheckboxDisabled(n)"
                    :class="{ 'bit-occupied': isBitOccupiedGray(n) }"
                    @update:model-value="(v: boolean) => toggleBit(n, v)"
                  >
                    BIT{{ n }}
                  </el-checkbox>
                </div>
                <el-button style="margin-top: 8px" :disabled="!canLoadBits" @click="loadBitPool">加载可选 BIT</el-button>
                <div class="hint">英文逗号分隔落库；请先加载可选 BIT。</div>
              </el-form-item>

              <el-divider content-position="left">2. 取值与场景</el-divider>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item>
                    <template #label>
                      <span>取值说明（中）</span>
                      <el-tooltip content="示例见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.valueDescriptionCn" type="textarea" :rows="2" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="取值说明（英）">
                    <el-input v-model="formMain.valueDescriptionEn" type="textarea" :rows="2" placeholder="默认/翻译" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item>
                    <template #label>
                      <span>应用场景（中）</span>
                      <el-tooltip content="示例见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.applicationScenarioCn" type="textarea" :rows="2" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="应用场景（英）">
                    <el-input v-model="formMain.applicationScenarioEn" type="textarea" :rows="2" placeholder="默认/翻译" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="参数默认值">
                    <el-input v-model="formMain.parameterDefaultValue" placeholder="整数" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="参数推荐值">
                    <el-input v-model="formMain.parameterRecommendedValue" placeholder="整数" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-divider content-position="left">3. 分类与归属</el-divider>
              <el-form-item label="适用网元">
                <el-select
                  v-model="applicableNeSelection"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="多选，顿号分隔落库"
                  style="width: 100%"
                  @change="onApplicableNeChange"
                >
                  <el-option
                    v-for="ne in applicableNeOptions"
                    :key="String(ne.neTypeId)"
                    :label="String(ne.neTypeNameCn || '未命名网元')"
                    :value="String(ne.neTypeNameCn || ne.neTypeId)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="所属特性">
                <el-select
                  v-model="selectedFeatureId"
                  filterable
                  clearable
                  placeholder="当前版本特性"
                  style="width: 100%"
                  @change="onFeatureChange"
                >
                  <el-option
                    v-for="f in featureOptions"
                    :key="String(f.featureId)"
                    :label="String(f.featureNameCn || f.featureNameEn || '未命名特性')"
                    :value="String(f.featureId)"
                  />
                </el-select>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="业务分类">
                    <el-select
                      v-model="selectedCategoryCode"
                      filterable
                      clearable
                      placeholder="选择分类"
                      style="width: 100%"
                      @change="onCategoryChange"
                    >
                      <el-option
                        v-for="c in categoryOptions"
                        :key="String(c.categoryId)"
                        :label="String(c.categoryNameCn || '未命名分类')"
                        :value="String(c.categoryId)"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="立即生效">
                    <el-select v-model="formMain.takeEffectImmediately" style="width: 140px">
                      <el-option label="是" value="是" />
                      <el-option label="否" value="否" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="生效方式（中）">
                    <el-select
                      v-model="selectedEffectiveModeId"
                      filterable
                      clearable
                      placeholder="主数据"
                      style="width: 100%"
                      @change="onEffectiveModeChange"
                    >
                      <el-option
                        v-for="m in effectiveModeOptions"
                        :key="String(m.effectiveModeId)"
                        :label="String(m.effectiveModeNameCn || '未命名方式')"
                        :value="String(m.effectiveModeId)"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="生效方式（英）">
                    <el-input v-model="formMain.effectiveModeEn" readonly placeholder="随中文带出" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="项目组">
                    <el-select
                      v-model="selectedTeamId"
                      filterable
                      clearable
                      placeholder="主数据"
                      style="width: 100%"
                      @change="onTeamChange"
                    >
                      <el-option
                        v-for="t in teamOptions"
                        :key="String(t.teamId)"
                        :label="String(t.teamName || '未命名项目组')"
                        :value="String(t.teamId)"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="归属模块">
                    <el-input v-model="formMain.belongingModule" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="变更来源">
                <el-input v-model="formMain.changeSource" type="textarea" :rows="2" placeholder="不对内容 trim 后再保存（由后端约定）" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="版本号">
                    <el-input v-model="formMain.patchVersion" placeholder="默认当前版本名" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="引入版本">
                    <el-input v-model="formMain.introducedVersion" placeholder="与版本域对齐" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item
                v-if="dialogMode === 'edit' && (formMain.introduceType || formMain.inheritReferenceVersionId)"
                label="引入/继承"
              >
                <el-input :model-value="inheritIntroSummary" readonly />
              </el-form-item>

              <el-divider content-position="left">4. 长文本</el-divider>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item>
                    <template #label>
                      <span>参数含义（中）</span>
                      <el-tooltip content="示例见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.parameterDescriptionCn" type="textarea" :rows="3" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="参数含义（英）">
                    <el-input v-model="formMain.parameterDescriptionEn" type="textarea" :rows="3" placeholder="默认/翻译" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item>
                    <template #label>
                      <span>影响说明（中）</span>
                      <el-tooltip content="示例见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.impactDescriptionCn" type="textarea" :rows="3" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="影响说明（英）">
                    <el-input v-model="formMain.impactDescriptionEn" type="textarea" :rows="3" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item>
                    <template #label>
                      <span>配置举例（中）</span>
                      <el-tooltip content="示例见 function.md §2.3.4" placement="top">
                        <span class="label-tip">ⓘ</span>
                      </el-tooltip>
                    </template>
                    <el-input v-model="formMain.configurationExampleCn" type="textarea" :rows="3" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="配置举例（英）">
                    <el-input v-model="formMain.configurationExampleEn" type="textarea" :rows="3" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="关联参数描述（中）">
                    <el-input v-model="formMain.relatedParameterDescriptionCn" type="textarea" :rows="2" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="关联参数描述（英）">
                    <el-input v-model="formMain.relatedParameterDescriptionEn" type="textarea" :rows="2" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="备注">
                <el-input v-model="formMain.remark" type="textarea" :rows="2" />
              </el-form-item>

              <el-collapse>
                <el-collapse-item title="扩展字段（enumeration / unit / range，§3.1 预留）" name="ext">
                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="枚举值（中）">
                        <el-input v-model="formMain.enumerationValuesCn" type="textarea" :rows="2" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="枚举值（英）">
                        <el-input v-model="formMain.enumerationValuesEn" type="textarea" :rows="2" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="参数单位（中）">
                        <el-input v-model="formMain.parameterUnitCn" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="参数单位（英）">
                        <el-input v-model="formMain.parameterUnitEn" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="parameter_range">
                    <el-input v-model="formMain.parameterRange" />
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>

              <el-divider content-position="left">5. 变更说明（至少 1 条，中英四格必填）</el-divider>
            <el-table :data="formChanges" border size="small" style="width: 100%">
              <el-table-column label="变更类型" width="160">
                <template #default="{ row }">
                  <el-select v-model="row.changeType" size="small" filterable style="width: 148px">
                    <el-option
                      v-for="opt in changeTypeSelectOptions"
                      :key="opt.changeTypeNameCn"
                      :label="opt.changeTypeNameCn || ''"
                      :value="opt.changeTypeNameCn || ''"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="原因（中）" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.changeReasonCn" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="原因（英）" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.changeReasonEn" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="影响（中）" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.changeImpactCn" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="影响（英）" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.changeImpactEn" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="导出 delta" width="100">
                <template #default="{ row }">
                  <el-select v-model="row.exportDelta" size="small" style="width: 88px">
                    <el-option label="是" value="是" />
                    <el-option label="否" value="否" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="不导出原因" min-width="100">
                <template #default="{ row }">
                  <el-input v-model="row.noExportReason" size="small" />
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top: 8px">
              <el-button size="small" @click="addChangeRow">增行</el-button>
            </div>
          </el-form>
          </div>

          <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :disabled="!canQuery" @click="submitDialog">保存</el-button>
          </template>
        </el-dialog>

        <el-dialog v-model="importVisible" title="导入" width="560px" destroy-on-close>
          <div style="margin-bottom: 10px; color: #666">
            支持 .xlsx。请先在左侧选择“命令”（可选：再选择类型）。
          </div>
          <el-radio-group v-model="importMode" style="margin-bottom: 10px">
            <el-radio label="FULL">全量导入（删除当前版本范围内参数后重新导入）</el-radio>
            <el-radio label="INCREMENTAL">增量导入（新增/修改；已基线参数不会被更改）</el-radio>
          </el-radio-group>
          <div v-if="importMode === 'INCREMENTAL'" style="margin-bottom: 10px; color: #666">
            提示：增量导入时，命中“已基线”的参数会被跳过（不导入、不修改）。
          </div>
          <el-upload
            :disabled="!canImport"
            :show-file-list="true"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            :before-upload="onBeforeImportUpload"
          >
            <el-button :disabled="!canImport" type="primary">选择文件</el-button>
          </el-upload>
          <div style="margin-top: 12px">
            <a class="template-link" href="javascript:void(0)" @click="openTemplateUrl">下载模板</a>
          </div>
          <template #footer>
            <el-button @click="importVisible = false">关闭</el-button>
          </template>
        </el-dialog>
  </el-main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { request } from '../../api/http'
import {
  fetchApplicableNes,
  fetchBusinessCategories,
  fetchEffectiveModes,
  fetchProjectTeams,
  fetchVersionFeatures,
} from '../../api/config-masterdata'
import { fetchCommands } from '../../api/command-domain'
import { fetchTypeBits } from '../../api/type-bits'
import {
  VERSION_ALL,
  baselineParameter,
  unbaselineParameter,
  createParameter,
  deleteParameter,
  extractViolatedKeywordRegex,
  fetchAvailableBits,
  fetchAvailableSequences,
  fetchBaselineCount,
  fetchBaselineCountByProduct,
  fetchConfigChangeTypes,
  fetchParameterCommandTree,
  fetchParameterPage,
  fetchParameterPageByProduct,
  importParameters,
  updateParameter,
  type ConfigChangeTypeItem,
  type ParameterCommandTreeNode,
  type ParameterImportMode,
  type ParameterSaveRequest,
} from '../../api/parameter'
import type { PageResponse } from '../../types/api-response'
import { useProductContextStore } from '../../stores/productContext'
import { useVersionContextStore } from '../../stores/versionContext'
import OperationLogDrawer from '../../components/OperationLogDrawer.vue'

const TYPE_NEW_PARAMETER_CN = '新增参数'
const BASELINE = '已基线'
const apiV1Prefix = '/api/v1'
const typeOptions = ['BIT', 'BYTE', 'DWORD', 'STRING', 'INT']

const typeBits = ref<Record<string, number>>({})
const typeBitsLoading = ref(false)

interface TreeNodeData {
  id: string
  label: string
  commandId?: string
  commandTypeCode?: string
  children?: TreeNodeData[]
}

const productContext = useProductContextStore()
const versionContext = useVersionContextStore()

/** 与顶栏产品选择器同源（Pinia），避免本地 ref 与 store 不一致导致「已选产品仍置灰」 */
const selectedProductId = computed(() => (productContext.ownedProductId || '').trim())
const versionId = ref(versionContext.versionId || '')
const versionOptions = ref<{ versionId: string; versionName: string }[]>([])
const commandSelectOptions = ref<{ commandId: string; commandName: string }[]>([])
const commandSelectLoading = ref(false)

const filterCommandId = ref('')
const filterCommandType = ref('')

const treeRef = ref<{ setCurrentKey: (k: string | null) => void } | null>(null)
const treeLoading = ref(false)
const treeData = ref<TreeNodeData[]>([])
const commandTreeRaw = ref<ParameterCommandTreeNode[]>([])

const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<Record<string, unknown>[]>([])
const tableLoading = ref(false)

const baselineCount = ref(0)
const importVisible = ref(false)
const opLogOpen = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)

const formMain = reactive<Record<string, unknown>>({})
const formTypeCode = ref('BIT')
const formSequence = ref(1)
const formChanges = ref<
  {
    changeType: string
    changeReasonCn: string
    changeImpactCn: string
    changeReasonEn: string
    changeImpactEn: string
    exportDelta: string
    noExportReason: string
  }[]
>([])

const seqOptions = ref<{ sequence: number; availability: string }[]>([])
const selectedBits = ref<number[]>([])
const rawAvailableBits = ref<Set<number>>(new Set())

const changeTypes = ref<ConfigChangeTypeItem[]>([])

const masterdataLoading = ref(false)
const applicableNeOptions = ref<Record<string, unknown>[]>([])
const featureOptions = ref<Record<string, unknown>[]>([])
const categoryOptions = ref<Record<string, unknown>[]>([])
const effectiveModeOptions = ref<Record<string, unknown>[]>([])
const teamOptions = ref<Record<string, unknown>[]>([])
const applicableNeSelection = ref<string[]>([])
const selectedFeatureId = ref('')
const selectedCategoryCode = ref('')
const selectedEffectiveModeId = ref('')
const selectedTeamId = ref('')

// 产品选择器已提升到全局 Header；本页仅消费上下文

const canQuery = computed(() => !!selectedProductId.value && !!versionId.value)

const tableEmptyText = computed(() => {
  if (!selectedProductId.value) {
    return '请先选择产品'
  }
  if (!versionId.value) {
    return '请选择版本或 ALL 以加载列表'
  }
  return '暂无数据'
})

const isAllVersionsView = computed(() => versionId.value === VERSION_ALL)

/** 新增时：若左树选中了“类型节点”，则类型只能为该类型 */
const createTypeLocked = computed(() => dialogMode.value === 'create' && !!filterCommandType.value)
const typeOptionsForDialog = computed(() => {
  if (createTypeLocked.value) return [filterCommandType.value]
  return typeOptions
})

/** 单版本：按版本查参数审计；ALL：合并各版本下的参数变更 */
const paramOpLogVersionId = computed(() => (isAllVersionsView.value ? undefined : versionId.value || undefined))
const paramOpLogIgnoreAllVersions = computed(() => isAllVersionsView.value)

/** 操作日志「操作对象」：参数编码 + 名称（中）；删除后仍用缓存避免只显示 ID */
const opLogResourceCache = ref<Record<string, string>>({})
const opLogResourceMap = computed<Record<string, string>>(() => opLogResourceCache.value)

function cacheParameterOpObject(row: Record<string, unknown>) {
  const id = String((row as any).parameterId ?? '').trim()
  if (!id) return
  const code = String((row as any).parameterCode || '').trim()
  const name = String((row as any).parameterNameCn || '').trim()
  const parts = [code, name].filter(Boolean)
  const label = parts.length ? parts.join(' · ') : ''
  if (!label) return
  if (opLogResourceCache.value[id] === label) return
  opLogResourceCache.value = { ...opLogResourceCache.value, [id]: label }
}

const displayVersionOptions = computed(() => [
  { versionId: VERSION_ALL, versionName: 'ALL（全产品）' },
  ...versionOptions.value,
])

const canImport = computed(() => canQuery.value && !isAllVersionsView.value)

const importMode = ref<ParameterImportMode>('INCREMENTAL')

function apiVersionIdForForm(): string {
  if (dialogMode.value === 'create') return isAllVersionsView.value ? '' : versionId.value || ''
  return String(formMain.ownedVersionId || versionId.value || '')
}

function resolveVersionIdForRow(row: Record<string, unknown>): string {
  if (isAllVersionsView.value) return String(row.ownedVersionId || '')
  return versionId.value || ''
}

const canLoadAllocation = computed(
  () =>
    !!selectedProductId.value &&
    !!apiVersionIdForForm() &&
    !!formMain.ownedCommandId &&
    !!formTypeCode.value,
)

const canLoadBits = computed(
  () => canLoadAllocation.value && formSequence.value > 0 && maxBitsForCurrentType.value > 0,
)

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增参数' : '编辑参数'))

const changeTypeSelectOptions = computed(() => {
  if (dialogMode.value === 'create') {
    return changeTypes.value.filter((t) => (t.changeTypeNameCn || '').trim() === TYPE_NEW_PARAMETER_CN)
  }
  return changeTypes.value
})

const templateUrl = computed(() => {
  if (!selectedProductId.value || !versionId.value || isAllVersionsView.value) return ''
  return `${apiV1Prefix}/products/${encodeURIComponent(selectedProductId.value)}/versions/${encodeURIComponent(versionId.value)}/parameters/import-templates`
})

const exportUrl = computed(() => {
  if (!selectedProductId.value || !versionId.value || isAllVersionsView.value) return ''
  const q = new URLSearchParams()
  if (filterCommandId.value) q.set('commandId', filterCommandId.value)
  if (filterCommandType.value) q.set('commandTypeId', filterCommandType.value)
  const qs = q.toString()
  return `${apiV1Prefix}/products/${encodeURIComponent(selectedProductId.value)}/versions/${encodeURIComponent(versionId.value)}/parameters/export${qs ? `?${qs}` : ''}`
})

const bitCheckboxRange = computed(() => {
  const m = maxBitsForCurrentType.value
  return Array.from({ length: m }, (_, i) => i + 1)
})

const maxBitsForCurrentType = computed(() => {
  const u = String(formTypeCode.value || '').trim().toUpperCase()
  const hit = typeBits.value[u]
  if (hit !== undefined && hit !== null && !Number.isNaN(Number(hit))) return Number(hit)
  // 兜底：兼容历史逻辑
  if (u === 'BIT') return 1
  if (u === 'BYTE') return 8
  if (u === 'DWORD') return 32
  if (u === 'STRING') return 32
  if (u === 'INT') return 0
  return 32
})

const currentVersionLabel = computed(() => {
  if (versionId.value === VERSION_ALL) return 'ALL（全产品）'
  const v = versionOptions.value.find((x) => x.versionId === versionId.value)
  return v?.versionName || versionId.value || ''
})

function versionNameForTable(id: string) {
  if (!id) return '—'
  const v = versionOptions.value.find((x) => x.versionId === id)
  return v?.versionName || '—'
}

function commandNameForTable(id: string) {
  const cid = (id || '').trim()
  if (!cid) return '—'
  const c = commandSelectOptions.value.find((x) => x.commandId === cid)
  return c?.commandName || cid
}

const inheritIntroSummary = computed(() => {
  const t = String(formMain.introduceType || '').trim()
  const refId = String(formMain.inheritReferenceVersionId || '').trim()
  if (!t && !refId) return ''
  const vn = refId ? versionNameForTable(refId) : ''
  const parts: string[] = []
  if (t) parts.push(t)
  if (refId) parts.push(vn !== '—' ? vn : '参考版本')
  return parts.join(' · ')
})

function parseDunhao(s: string): string[] {
  return s.split(/[、,，]/).map((x) => x.trim()).filter(Boolean)
}

async function loadMasterdataForForm(contextVersionId: string) {
  if (!selectedProductId.value || !contextVersionId) return
  masterdataLoading.value = true
  try {
    const [nes, cats, modes, teams, feats] = await Promise.all([
      fetchApplicableNes(selectedProductId.value),
      fetchBusinessCategories(selectedProductId.value),
      fetchEffectiveModes(selectedProductId.value),
      fetchProjectTeams(selectedProductId.value),
      fetchVersionFeatures(selectedProductId.value, contextVersionId),
    ])
    applicableNeOptions.value = nes
    categoryOptions.value = cats
    effectiveModeOptions.value = modes
    teamOptions.value = teams
    featureOptions.value = feats
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载主数据失败')
  } finally {
    masterdataLoading.value = false
  }
}

function syncSelectionsFromForm() {
  applicableNeSelection.value = parseDunhao(String(formMain.applicableNe || ''))
  selectedFeatureId.value = String(formMain.featureId || '')
  selectedCategoryCode.value = String(formMain.categoryId || '')
  const cn = String(formMain.effectiveModeCn || '')
  const em = effectiveModeOptions.value.find((m) => String(m.effectiveModeNameCn) === cn)
  selectedEffectiveModeId.value = em ? String(em.effectiveModeId || '') : ''
  const pt = String(formMain.projectTeam || '')
  const tm = teamOptions.value.find((t) => String(t.teamName) === pt)
  selectedTeamId.value = tm ? String(tm.teamId || '') : ''
}

function onApplicableNeChange() {
  formMain.applicableNe = applicableNeSelection.value.join('、')
}

function onFeatureChange(fid: string | undefined) {
  const id = fid || ''
  selectedFeatureId.value = id
  const f = featureOptions.value.find((x) => String(x.featureId) === id)
  formMain.featureId = id
  formMain.feature = f ? String(f.featureNameCn || '') : ''
}

function onCategoryChange(code: string | undefined) {
  const c = code || ''
  selectedCategoryCode.value = c
  if (!c) {
    formMain.categoryId = ''
    formMain.businessClassification = ''
    return
  }
  const row = categoryOptions.value.find((x) => String(x.categoryId) === c)
  formMain.categoryId = c
  formMain.businessClassification = row ? String(row.categoryNameCn || '') : ''
}

function onEffectiveModeChange(id: string | undefined) {
  const mid = id || ''
  selectedEffectiveModeId.value = mid
  if (!mid) {
    formMain.effectiveModeCn = ''
    formMain.effectiveModeEn = ''
    return
  }
  const row = effectiveModeOptions.value.find((x) => String(x.effectiveModeId) === mid)
  formMain.effectiveModeCn = row ? String(row.effectiveModeNameCn || '') : ''
  formMain.effectiveModeEn = row ? String(row.effectiveModeNameEn || '') : ''
}

function onTeamChange(tid: string | undefined) {
  const id = tid || ''
  selectedTeamId.value = id
  if (!id) {
    formMain.projectTeam = ''
    return
  }
  const row = teamOptions.value.find((x) => String(x.teamId) === id)
  formMain.projectTeam = row ? String(row.teamName || '') : ''
}

function copyMainFromRow(row: Record<string, unknown>) {
  const keys = [
    'ownedVersionId',
    'parameterNameCn',
    'parameterNameEn',
    'valueRange',
    'bitUsage',
    'valueDescriptionCn',
    'valueDescriptionEn',
    'applicationScenarioCn',
    'applicationScenarioEn',
    'parameterDefaultValue',
    'parameterRecommendedValue',
    'applicableNe',
    'feature',
    'featureId',
    'businessClassification',
    'categoryId',
    'takeEffectImmediately',
    'effectiveModeCn',
    'effectiveModeEn',
    'projectTeam',
    'belongingModule',
    'changeSource',
    'patchVersion',
    'introducedVersion',
    'parameterDescriptionCn',
    'parameterDescriptionEn',
    'impactDescriptionCn',
    'impactDescriptionEn',
    'configurationExampleCn',
    'configurationExampleEn',
    'relatedParameterDescriptionCn',
    'relatedParameterDescriptionEn',
    'remark',
    'enumerationValuesCn',
    'enumerationValuesEn',
    'parameterUnitCn',
    'parameterUnitEn',
    'parameterRange',
    'introduceType',
    'inheritReferenceVersionId',
  ]
  for (const k of keys) {
    const v = row[k]
    formMain[k] = v !== undefined && v !== null ? v : ''
  }
}

function buildTreeData(nodes: ParameterCommandTreeNode[]): TreeNodeData[] {
  const children: TreeNodeData[] = nodes.map((cmd) => ({
    id: `c:${cmd.commandId}`,
    label: cmd.commandName || cmd.commandId,
    commandId: cmd.commandId,
    children: (cmd.types || []).map((t) => ({
      id: `c:${cmd.commandId}|${t.code}`,
      label: t.name || t.code,
      commandId: cmd.commandId,
      commandTypeCode: t.code,
    })),
  }))
  return [
    {
      id: '__all__',
      label: '全部参数',
      children,
    },
  ]
}

function onTreeNodeClick(data: TreeNodeData) {
  if (data.id === '__all__') {
    filterCommandId.value = ''
    filterCommandType.value = ''
  } else if (data.commandTypeCode && data.commandId) {
    filterCommandId.value = data.commandId
    filterCommandType.value = data.commandTypeCode
  } else if (data.commandId) {
    filterCommandId.value = data.commandId
    filterCommandType.value = ''
  }
  page.value = 1
  loadRows()
}

function isBaseline(row: Record<string, unknown>) {
  return String(row.dataStatus || '') === BASELINE
}

async function loadTypeBits() {
  typeBitsLoading.value = true
  try {
    const rows = await fetchTypeBits()
    const map: Record<string, number> = {}
    for (const r of rows) {
      const k = String((r as any).typeEnum || '').trim().toUpperCase()
      const v = Number((r as any).bitCount)
      if (!k) continue
      map[k] = Number.isNaN(v) ? 0 : v
    }
    typeBits.value = map
  } finally {
    typeBitsLoading.value = false
  }
}

function parseBits(s: string): number[] {
  if (!s || !s.trim()) return []
  return s
    .split(',')
    .map((x) => Number(x.trim()))
    .filter((n) => !Number.isNaN(n) && n > 0)
}

function syncCode() {
  formMain.parameterCode = `${formTypeCode.value}_${formSequence.value}`
  formMain.parameterSequence = formSequence.value
}

function onTypeOrSeqChange() {
  syncCode()
}

function expandRawToFullRange() {
  const m = maxBitsForCurrentType.value
  rawAvailableBits.value = new Set(Array.from({ length: m }, (_, i) => i + 1))
}

function onTypeChange() {
  syncCode()
  selectedBits.value = []
  formMain.bitUsage = ''
  expandRawToFullRange()
}

function onSequencePick() {
  syncCode()
}

/** 接口返回的「仍可选」集合（合并当前行已选，供判定可选范围） */
function mergedAvailableSet(): Set<number> {
  const m = new Set(rawAvailableBits.value)
  selectedBits.value.forEach((b) => m.add(b))
  return m
}

function isBitOccupiedGray(n: number): boolean {
  return selectedBits.value.includes(n) && !rawAvailableBits.value.has(n)
}

function isBitCheckboxDisabled(n: number) {
  if (selectedBits.value.includes(n) && !rawAvailableBits.value.has(n)) return true
  const merged = mergedAvailableSet()
  if (!merged.has(n)) return true
  return false
}

function toggleBit(n: number, checked: boolean) {
  if (isBitCheckboxDisabled(n)) return
  if (checked) {
    if (!selectedBits.value.includes(n)) selectedBits.value.push(n)
  } else {
    selectedBits.value = selectedBits.value.filter((x) => x !== n)
  }
  syncBitUsageFromSelect()
}

async function loadSeqOptions() {
  const vid = apiVersionIdForForm()
  if (!canLoadAllocation.value || !selectedProductId.value || !vid) return
  try {
    const data = await fetchAvailableSequences(
      selectedProductId.value,
      vid,
      String(formMain.ownedCommandId),
      formTypeCode.value,
    )
    seqOptions.value = data.sequences || []
    if (seqOptions.value.length) {
      ElMessage.success(`已加载 ${seqOptions.value.length} 个可用序号`)
    } else {
      ElMessage.info('当前无可用序号（可能已被占满或无区段配置）')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载序号失败')
  }
}

async function loadBitPool() {
  const vid = apiVersionIdForForm()
  if (!canLoadBits.value || !selectedProductId.value || !vid) return
  try {
    const data = await fetchAvailableBits(
      selectedProductId.value,
      vid,
      String(formMain.ownedCommandId),
      formTypeCode.value,
      formSequence.value,
    )
    const avail = data.availableBitIndexes || []
    rawAvailableBits.value = new Set(avail)
    const merged = mergedAvailableSet()
    selectedBits.value = parseBits(String(formMain.bitUsage || '')).filter((b) => merged.has(b))
    syncBitUsageFromSelect()
    ElMessage.success('已加载可选 BIT')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载 BIT 失败')
  }
}

function syncBitUsageFromSelect() {
  formMain.bitUsage = selectedBits.value
    .slice()
    .sort((a, b) => a - b)
    .join(',')
}

function defaultChangeRow(): {
  changeType: string
  changeReasonCn: string
  changeImpactCn: string
  changeReasonEn: string
  changeImpactEn: string
  exportDelta: string
  noExportReason: string
} {
  return {
    changeType: TYPE_NEW_PARAMETER_CN,
    changeReasonCn: '',
    changeImpactCn: '',
    changeReasonEn: '',
    changeImpactEn: '',
    exportDelta: '是',
    noExportReason: '',
  }
}

function resetForm() {
  Object.keys(formMain).forEach((k) => delete formMain[k])
  formMain.ownedCommandId = filterCommandId.value || (commandTreeRaw.value[0]?.commandId ?? 'command_demo')
  formTypeCode.value = (filterCommandType.value || 'BIT') as string
  formSequence.value = 1
  syncCode()
  formMain.parameterNameCn = ''
  formMain.parameterNameEn = ''
  formMain.valueRange = '0'
  formMain.bitUsage = ''
  formMain.changeSource = ''
  formMain.valueDescriptionCn = ''
  formMain.valueDescriptionEn = ''
  formMain.applicationScenarioCn = ''
  formMain.applicationScenarioEn = ''
  formMain.parameterDefaultValue = ''
  formMain.parameterRecommendedValue = ''
  formMain.applicableNe = ''
  formMain.feature = ''
  formMain.featureId = ''
  formMain.businessClassification = ''
  formMain.categoryId = ''
  formMain.takeEffectImmediately = '是'
  formMain.effectiveModeCn = ''
  formMain.effectiveModeEn = ''
  formMain.projectTeam = ''
  formMain.belongingModule = ''
  formMain.patchVersion = currentVersionLabel.value
  formMain.introducedVersion = versionId.value === VERSION_ALL ? '' : versionId.value || ''
  formMain.parameterDescriptionCn = ''
  formMain.parameterDescriptionEn = ''
  formMain.impactDescriptionCn = ''
  formMain.impactDescriptionEn = ''
  formMain.configurationExampleCn = ''
  formMain.configurationExampleEn = ''
  formMain.relatedParameterDescriptionCn = ''
  formMain.relatedParameterDescriptionEn = ''
  formMain.remark = ''
  formMain.enumerationValuesCn = ''
  formMain.enumerationValuesEn = ''
  formMain.parameterUnitCn = ''
  formMain.parameterUnitEn = ''
  formMain.parameterRange = ''
  applicableNeSelection.value = []
  selectedFeatureId.value = ''
  selectedCategoryCode.value = ''
  selectedEffectiveModeId.value = ''
  selectedTeamId.value = ''
  formChanges.value = [defaultChangeRow()]
  seqOptions.value = []
  selectedBits.value = []
  expandRawToFullRange()
}

function fillFromRow(row: Record<string, unknown>) {
  resetForm()
  formMain.ownedCommandId = String(row.ownedCommandId || '')
  const code = String(row.parameterCode || '')
  const parts = code.lastIndexOf('_')
  if (parts > 0) {
    formTypeCode.value = code.substring(0, parts)
    const n = Number(code.substring(parts + 1))
    if (!Number.isNaN(n)) formSequence.value = n
  }
  copyMainFromRow(row)
  syncCode()
  selectedBits.value = parseBits(String(formMain.bitUsage || ''))
  expandRawToFullRange()
}

async function openCreate() {
  if (!canQuery.value) {
    ElMessage.warning('请先选择产品与版本')
    return
  }
  if (isAllVersionsView.value) {
    ElMessage.warning('全产品（ALL）视图下请切换到具体版本后再新增')
    return
  }
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  ensureCreateChangeType()
  await loadMasterdataForForm(versionId.value!)
  syncSelectionsFromForm()
  dialogVisible.value = true
}

function ensureCreateChangeType() {
  const row = formChanges.value[0]
  if (!row) return
  const allowed = changeTypeSelectOptions.value.map((x) => x.changeTypeNameCn || '').filter(Boolean)
  if (!allowed.includes(row.changeType)) {
    row.changeType = TYPE_NEW_PARAMETER_CN
  }
}

async function openEdit(row: Record<string, unknown>) {
  const ctxVid = String(row.ownedVersionId || versionId.value || '')
  if (!ctxVid) {
    ElMessage.error('该行缺少所属版本，无法编辑')
    return
  }
  dialogMode.value = 'edit'
  editingId.value = Number(row.parameterId)
  fillFromRow(row)
  const cmdId = String(formMain.ownedCommandId || '')
  if (cmdId && !commandSelectOptions.value.some((c) => c.commandId === cmdId)) {
    commandSelectOptions.value = [...commandSelectOptions.value, { commandId: cmdId, commandName: '（不在当前列表）' }]
  }
  await loadMasterdataForForm(ctxVid)
  syncSelectionsFromForm()
  dialogVisible.value = true
}

function onDialogClosed() {
  editingId.value = null
}

function addChangeRow() {
  const base = defaultChangeRow()
  if (dialogMode.value === 'create') {
    base.changeType = TYPE_NEW_PARAMETER_CN
  }
  formChanges.value.push(base)
}

function buildSaveRequest(): ParameterSaveRequest {
  syncCode()
  const main: Record<string, unknown> = { ...formMain }
  main.parameterCode = formMain.parameterCode
  main.parameterSequence = formSequence.value
  main.bitUsage = formMain.bitUsage || ''
  main.ownedCommandId = formMain.ownedCommandId
  main.parameterNameCn = formMain.parameterNameCn
  main.valueRange = formMain.valueRange || '0'
  main.changeSource = formMain.changeSource === undefined || formMain.changeSource === null ? '' : formMain.changeSource

  const changeDescriptions = formChanges.value.map((c) => ({
    changeDescriptionId: '',
    changeType: c.changeType,
    changeReasonCn: c.changeReasonCn,
    changeImpactCn: c.changeImpactCn,
    changeReasonEn: c.changeReasonEn,
    changeImpactEn: c.changeImpactEn,
    exportDelta: c.exportDelta,
    noExportReason: c.exportDelta === '否' ? c.noExportReason : '',
  }))

  return { main, changeDescriptions }
}

function validateOptionalInt(label: string, v: unknown): string | undefined {
  const s = String(v ?? '').trim()
  if (!s) return undefined
  if (!/^-?\d+$/.test(s)) return `${label}须为整数`
  return undefined
}

function validateForm(): string | undefined {
  if (!String(formMain.ownedCommandId || '').trim()) return '请选择归属命令'
  if (!String(formMain.parameterNameCn || '').trim()) return '请填写参数名称（中文）'
  if (maxBitsForCurrentType.value > 0 && !String(formMain.bitUsage || '').trim()) return '请选择或填写 BIT 占用'
  const d = validateOptionalInt('参数默认值', formMain.parameterDefaultValue)
  if (d) return d
  const r = validateOptionalInt('参数推荐值', formMain.parameterRecommendedValue)
  if (r) return r
  if (!formChanges.value.length) return '至少一条变更说明'
  for (const c of formChanges.value) {
    if (!String(c.changeType || '').trim()) return '请选择变更类型'
    if (!String(c.changeReasonCn || '').trim()) return '请填写变更原因（中）'
    if (!String(c.changeReasonEn || '').trim()) return '请填写变更原因（英）'
    if (!String(c.changeImpactCn || '').trim()) return '请填写变更影响（中）'
    if (!String(c.changeImpactEn || '').trim()) return '请填写变更影响（英）'
    if (c.exportDelta === '否' && !String(c.noExportReason || '').trim()) {
      return 'export_delta 为「否」时请填写不导出原因'
    }
  }
  return undefined
}

async function submitDialog() {
  const msg = validateForm()
  if (msg) {
    ElMessage.warning(msg)
    return
  }
  if (!selectedProductId.value) return
  const body = buildSaveRequest()
  try {
    if (dialogMode.value === 'create') {
      if (!versionId.value || isAllVersionsView.value) {
        ElMessage.warning('请选择具体版本后再保存')
        return
      }
      await createParameter(selectedProductId.value, versionId.value, body)
    } else if (editingId.value != null) {
      const saveVid = String(formMain.ownedVersionId || versionId.value || '')
      if (!saveVid) {
        ElMessage.warning('无法解析参数所属版本')
        return
      }
      await updateParameter(selectedProductId.value, saveVid, editingId.value, body)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadRows()
    await loadBaselineCount()
  } catch (e) {
    const rx = extractViolatedKeywordRegex(e)
    const m = e instanceof Error ? e.message : '保存失败'
    if (rx) {
      ElMessage.error(`${m}（命中正则：${rx}）`)
    } else {
      ElMessage.error(m)
    }
  }
}

async function removeRow(row: Record<string, unknown>) {
  if (!canQuery.value) return
  const vid = resolveVersionIdForRow(row)
  if (!vid) {
    ElMessage.error('缺少所属版本，无法删除')
    return
  }
  cacheParameterOpObject(row)
  await ElMessageBox.confirm('确认删除该参数？', '确认', { type: 'warning' })
  const id = Number(row.parameterId)
  try {
    await deleteParameter(selectedProductId.value!, vid, id)
    ElMessage.success('已删除')
    await loadRows()
    await loadBaselineCount()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function doBaseline(row: Record<string, unknown>) {
  if (!canQuery.value) return
  const vid = resolveVersionIdForRow(row)
  if (!vid) {
    ElMessage.error('缺少所属版本，无法基线')
    return
  }
  await ElMessageBox.confirm('确认将该参数置为「已基线」？基线后不可再编辑/删除。', '基线', { type: 'warning' })
  const id = Number(row.parameterId)
  try {
    await baselineParameter(selectedProductId.value!, vid, id)
    ElMessage.success('已基线')
    await loadRows()
    await loadBaselineCount()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '基线失败')
  }
}

async function doUnbaseline(row: Record<string, unknown>) {
  if (!canQuery.value) return
  const vid = resolveVersionIdForRow(row)
  if (!vid) {
    ElMessage.error('缺少所属版本，无法解锁')
    return
  }
  await ElMessageBox.confirm('确认解锁该参数基线？解锁后可再次编辑/删除。', '解锁', { type: 'warning' })
  const id = Number(row.parameterId)
  try {
    await unbaselineParameter(selectedProductId.value!, vid, id)
    ElMessage.success('已解锁')
    await loadRows()
    await loadBaselineCount()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '解锁失败')
  }
}

async function loadRows() {
  if (!canQuery.value) {
    rows.value = []
    total.value = 0
    return
  }
  tableLoading.value = true
  try {
    if (versionId.value === VERSION_ALL) {
      const data = await fetchParameterPageByProduct(selectedProductId.value!, {
        page: page.value,
        size: size.value,
        commandId: filterCommandId.value || undefined,
        commandTypeId: filterCommandType.value || undefined,
      })
      rows.value = data.records
      total.value = data.total
      for (const r of rows.value) cacheParameterOpObject(r)
    } else {
      const data = await fetchParameterPage(selectedProductId.value!, versionId.value!, {
        page: page.value,
        size: size.value,
        commandId: filterCommandId.value || undefined,
        commandTypeId: filterCommandType.value || undefined,
      })
      rows.value = data.records
      total.value = data.total
      for (const r of rows.value) cacheParameterOpObject(r)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    tableLoading.value = false
  }
}

async function loadBaselineCount() {
  if (!selectedProductId.value || !versionId.value) {
    baselineCount.value = 0
    return
  }
  try {
    if (versionId.value === VERSION_ALL) {
      const data = await fetchBaselineCountByProduct(selectedProductId.value)
      baselineCount.value = Number(data.baselineCount ?? 0)
    } else {
      const data = await fetchBaselineCount(selectedProductId.value, versionId.value)
      baselineCount.value = Number(data.baselineCount ?? 0)
    }
  } catch {
    baselineCount.value = 0
  }
}

async function loadCommandTree() {
  if (!selectedProductId.value) {
    treeData.value = []
    commandTreeRaw.value = []
    return
  }
  treeLoading.value = true
  try {
    const data = await fetchParameterCommandTree(selectedProductId.value)
    commandTreeRaw.value = data
    treeData.value = buildTreeData(data)
    treeRef.value?.setCurrentKey('__all__')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载命令树失败')
  } finally {
    treeLoading.value = false
  }
}

async function loadChangeTypes() {
  try {
    changeTypes.value = await fetchConfigChangeTypes()
  } catch {
    changeTypes.value = []
  }
}

function openExport() {
  if (!exportUrl.value) return
  window.open(exportUrl.value, '_blank')
}

function openTemplateUrl() {
  if (!templateUrl.value) return
  window.open(templateUrl.value, '_blank')
}

function onBeforeImportUpload(file: File) {
  void onImport(file)
  importVisible.value = false
  return false
}

async function onImport(file: File) {
  if (!canImport.value) return
  if (!filterCommandId.value) {
    ElMessage.warning('请先在左侧选择命令后再导入')
    return
  }
  try {
    const data = await importParameters(selectedProductId.value!, versionId.value!, file, {
      mode: importMode.value,
      commandId: filterCommandId.value,
      commandTypeCode: filterCommandType.value || undefined,
    })
    ElMessage.success(`导入结束：成功 ${data.successCount}，失败 ${data.failureCount}`)
    await loadRows()
    await loadBaselineCount()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  }
}

watch(
  () => productContext.ownedProductId,
  async (owned) => {
    const pid = (owned || '').trim()
    /** 宿主 Query / Pinia 注入的版本；不在此处立即清空 versionContext，避免 App 侧 URL 同步抢先去掉 versionId（spec-04） */
    const restoreVid = (versionContext.versionId || '').trim()
    versionId.value = ''
    versionOptions.value = []
    commandSelectOptions.value = []
    rows.value = []
    total.value = 0
    treeData.value = []
    commandTreeRaw.value = []
    filterCommandId.value = ''
    filterCommandType.value = ''
    if (!pid) {
      versionContext.setVersionId('')
      return
    }
    await loadVersionOptions(pid)
    await loadCommandSelectOptions(pid)
    await loadCommandTree()
    const canRestore =
      !!restoreVid &&
      (restoreVid === VERSION_ALL || versionOptions.value.some((v) => v.versionId === restoreVid))
    if (canRestore) {
      versionId.value = restoreVid
      versionContext.setVersionId(restoreVid)
      await onVersionChange()
    } else {
      versionContext.setVersionId('')
    }
  },
  { immediate: true },
)

async function loadVersionOptions(pid: string) {
  versionOptions.value = []
  if (!pid) return
  const resp = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${encodeURIComponent(pid)}/versions`,
    method: 'GET',
    params: { page: 1, size: 200 },
  })
  versionOptions.value = (resp.data.records as Record<string, unknown>[]).map((r) => ({
    versionId: String(r.versionId || ''),
    versionName: String(r.versionName || '').trim() || '未命名版本',
  }))
}

async function loadCommandSelectOptions(pid: string) {
  commandSelectOptions.value = []
  if (!pid) return
  commandSelectLoading.value = true
  try {
    const data = await fetchCommands(pid, { page: 1, size: 500 })
    commandSelectOptions.value = (data.records || []).map((r) => ({
      commandId: String(r.commandId || ''),
      commandName: String(r.commandName || '').trim() || '未命名命令',
    }))
  } finally {
    commandSelectLoading.value = false
  }
}

async function onVersionChange() {
  versionContext.setVersionId(versionId.value || '')
  filterCommandId.value = ''
  filterCommandType.value = ''
  treeRef.value?.setCurrentKey('__all__')
  page.value = 1
  if (versionId.value) {
    await loadBaselineCount()
    await loadRows()
  } else {
    rows.value = []
    total.value = 0
    baselineCount.value = 0
  }
}

onMounted(() => {
  loadTypeBits()
  loadChangeTypes()
})
</script>

<style scoped>
.filter-row {
  margin-bottom: 8px;
}

.param-split {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
  min-height: 420px;
}

.param-aside {
  border-right: 1px solid #eee;
  padding: 10px;
  box-sizing: border-box;
}

.aside-title {
  color: #555;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.param-main-inner {
  padding: 10px 12px;
}

.param-toolbar {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}

.param-toolbar-with-oplog {
  justify-content: space-between;
  align-items: flex-start;
}

.param-toolbar-main {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.param-toolbar-with-oplog .op-log-link {
  flex-shrink: 0;
  align-self: center;
}

.param-expand {
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 4px;
}
.param-expand-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 8px 16px;
}
.param-expand-grid .kv {
  display: flex;
  gap: 8px;
  min-width: 0;
}
.param-expand-grid .kv.full {
  grid-column: 1 / -1;
}
.param-expand-grid .k {
  flex: 0 0 auto;
  color: #666;
  font-weight: 600;
  white-space: nowrap;
}
.param-expand-grid .v {
  flex: 1 1 auto;
  color: #333;
  min-width: 0;
  word-break: break-word;
}

.sync-wrap {
  display: inline-block;
}

.baseline-tag {
  margin-left: 4px;
}

.bit-check-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(72px, 1fr));
  gap: 8px 12px;
  max-width: 640px;
}

.bit-check-grid :deep(.el-checkbox.bit-occupied) {
  color: #888;
}

.hint {
  color: #888;
  font-size: 12px;
  margin-top: 4px;
}

.template-link {
  color: #2b91f6;
  cursor: pointer;
  text-decoration: underline;
}

.param-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.param-form-scroll {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  padding-right: 8px;
}

.label-tip {
  color: #888;
  cursor: help;
  margin-left: 4px;
}
</style>
