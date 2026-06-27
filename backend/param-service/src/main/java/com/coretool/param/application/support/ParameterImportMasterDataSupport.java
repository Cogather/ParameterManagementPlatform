/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityBusinessCategoryMapper;
import com.coretool.param.infrastructure.persistence.mapper.VersionFeatureDictMapper;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数导入时从主数据字典解析业务分类、所属特性 ID。
 *
 * @since 2026-06-11
 */
public final class ParameterImportMasterDataSupport {

    private static final int STATUS_DISABLED = 0;

    private ParameterImportMasterDataSupport() {}

    /**
     * 加载产品下业务分类名称（或 ID）到 categoryId 的映射。
     *
     * @param productId      产品 ID
     * @param categoryMapper 业务分类 Mapper
     * @return 中文名、英文名或 categoryId → categoryId
     */
    public static Map<String, String> loadCategoryIdByTokenMap(
            String productId, EntityBusinessCategoryMapper categoryMapper) {
        List<EntityBusinessCategoryPo> list =
                categoryMapper.selectList(
                        new LambdaQueryWrapper<EntityBusinessCategoryPo>()
                                .eq(EntityBusinessCategoryPo::getOwnedProductId, productId)
                                .ne(EntityBusinessCategoryPo::getCategoryStatus, STATUS_DISABLED));
        Map<String, String> out = new HashMap<>();
        for (EntityBusinessCategoryPo row : list) {
            if (row == null || StringUtils.isBlank(row.getCategoryId())) {
                continue;
            }
            String id = row.getCategoryId().trim();
            out.putIfAbsent(id, id);
            putNameToken(out, row.getCategoryNameCn(), id);
            putNameToken(out, row.getCategoryNameEn(), id);
        }
        return out;
    }

    /**
     * 加载版本下特性名称（或 ID）到 featureId 的映射。
     *
     * @param productId      产品 ID
     * @param versionId      版本 ID
     * @param featureMapper  版本特性 Mapper
     * @return 中文名、英文名或 featureId → featureId
     */
    public static Map<String, String> loadFeatureIdByTokenMap(
            String productId, String versionId, VersionFeatureDictMapper featureMapper) {
        List<VersionFeatureDictPo> list =
                featureMapper.selectList(
                        new LambdaQueryWrapper<VersionFeatureDictPo>()
                                .eq(VersionFeatureDictPo::getOwnedProductPbiId, productId)
                                .eq(VersionFeatureDictPo::getOwnedVersionId, versionId)
                                .ne(VersionFeatureDictPo::getFeatureStatus, STATUS_DISABLED));
        Map<String, String> out = new HashMap<>();
        for (VersionFeatureDictPo row : list) {
            if (row == null || StringUtils.isBlank(row.getFeatureId())) {
                continue;
            }
            String id = row.getFeatureId().trim();
            out.putIfAbsent(id, id);
            putNameToken(out, row.getFeatureNameCn(), id);
            putNameToken(out, row.getFeatureNameEn(), id);
        }
        return out;
    }

    /**
     * 当 Excel 行填写了业务分类/所属特性时，解析并写入 ID 字段。
     *
     * @param po                  待写入的参数对象
     * @param businessCategoryCell 「业务分类」列原文（空表示未导入该列）
     * @param featureCell           「所属特性」列原文（空表示未导入该列）
     * @param categoryIdByToken     业务分类 token → categoryId
     * @param featureIdByToken      特性 token → featureId
     * @throws DomainRuleException 列有值但无法识别时
     */
    public static void applyDictionaryIdsFromImportCells(
            SystemParameterPo po,
            String businessCategoryCell,
            String featureCell,
            Map<String, String> categoryIdByToken,
            Map<String, String> featureIdByToken) {
        applyCategoryId(po, businessCategoryCell, categoryIdByToken);
        applyFeatureId(po, featureCell, featureIdByToken);
    }

    private static void applyCategoryId(
            SystemParameterPo po, String cell, Map<String, String> categoryIdByToken) {
        if (StringUtils.isBlank(cell)) {
            return;
        }
        String token = cell.trim();
        String resolved = categoryIdByToken.get(token);
        if (StringUtils.isBlank(resolved)) {
            throw new DomainRuleException("业务分类无法识别: " + token);
        }
        po.setCategoryId(resolved);
        po.setBusinessClassification(token);
    }

    private static void applyFeatureId(
            SystemParameterPo po, String cell, Map<String, String> featureIdByToken) {
        if (StringUtils.isBlank(cell)) {
            return;
        }
        String token = cell.trim();
        String resolved = featureIdByToken.get(token);
        if (StringUtils.isBlank(resolved)) {
            throw new DomainRuleException("所属特性无法识别: " + token);
        }
        po.setFeatureId(resolved);
        po.setFeature(token);
    }

    private static void putNameToken(Map<String, String> out, String name, String id) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        out.putIfAbsent(name.trim(), id);
    }
}
