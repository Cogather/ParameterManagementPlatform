package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;

public final class CommandTypeDefinitionAssembler {

    private CommandTypeDefinitionAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static CommandTypeDefinition toDomain(CommandTypeDefinitionPo po) {
        if (po == null) {
            return null;
        }
        return CommandTypeDefinition.rehydrate(
                new CommandTypeDefinition.Snapshot(
                        po.getOwnedProductId(),
                        po.getOwnedCommandId(),
                        po.getCommandTypeId(),
                        po.getCommandTypeName(),
                        po.getCommandType(),
                        po.getMinValue(),
                        po.getMaxValue(),
                        po.getOccupiedSerialNumber(),
                        po.getCommandTypeStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param t 领域对象
     * @return 持久化对象
     */
    public static CommandTypeDefinitionPo toPo(CommandTypeDefinition t) {
        if (t == null) {
            return null;
        }
        CommandTypeDefinitionPo po = new CommandTypeDefinitionPo();
        po.setOwnedProductId(t.getOwnedProductId());
        po.setOwnedCommandId(t.getOwnedCommandId());
        po.setCommandTypeId(t.getCommandTypeId());
        po.setCommandTypeName(t.getCommandTypeName());
        po.setCommandType(t.getCommandType());
        po.setMinValue(t.getMinValue());
        po.setMaxValue(t.getMaxValue());
        po.setOccupiedSerialNumber(t.getOccupiedSerialNumber());
        po.setCommandTypeStatus(t.getCommandTypeStatus());
        po.setCreatorId(t.getCreatorId());
        po.setCreationTimestamp(t.getCreationTimestamp());
        po.setUpdaterId(t.getUpdaterId());
        po.setUpdateTimestamp(t.getUpdateTimestamp());
        return po;
    }
}
