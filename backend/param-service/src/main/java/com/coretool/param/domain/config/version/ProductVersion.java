package com.coretool.param.domain.config.version;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 产品版本聚合（表 entity_version_info）。
 */
public class ProductVersion {

    public record Registration(
            String ownedProductId,
            String versionId,
            String versionName,
            String versionType,
            String versionDescription,
            String baselineVersionId,
            String baselineVersionName,
            String versionDesc,
            String approver,
            String isHidden,
            String supportedVersion,
            String introducedProductId,
            String ownerList,
            Integer versionStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String versionId,
            String versionName,
            String versionType,
            String versionDescription,
            String baselineVersionId,
            String baselineVersionName,
            String versionDesc,
            String approver,
            String isHidden,
            String supportedVersion,
            String introducedProductId,
            String ownerList,
            Integer versionStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record BranchFromBaselineCommand(
            ProductVersion baseline,
            String productId,
            String newVersionId,
            String newVersionName,
            String supportedVersionOverride,
            String descriptionOverride,
            LocalDateTime now) {}

    public record AttributePatch(
            String supportedVersion,
            String versionDescription,
            String versionDesc,
            Integer versionStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String versionId;
    private String versionName;
    private String versionType;
    private String versionDescription;
    private String baselineVersionId;
    private String baselineVersionName;
    private String versionDesc;
    private String approver;
    private String isHidden;
    private String supportedVersion;
    private int versionStatus;
    private String introducedProductId;
    private String ownerList;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新产品版本（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新产品版本
     */
    public static ProductVersion registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.versionId(), input.versionName(), input.versionType())) {
            throw new DomainRuleException("版本ID、名称与版本类型不能为空");
        }
        if (StringUtils.isBlank(input.ownerList())) {
            throw new DomainRuleException("责任人不能为空");
        }
        ProductVersion v = new ProductVersion();
        v.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        v.versionId = input.versionId().trim();
        v.versionName = input.versionName().trim();
        v.versionType = input.versionType().trim();
        v.versionDescription = input.versionDescription();
        v.baselineVersionId = input.baselineVersionId();
        v.baselineVersionName = input.baselineVersionName();
        v.versionDesc = input.versionDesc();
        v.approver = input.approver();
        v.isHidden = input.isHidden();
        v.supportedVersion = StringUtils.trimToNull(input.supportedVersion());
        v.introducedProductId = input.introducedProductId();
        v.ownerList = input.ownerList().trim();
        v.versionStatus = input.versionStatus() == null ? 1 : input.versionStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        v.creatorId = c;
        v.creationTimestamp = input.now();
        v.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        v.updateTimestamp = input.now();
        return v;
    }

    /**
     * 从持久化数据重建产品版本（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的产品版本
     */
    public static ProductVersion rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        ProductVersion v = new ProductVersion();
        v.ownedProductId = input.ownedProductId();
        v.versionId = input.versionId();
        v.versionName = input.versionName();
        v.versionType = input.versionType();
        v.versionDescription = input.versionDescription();
        v.baselineVersionId = input.baselineVersionId();
        v.baselineVersionName = input.baselineVersionName();
        v.versionDesc = input.versionDesc();
        v.approver = input.approver();
        v.isHidden = input.isHidden();
        v.supportedVersion = input.supportedVersion();
        v.introducedProductId = input.introducedProductId();
        v.ownerList = input.ownerList();
        v.versionStatus = input.versionStatus() == null ? 1 : input.versionStatus();
        v.creatorId = input.creatorId();
        v.creationTimestamp = input.creationTimestamp();
        v.updaterId = input.updaterId();
        v.updateTimestamp = input.updateTimestamp();
        return v;
    }

    /**
     * 基于基线版本拉分支创建新版本（仅生成版本实体，参数复制由应用层/其它用例处理）。
     *
     * @param input 拉分支入参，各字段与 {@link BranchFromBaselineCommand} 记录组件一一对应
     * @return 新版本
     */
    public static ProductVersion branchFromBaseline(BranchFromBaselineCommand input) {
        if (input == null) {
            throw new DomainRuleException("拉分支参数不能为空");
        }
        ProductVersion baseline = input.baseline();
        if (baseline == null || !baseline.belongsToProduct(input.productId())) {
            throw new DomainRuleException("基线版本不存在或不属于该产品");
        }
        if (StringUtils.isAnyBlank(input.newVersionId(), input.newVersionName())) {
            throw new DomainRuleException("拉分支参数不完整");
        }
        String sv =
                StringUtils.isNotBlank(input.supportedVersionOverride())
                        ? input.supportedVersionOverride()
                        : baseline.supportedVersion;
        String desc =
                input.descriptionOverride() != null ? input.descriptionOverride() : baseline.versionDescription;
        return registerNew(
                new Registration(
                        input.productId(),
                        input.newVersionId(),
                        input.newVersionName(),
                        baseline.versionType,
                        desc,
                        baseline.baselineVersionId,
                        baseline.baselineVersionName,
                        baseline.versionDesc,
                        baseline.approver,
                        baseline.isHidden,
                        sv,
                        baseline.introducedProductId,
                        baseline.ownerList,
                        1,
                        baseline.creatorId,
                        baseline.creatorId,
                        input.now()));
    }

    /**
     * 重命名版本名称。
     *
     * @param newVersionName 新版本名称
     */
    public void rename(String newVersionName) {
        if (StringUtils.isBlank(newVersionName)) {
            throw new DomainRuleException("版本名称不能为空");
        }
        this.versionName = newVersionName.trim();
    }

    /**
     * 更新可编辑属性字段（局部更新）。
     *
     * @param patch 属性局部变更，各字段与 {@link AttributePatch} 记录组件一一对应
     */
    public void applyAttributePatch(AttributePatch patch) {
        if (patch == null) {
            return;
        }
        if (StringUtils.isNotBlank(patch.supportedVersion())) {
            this.supportedVersion = patch.supportedVersion();
        }
        if (patch.versionDescription() != null) {
            this.versionDescription = patch.versionDescription();
        }
        if (patch.versionDesc() != null) {
            this.versionDesc = patch.versionDesc();
        }
        if (patch.versionStatus() != null) {
            this.versionStatus = patch.versionStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将版本置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.versionStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 判断版本是否归属指定产品。
     *
     * @param productId 产品 ID
     * @return 是否归属该产品
     */
    public boolean belongsToProduct(String productId) {
        return Objects.equals(this.ownedProductId, productId);
    }

    /**
     * 获取归属产品 ID。
     *
     * @return 归属产品 ID
     */
    public String getOwnedProductId() {
        return ownedProductId;
    }

    /**
     * 获取版本 ID。
     *
     * @return 版本 ID
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * 获取版本名称。
     *
     * @return 版本名称
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * 获取版本类型。
     *
     * @return 版本类型
     */
    public String getVersionType() {
        return versionType;
    }

    /**
     * 获取版本描述。
     *
     * @return 版本描述
     */
    public String getVersionDescription() {
        return versionDescription;
    }

    /**
     * 获取基线版本 ID。
     *
     * @return 基线版本 ID
     */
    public String getBaselineVersionId() {
        return baselineVersionId;
    }

    /**
     * 获取基线版本名称。
     *
     * @return 基线版本名称
     */
    public String getBaselineVersionName() {
        return baselineVersionName;
    }

    /**
     * 获取版本描述补充。
     *
     * @return 版本描述补充
     */
    public String getVersionDesc() {
        return versionDesc;
    }

    /**
     * 获取审批人。
     *
     * @return 审批人
     */
    public String getApprover() {
        return approver;
    }

    /**
     * 获取是否隐藏标记。
     *
     * @return 是否隐藏标记
     */
    public String getIsHidden() {
        return isHidden;
    }

    /**
     * 获取支持版本。
     *
     * @return 支持版本
     */
    public String getSupportedVersion() {
        return supportedVersion;
    }

    /**
     * 获取版本状态。
     *
     * @return 版本状态
     */
    public int getVersionStatus() {
        return versionStatus;
    }

    /**
     * 获取引入产品 ID。
     *
     * @return 引入产品 ID
     */
    public String getIntroducedProductId() {
        return introducedProductId;
    }

    /**
     * 获取责任人列表。
     *
     * @return 责任人列表
     */
    public String getOwnerList() {
        return ownerList;
    }

    /**
     * 获取创建人 ID。
     *
     * @return 创建人 ID
     */
    public String getCreatorId() {
        return creatorId;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * 获取更新人 ID。
     *
     * @return 更新人 ID
     */
    public String getUpdaterId() {
        return updaterId;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }
}
