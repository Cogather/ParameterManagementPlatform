package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.command.Command;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;

public final class CommandAssembler {

    private CommandAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static Command toDomain(EntityCommandMappingPo po) {
        if (po == null) {
            return null;
        }
        return Command.rehydrate(
                new Command.Snapshot(
                        po.getOwnedProductId(),
                        po.getCommandId(),
                        po.getCommandName(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp(),
                        po.getOwnerList(),
                        po.getCommandStatus()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param c 领域对象
     * @return 持久化对象
     */
    public static EntityCommandMappingPo toPo(Command c) {
        if (c == null) {
            return null;
        }
        EntityCommandMappingPo po = new EntityCommandMappingPo();
        po.setOwnedProductId(c.getOwnedProductId());
        po.setCommandId(c.getCommandId());
        po.setCommandName(c.getCommandName());
        po.setCreatorId(c.getCreatorId());
        po.setCreationTimestamp(c.getCreationTimestamp());
        po.setUpdaterId(c.getUpdaterId());
        po.setUpdateTimestamp(c.getUpdateTimestamp());
        po.setOwnerList(c.getOwnerList());
        po.setCommandStatus(c.getCommandStatus());
        return po;
    }
}
