package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo;

public final class EffectiveModeAssembler {

    private EffectiveModeAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static EffectiveMode toDomain(EntityEffectiveModeDictPo po) {
        return EffectiveMode.rehydrate(
                new EffectiveMode.Snapshot(
                        po.getOwnedProductId(),
                        po.getEffectiveModeId(),
                        po.getEffectiveModeNameCn(),
                        po.getEffectiveModeNameEn(),
                        po.getEffectiveModeDescription(),
                        po.getEffectiveModeStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param m 领域对象
     * @return 持久化对象
     */
    public static EntityEffectiveModeDictPo toPo(EffectiveMode m) {
        EntityEffectiveModeDictPo po = new EntityEffectiveModeDictPo();
        po.setOwnedProductId(m.getOwnedProductId());
        po.setEffectiveModeId(m.getEffectiveModeId());
        po.setEffectiveModeNameCn(m.getEffectiveModeNameCn());
        po.setEffectiveModeNameEn(m.getEffectiveModeNameEn());
        po.setEffectiveModeDescription(m.getEffectiveModeDescription());
        po.setEffectiveModeStatus(m.getEffectiveModeStatus());
        po.setCreatorId(m.getCreatorId());
        po.setCreationTimestamp(m.getCreationTimestamp());
        po.setUpdaterId(m.getUpdaterId());
        po.setUpdateTimestamp(m.getUpdateTimestamp());
        return po;
    }
}
