package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;

public final class BusinessCategoryAssembler {

    private BusinessCategoryAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static BusinessCategory toDomain(EntityBusinessCategoryPo po) {
        return BusinessCategory.rehydrate(
                new BusinessCategory.Snapshot(
                        po.getOwnedProductId(),
                        po.getCategoryId(),
                        po.getCategoryNameCn(),
                        po.getCategoryNameEn(),
                        po.getFeatureRange(),
                        po.getCategoryType(),
                        po.getCategoryStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param b 领域对象
     * @return 持久化对象
     */
    public static EntityBusinessCategoryPo toPo(BusinessCategory b) {
        EntityBusinessCategoryPo po = new EntityBusinessCategoryPo();
        po.setOwnedProductId(b.getOwnedProductId());
        po.setCategoryId(b.getCategoryId());
        po.setCategoryNameCn(b.getCategoryNameCn());
        po.setCategoryNameEn(b.getCategoryNameEn());
        po.setFeatureRange(b.getFeatureRange());
        po.setCategoryType(b.getCategoryType());
        po.setCategoryStatus(b.getCategoryStatus());
        po.setCreatorId(b.getCreatorId());
        po.setCreationTimestamp(b.getCreationTimestamp());
        po.setUpdaterId(b.getUpdaterId());
        po.setUpdateTimestamp(b.getUpdateTimestamp());
        return po;
    }
}
