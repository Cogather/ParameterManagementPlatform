package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;

public final class NfConfigEntryAssembler {

    private NfConfigEntryAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static NfConfigEntry toDomain(EntityNfConfigDictPo po) {
        return NfConfigEntry.rehydrate(
                new NfConfigEntry.Snapshot(
                        po.getOwnedProductId(),
                        po.getNfConfigId(),
                        po.getNfConfigNameCn(),
                        po.getNfConfigDescription(),
                        po.getNfConfigStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param e 领域对象
     * @return 持久化对象
     */
    public static EntityNfConfigDictPo toPo(NfConfigEntry e) {
        EntityNfConfigDictPo po = new EntityNfConfigDictPo();
        po.setOwnedProductId(e.getOwnedProductId());
        po.setNfConfigId(e.getNfConfigId());
        po.setNfConfigNameCn(e.getNfConfigNameCn());
        po.setNfConfigDescription(e.getNfConfigDescription());
        po.setNfConfigStatus(e.getNfConfigStatus());
        po.setCreatorId(e.getCreatorId());
        po.setCreationTimestamp(e.getCreationTimestamp());
        po.setUpdaterId(e.getUpdaterId());
        po.setUpdateTimestamp(e.getUpdateTimestamp());
        return po;
    }
}
