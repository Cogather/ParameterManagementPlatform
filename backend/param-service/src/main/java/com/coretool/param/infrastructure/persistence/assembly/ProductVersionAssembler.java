package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;

public final class ProductVersionAssembler {

    private ProductVersionAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static ProductVersion toDomain(EntityVersionInfoPo po) {
        int vs = po.getVersionStatus() == null ? 1 : po.getVersionStatus();
        return ProductVersion.rehydrate(
                new ProductVersion.Snapshot(
                        po.getOwnedProductId(),
                        po.getVersionId(),
                        po.getVersionName(),
                        po.getVersionType(),
                        po.getVersionDescription(),
                        po.getBaselineVersionId(),
                        po.getBaselineVersionName(),
                        po.getVersionDesc(),
                        po.getApprover(),
                        po.getIsHidden(),
                        po.getSupportedVersion(),
                        po.getIntroducedProductId(),
                        po.getOwnerList(),
                        vs,
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param v 领域对象
     * @return 持久化对象
     */
    public static EntityVersionInfoPo toPo(ProductVersion v) {
        EntityVersionInfoPo po = new EntityVersionInfoPo();
        po.setOwnedProductId(v.getOwnedProductId());
        po.setVersionId(v.getVersionId());
        po.setVersionName(v.getVersionName());
        po.setVersionType(v.getVersionType());
        po.setVersionDescription(v.getVersionDescription());
        po.setBaselineVersionId(v.getBaselineVersionId());
        po.setBaselineVersionName(v.getBaselineVersionName());
        po.setVersionDesc(v.getVersionDesc());
        po.setApprover(v.getApprover());
        po.setIsHidden(v.getIsHidden());
        po.setSupportedVersion(v.getSupportedVersion());
        po.setVersionStatus(v.getVersionStatus());
        po.setIntroducedProductId(v.getIntroducedProductId());
        po.setOwnerList(v.getOwnerList());
        po.setCreatorId(v.getCreatorId());
        po.setCreationTimestamp(v.getCreationTimestamp());
        po.setUpdaterId(v.getUpdaterId());
        po.setUpdateTimestamp(v.getUpdateTimestamp());
        return po;
    }
}
