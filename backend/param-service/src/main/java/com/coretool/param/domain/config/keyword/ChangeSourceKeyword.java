package com.coretool.param.domain.config.keyword;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 变更来源关键字（config_change_source_keyword），黑名单用正则必须可编译。
 */
public class ChangeSourceKeyword {

    public record Registration(
            String ownedProductId,
            String keywordId,
            String keywordRegex,
            String reason,
            Integer keywordStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String keywordId,
            String keywordRegex,
            String reason,
            Integer keywordStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record RegexAndMetaPatch(
            String newRegex,
            String description,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String keywordId;
    private String keywordRegex;
    private String reason;
    private int keywordStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 校验关键字正则是否可编译。
     *
     * @param keywordRegex 关键字正则
     */
    public static void assertRegexCompilable(String keywordRegex) {
        if (StringUtils.isBlank(keywordRegex)) {
            throw new DomainRuleException("关键字正则不能为空");
        }
        try {
            Pattern.compile(keywordRegex);
        } catch (PatternSyntaxException e) {
            String desc = e.getDescription();
            if (StringUtils.isBlank(desc)) {
                throw new DomainRuleException("KEYWORD_REGEX_INVALID: 正则表达式非法");
            }
            throw new DomainRuleException("KEYWORD_REGEX_INVALID: 正则表达式非法: " + desc);
        }
    }

    /**
     * 注册新的变更来源关键字（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新关键字
     */
    public static ChangeSourceKeyword registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isBlank(input.keywordId())) {
            throw new DomainRuleException("关键字ID不能为空");
        }
        if (StringUtils.isBlank(input.reason())) {
            throw new DomainRuleException("原因不能为空");
        }
        assertRegexCompilable(input.keywordRegex());
        ChangeSourceKeyword k = new ChangeSourceKeyword();
        k.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        k.keywordId = input.keywordId().trim();
        k.keywordRegex = input.keywordRegex().trim();
        k.reason = input.reason();
        k.keywordStatus = input.keywordStatus() == null ? 1 : input.keywordStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        k.creatorId = c;
        k.creationTimestamp = input.now();
        k.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        k.updateTimestamp = input.now();
        return k;
    }

    /**
     * 从持久化数据重建变更来源关键字（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的关键字
     */
    public static ChangeSourceKeyword rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        ChangeSourceKeyword k = new ChangeSourceKeyword();
        k.ownedProductId = input.ownedProductId();
        k.keywordId = input.keywordId();
        k.keywordRegex = input.keywordRegex();
        k.reason = input.reason();
        k.keywordStatus = input.keywordStatus() == null ? 1 : input.keywordStatus();
        k.creatorId = input.creatorId();
        k.creationTimestamp = input.creationTimestamp();
        k.updaterId = input.updaterId();
        k.updateTimestamp = input.updateTimestamp();
        return k;
    }

    /**
     * 替换关键字正则与其它可编辑元数据（调用方传入 newRegex 时会执行严格正则校验）。
     *
     * @param patch 可编辑变更，各字段与 {@link RegexAndMetaPatch} 记录组件一一对应
     */
    public void replaceRegexAndMeta(RegexAndMetaPatch patch) {
        if (patch == null) {
            return;
        }
        // 只要调用方传入了 newRegex（哪怕是空串），就视为用户要修改该字段并执行严格校验。
        if (patch.newRegex() != null) {
            assertRegexCompilable(patch.newRegex());
            this.keywordRegex = patch.newRegex().trim();
        }
        if (patch.description() != null) {
            this.reason = patch.description();
        }
        if (patch.status() != null) {
            this.keywordStatus = patch.status();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将关键字置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.keywordStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 判断关键字是否归属指定产品。
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
     * 获取关键字 ID。
     *
     * @return 关键字 ID
     */
    public String getKeywordId() {
        return keywordId;
    }

    /**
     * 获取关键字正则。
     *
     * @return 关键字正则
     */
    public String getKeywordRegex() {
        return keywordRegex;
    }

    /**
     * 获取原因/说明。
     *
     * @return 原因/说明
     */
    public String getReason() {
        return reason;
    }

    /**
     * 获取关键字状态。
     *
     * @return 状态
     */
    public int getKeywordStatus() {
        return keywordStatus;
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
