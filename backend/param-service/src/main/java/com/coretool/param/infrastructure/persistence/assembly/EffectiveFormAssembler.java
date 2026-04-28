package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveFormDictPo;

public final class EffectiveFormAssembler {

    private EffectiveFormAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static EffectiveForm toDomain(EntityEffectiveFormDictPo po) {
        return EffectiveForm.rehydrate(
                new EffectiveForm.Snapshot(
                        po.getOwnedProductId(),
                        po.getEffectiveFormId(),
                        po.getEffectiveFormNameCn(),
                        po.getEffectiveFormNameEn(),
                        po.getEffectiveFormDescription(),
                        po.getEffectiveFormStatus(),
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
    public static EntityEffectiveFormDictPo toPo(EffectiveForm f) {
        EntityEffectiveFormDictPo po = new EntityEffectiveFormDictPo();
        po.setOwnedProductId(f.getOwnedProductId());
        po.setEffectiveFormId(f.getEffectiveFormId());
        po.setEffectiveFormNameCn(f.getEffectiveFormNameCn());
        po.setEffectiveFormNameEn(f.getEffectiveFormNameEn());
        po.setEffectiveFormDescription(f.getEffectiveFormDescription());
        po.setEffectiveFormStatus(f.getEffectiveFormStatus());
        po.setCreatorId(f.getCreatorId());
        po.setCreationTimestamp(f.getCreationTimestamp());
        po.setUpdaterId(f.getUpdaterId());
        po.setUpdateTimestamp(f.getUpdateTimestamp());
        return po;
    }
}
