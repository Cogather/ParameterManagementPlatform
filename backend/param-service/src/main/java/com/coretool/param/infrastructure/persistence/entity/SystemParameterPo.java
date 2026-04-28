package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("system_parameter")
public class SystemParameterPo {

    @TableId(type = IdType.AUTO)
    private Integer parameterId;
    private String parameterCode;
    private String ownedObjectType;
    private String ownedObjectCode;
    private String tenantId;
    private Integer domainId;
    private String dataStatus;
    private String ownedProductId;
    private String ownedVersionId;
    private String ownedCommandId;
    private String introduceType;
    private String inheritReferenceVersionId;
    private String parameterNameCn;
    private String parameterNameEn;
    private String bitUsage;
    private Integer parameterSequence;
    private String valueRange;
    private String valueDescriptionCn;
    private String valueDescriptionEn;
    private String applicationScenarioCn;
    private String applicationScenarioEn;
    private String parameterDefaultValue;
    private String parameterRecommendedValue;
    private String applicableNe;
    private String feature;
    private String featureId;
    private String businessClassification;
    private String categoryId;
    private String takeEffectImmediately;
    private String effectiveModeCn;
    private String effectiveModeEn;
    private String projectTeam;
    private String belongingModule;
    private String patchVersion;
    private String introducedVersion;
    private String parameterDescriptionCn;
    private String parameterDescriptionEn;
    private String impactDescriptionCn;
    private String impactDescriptionEn;
    private String configurationExampleCn;
    private String configurationExampleEn;
    private String helpDocumentInDatabase;
    private String relatedParameterCn;
    private String relatedParameterEn;
    private String relatedParameterUsage;
    private String relatedParameterDescriptionCn;
    private String relatedParameterDescriptionEn;
    private String remark;
    private String parameterBit;
    private String enumerationValuesCn;
    private String enumerationValuesEn;
    private String applicableLogicalEntityCn;
    private String applicableLogicalEntityEn;
    private String relatedFeatureCn;
    private String relatedFeatureEn;
    private String changeFactors;
    private String changeRelatedNumber;
    private String changeSource;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
    private String parameterUnitCn;
    private String parameterUnitEn;
    private String parameterRange;
    private String effectiveFormCn;
    private String effectiveFormEn;
    private String implementationPrincipleCn;
    private String implementationPrincipleEn;
    private String impactLevelCn;
    private String impactLevelEn;
    private String figureExampleCn;
    private String figureExampleEn;
    private String internalDescription;
}
