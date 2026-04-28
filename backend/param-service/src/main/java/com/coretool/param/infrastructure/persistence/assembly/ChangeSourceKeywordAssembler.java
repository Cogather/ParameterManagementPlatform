package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeSourceKeywordPo;

public final class ChangeSourceKeywordAssembler {

    private ChangeSourceKeywordAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static ChangeSourceKeyword toDomain(ConfigChangeSourceKeywordPo po) {
        int st = po.getKeywordStatus() == null ? 1 : po.getKeywordStatus();
        return ChangeSourceKeyword.rehydrate(
                new ChangeSourceKeyword.Snapshot(
                        po.getOwnedProductId(),
                        po.getKeywordId(),
                        po.getKeywordRegex(),
                        po.getReason(),
                        st,
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param k 领域对象
     * @return 持久化对象
     */
    public static ConfigChangeSourceKeywordPo toPo(ChangeSourceKeyword k) {
        ConfigChangeSourceKeywordPo po = new ConfigChangeSourceKeywordPo();
        po.setOwnedProductId(k.getOwnedProductId());
        po.setKeywordId(k.getKeywordId());
        po.setKeywordRegex(k.getKeywordRegex());
        po.setReason(k.getReason());
        po.setKeywordStatus(k.getKeywordStatus());
        po.setCreatorId(k.getCreatorId());
        po.setCreationTimestamp(k.getCreationTimestamp());
        po.setUpdaterId(k.getUpdaterId());
        po.setUpdateTimestamp(k.getUpdateTimestamp());
        return po;
    }
}
