package com.coretool.param.domain.config.effectiveform.repository;

import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface EffectiveFormRepository {

    Optional<EffectiveForm> findById(String effectiveFormId);

    /** 同一产品下按中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<EffectiveForm> findDisabledByNameCnInProduct(String productId, String effectiveFormNameCn);

    void insert(EffectiveForm form);

    void update(EffectiveForm form);

    PageSlice<EffectiveForm> pageByProduct(String productId, int page, int size, String keyword);
}

