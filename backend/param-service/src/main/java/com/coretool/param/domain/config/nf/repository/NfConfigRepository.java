package com.coretool.param.domain.config.nf.repository;

import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface NfConfigRepository {

    Optional<NfConfigEntry> findById(String nfConfigId);

    boolean existsSameNameInProduct(String productId, String nfConfigNameCn, String excludeNfConfigId);

    /** 同一产品下按 NF 名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<NfConfigEntry> findDisabledByNameInProduct(String productId, String nfConfigNameCn);

    void insert(NfConfigEntry entry);

    void update(NfConfigEntry entry);

    PageSlice<NfConfigEntry> pageByProduct(String productId, int page, int size, String nameKeyword);
}

