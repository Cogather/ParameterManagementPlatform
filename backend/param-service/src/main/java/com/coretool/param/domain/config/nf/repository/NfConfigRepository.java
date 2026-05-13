package com.coretool.param.domain.config.nf.repository;

import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「NfConfigRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface NfConfigRepository {

/**
 * findById。
 *
 * @param nfConfigId 见方法签名
 * @return 可选结果
 */

    Optional<NfConfigEntry> findById(String nfConfigId);

/**
 * existsSameNameInProduct。
 *
 * @param productId 见方法签名
 * @param nfConfigNameCn 见方法签名
 * @param excludeNfConfigId 见方法签名
 * @return 布尔结果
 */

    boolean existsSameNameInProduct(String productId, String nfConfigNameCn, String excludeNfConfigId);

    /**
     * 同一产品下按 NF 名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param nfConfigNameCn NF 配置中文名称
     * @return 若存在则返回配置项
     */
    Optional<NfConfigEntry> findDisabledByNameInProduct(String productId, String nfConfigNameCn);

/**
 * insert。
 *
 * @param entry 见方法签名
 */

    void insert(NfConfigEntry entry);

/**
 * update。
 *
 * @param entry 见方法签名
 */

    void update(NfConfigEntry entry);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param nameKeyword 见方法签名
 * @return 结果
 */

    PageSlice<NfConfigEntry> pageByProduct(String productId, int page, int size, String nameKeyword);
}
