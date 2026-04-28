package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;

public final class CommandTypeVersionRangeAssembler {

    private CommandTypeVersionRangeAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static CommandTypeVersionRange toDomain(CommandTypeVersionRangePo po) {
        if (po == null) {
            return null;
        }
        return CommandTypeVersionRange.rehydrate(
                new CommandTypeVersionRange.Snapshot(
                        po.getOwnedProductId(),
                        po.getOwnedCommandId(),
                        po.getOwnedTypeId(),
                        po.getVersionRangeId(),
                        po.getStartIndex(),
                        po.getEndIndex(),
                        po.getRangeDescription(),
                        po.getRangeType(),
                        po.getOwnedVersionOrBusinessId(),
                        po.getRangeStatus(),
                        po.getCreatorId(),
                        po.getCreationTimestamp(),
                        po.getUpdaterId(),
                        po.getUpdateTimestamp()));
    }

    /**
     * 领域对象转换为持久化对象。
     *
     * @param r 领域对象
     * @return 持久化对象
     */
    public static CommandTypeVersionRangePo toPo(CommandTypeVersionRange r) {
        if (r == null) {
            return null;
        }
        CommandTypeVersionRangePo po = new CommandTypeVersionRangePo();
        po.setVersionRangeId(r.getVersionRangeId());
        po.setOwnedProductId(r.getOwnedProductId());
        po.setOwnedCommandId(r.getOwnedCommandId());
        po.setOwnedTypeId(r.getOwnedTypeId());
        po.setStartIndex(r.getStartIndex());
        po.setEndIndex(r.getEndIndex());
        po.setRangeDescription(r.getRangeDescription());
        po.setRangeType(r.getRangeType());
        po.setOwnedVersionOrBusinessId(r.getOwnedVersionOrBusinessId());
        po.setRangeStatus(r.getRangeStatus());
        po.setCreatorId(r.getCreatorId());
        po.setCreationTimestamp(r.getCreationTimestamp());
        po.setUpdaterId(r.getUpdaterId());
        po.setUpdateTimestamp(r.getUpdateTimestamp());
        return po;
    }
}
