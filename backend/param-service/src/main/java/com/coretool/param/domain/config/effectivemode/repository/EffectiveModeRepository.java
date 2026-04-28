package com.coretool.param.domain.config.effectivemode.repository;

import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface EffectiveModeRepository {

    Optional<EffectiveMode> findById(String effectiveModeId);

    /** 同一产品下按中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<EffectiveMode> findDisabledByNameCnInProduct(String productId, String effectiveModeNameCn);

    void insert(EffectiveMode mode);

    void update(EffectiveMode mode);

    PageSlice<EffectiveMode> pageByProduct(String productId, int page, int size, String keyword);
}

