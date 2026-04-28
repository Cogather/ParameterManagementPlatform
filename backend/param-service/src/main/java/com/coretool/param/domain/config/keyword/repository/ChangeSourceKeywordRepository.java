package com.coretool.param.domain.config.keyword.repository;

import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.support.PageSlice;

import java.util.List;
import java.util.Optional;

public interface ChangeSourceKeywordRepository {

    Optional<ChangeSourceKeyword> findByKeywordId(String keywordId);

    void insert(ChangeSourceKeyword keyword);

    void update(ChangeSourceKeyword keyword);

    PageSlice<ChangeSourceKeyword> pageByProduct(String productId, int page, int size, String textKeyword);

    /** keyword_status=1 的 keyword_regex 列表（黑名单校验）。 */
    List<String> listEnabledRegexesByProduct(String productId);
}

