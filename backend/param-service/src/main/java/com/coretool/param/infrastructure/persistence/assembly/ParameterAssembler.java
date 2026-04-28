package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.parameter.Parameter;
import com.coretool.param.domain.parameter.ParameterAllocationDomainService;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

public final class ParameterAssembler {

    private ParameterAssembler() {}

    /**
     * 持久化对象转换为领域对象（仅承载写入不变量判断所需字段）。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static Parameter toDomain(SystemParameterPo po) {
        return new Parameter(po.getDataStatus());
    }

    /**
     * 持久化对象转换为参数分配计算所需快照。
     *
     * @param p 持久化对象
     * @return 快照
     */
    public static ParameterAllocationDomainService.ParameterSnapshot toSnapshot(SystemParameterPo p) {
        return new ParameterAllocationDomainService.ParameterSnapshot(
                p.getParameterCode(),
                p.getParameterSequence(),
                p.getBitUsage(),
                p.getOwnedCommandId());
    }
}
