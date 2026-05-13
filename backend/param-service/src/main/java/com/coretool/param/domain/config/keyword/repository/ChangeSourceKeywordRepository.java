package com.coretool.param.domain.config.keyword.repository;

import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.support.PageSlice;

import java.util.List;
import java.util.Optional;

/**
 * 领域仓储接口「ChangeSourceKeywordRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface ChangeSourceKeywordRepository {

/**
 * findByKeywordId。
 *
 * @param keywordId 见方法签名
 * @return 可选结果
 */

    Optional<ChangeSourceKeyword> findByKeywordId(String keywordId);

/**
 * insert。
 *
 * @param keyword 见方法签名
 */

    void insert(ChangeSourceKeyword keyword);

/**
 * update。
 *
 * @param keyword 见方法签名
 */

    void update(ChangeSourceKeyword keyword);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param textKeyword 见方法签名
 * @return 结果
 */

    PageSlice<ChangeSourceKeyword> pageByProduct(String productId, int page, int size, String textKeyword);

    /**
     * 查询某产品下 keyword_status=1 的关键字正则列表（用于黑名单校验）。
     *
     * @param productId 产品 ID
     * @return 启用的正则表达式列表
     */
    List<String> listEnabledRegexesByProduct(String productId);
}
