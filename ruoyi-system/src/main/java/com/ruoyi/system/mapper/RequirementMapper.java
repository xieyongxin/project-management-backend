package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementOwner;
import com.ruoyi.system.domain.RequirementVersion;

public interface RequirementMapper
{
    int insertRequirement(Requirement requirement);

    int insertRequirementVersion(RequirementVersion version);

    int updateCurrentVersion(@Param("requirementId") Long requirementId,
        @Param("versionId") Long versionId);

    int insertRequirementOwner(@Param("requirementId") Long requirementId,
        @Param("userId") Long userId);

    Requirement selectRequirementForUser(@Param("projectId") Long projectId,
        @Param("requirementId") Long requirementId, @Param("userId") Long userId);

    List<Requirement> selectRequirementsForUser(@Param("projectId") Long projectId,
        @Param("userId") Long userId);

    List<RequirementOwner> selectRequirementOwners(@Param("requirementId") Long requirementId);

    List<RequirementOwner> selectProjectMembersByIds(@Param("projectId") Long projectId,
        @Param("userIds") List<Long> userIds);

    SysDictData selectActiveRequirementStatus(@Param("status") String status);

    List<SysDictData> selectActiveRequirementStatuses();

}
