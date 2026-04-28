package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;

public final class VersionFeatureAssembler {

    private VersionFeatureAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static VersionFeature toDomain(VersionFeatureDictPo po) {
        return VersionFeature.rehydrate(
                new VersionFeature.Snapshot(
                        po.getOwnedProductPbiId(),
                        po.getOwnedVersionId(),
                        po.getFeatureId(),
                        po.getFeatureCode(),
                        po.getFeatureNameCn(),
                        po.getFeatureNameEn(),
                        po.getIntroduceType(),
                        po.getInheritReferenceVersionId(),
                        po.getFeatureStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param f 领域对象
     * @return 持久化对象
     */
    public static VersionFeatureDictPo toPo(VersionFeature f) {
        VersionFeatureDictPo po = new VersionFeatureDictPo();
        po.setOwnedProductPbiId(f.getOwnedProductPbiId());
        po.setOwnedVersionId(f.getOwnedVersionId());
        po.setFeatureId(f.getFeatureId());
        po.setFeatureCode(f.getFeatureCode());
        po.setFeatureNameCn(f.getFeatureNameCn());
        po.setFeatureNameEn(f.getFeatureNameEn());
        po.setIntroduceType(f.getIntroduceType());
        po.setInheritReferenceVersionId(f.getInheritReferenceVersionId());
        po.setFeatureStatus(f.getFeatureStatus());
        po.setCreatorId(f.getCreatorId());
        po.setCreationTimestamp(f.getCreationTimestamp());
        po.setUpdaterId(f.getUpdaterId());
        po.setUpdateTimestamp(f.getUpdateTimestamp());
        return po;
    }
}
