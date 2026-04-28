package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;

public final class ApplicableNeAssembler {

    private ApplicableNeAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static ApplicableNe toDomain(EntityApplicableNeDictPo po) {
        return ApplicableNe.rehydrate(
                new ApplicableNe.Snapshot(
                        po.getOwnedProductId(),
                        po.getNeTypeId(),
                        po.getNeTypeNameCn(),
                        po.getNeTypeDescription(),
                        po.getNeTypeStatus(),
                        po.getProductForm(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param n 领域对象
     * @return 持久化对象
     */
    public static EntityApplicableNeDictPo toPo(ApplicableNe n) {
        EntityApplicableNeDictPo po = new EntityApplicableNeDictPo();
        po.setOwnedProductId(n.getOwnedProductId());
        po.setNeTypeId(n.getNeTypeId());
        po.setNeTypeNameCn(n.getNeTypeNameCn());
        po.setNeTypeDescription(n.getNeTypeDescription());
        po.setNeTypeStatus(n.getNeTypeStatus());
        po.setProductForm(n.getProductForm());
        po.setCreatorId(n.getCreatorId());
        po.setCreationTimestamp(n.getCreationTimestamp());
        po.setUpdaterId(n.getUpdaterId());
        po.setUpdateTimestamp(n.getUpdateTimestamp());
        return po;
    }
}
