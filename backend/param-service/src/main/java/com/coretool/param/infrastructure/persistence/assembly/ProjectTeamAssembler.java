package com.coretool.param.infrastructure.persistence.assembly;

import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.infrastructure.persistence.entity.ProjectTeamDictPo;

public final class ProjectTeamAssembler {

    private ProjectTeamAssembler() {}

    /**
     * 持久化对象转换为领域对象。
     *
     * @param po 持久化对象
     * @return 领域对象
     */
    public static ProjectTeam toDomain(ProjectTeamDictPo po) {
        return ProjectTeam.rehydrate(
                new ProjectTeam.Snapshot(
                        po.getOwnedProductId(),
                        po.getTeamId(),
                        po.getTeamName(),
                        po.getTeamDescription(),
                        po.getTeamStatus(),
                        po.getOwnerList(),
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
    public static ProjectTeamDictPo toPo(ProjectTeam t) {
        ProjectTeamDictPo po = new ProjectTeamDictPo();
        po.setOwnedProductId(t.getOwnedProductId());
        po.setTeamId(t.getTeamId());
        po.setTeamName(t.getTeamName());
        po.setTeamDescription(t.getTeamDescription());
        po.setTeamStatus(t.getTeamStatus());
        po.setOwnerList(t.getOwnerList());
        po.setCreatorId(t.getCreatorId());
        po.setCreationTimestamp(t.getCreationTimestamp());
        po.setUpdaterId(t.getUpdaterId());
        po.setUpdateTimestamp(t.getUpdateTimestamp());
        return po;
    }
}
