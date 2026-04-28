package com.coretool.param.domain.command.service;

import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * 命令类型定义领域服务：封装跨实体/仓储的规则校验（唯一性、归属等）。
 *
 * <p>说明：应用层只做用例编排与事务边界；领域规则集中在 domain。
 */
public class CommandTypeDefinitionDomainService {

    public record CreateCommand(
            String productId,
            String ownedCommandId,
            String commandTypeId,
            String commandTypeName,
            String commandTypeEnum,
            Integer minValue,
            Integer maxValue,
            String occupiedSerialNumber,
            Integer commandTypeStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String commandTypeId,
            String commandTypeName,
            String commandTypeEnum,
            Integer minValue,
            Integer maxValue,
            String occupiedSerialNumber,
            Integer commandTypeStatus,
            String updaterId,
            LocalDateTime now) {}

    private final CommandTypeDefinitionRepository repository;

    /**
     * 创建命令类型定义领域服务。
     *
     * @param repository 命令类型定义仓储
     */
    public CommandTypeDefinitionDomainService(CommandTypeDefinitionRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新的命令类型定义（含类型 ID 冲突与同产品下名称唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新命令类型定义
     */
    public CommandTypeDefinition createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (StringUtils.isBlank(input.commandTypeId())) {
            throw new DomainRuleException("类型ID不能为空");
        }
        if (repository.findById(input.commandTypeId()).isPresent()) {
            throw new DomainRuleException("TYPE_ID_CONFLICT: 类型ID已存在");
        }
        if (repository.existsSameNameInProduct(input.productId(), input.commandTypeName(), null)) {
            throw new DomainRuleException("TYPE_NAME_DUPLICATE: 类型名称已存在");
        }
        return CommandTypeDefinition.registerNew(
                new CommandTypeDefinition.Registration(
                        input.productId(),
                        input.ownedCommandId(),
                        input.commandTypeId(),
                        input.commandTypeName(),
                        input.commandTypeEnum(),
                        input.minValue(),
                        input.maxValue(),
                        input.occupiedSerialNumber(),
                        input.commandTypeStatus(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有命令类型定义（含归属校验与同产品下名称唯一性校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的命令类型定义
     */
    public CommandTypeDefinition updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        CommandTypeDefinition existing =
                repository.findById(input.commandTypeId()).orElseThrow(() -> new DomainRuleException("类型不存在"));
        if (!existing.belongsToProduct(input.productId())) {
            throw new DomainRuleException("类型不属于该产品");
        }
        if (StringUtils.isNotBlank(input.commandTypeName())
                && !StringUtils.equals(input.commandTypeName(), existing.getCommandTypeName())) {
            if (repository.existsSameNameInProduct(
                    input.productId(), input.commandTypeName(), input.commandTypeId())) {
                throw new DomainRuleException("TYPE_NAME_DUPLICATE: 类型名称已存在");
            }
        }
        existing.applyEditablePatch(
                new CommandTypeDefinition.EditablePatch(
                        input.commandTypeName(),
                        input.commandTypeEnum(),
                        input.minValue(),
                        input.maxValue(),
                        input.occupiedSerialNumber(),
                        input.commandTypeStatus(),
                        input.updaterId(),
                        input.now()));
        return existing;
    }

    /**
     * 获取并校验命令类型定义归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId     产品 ID
     * @param commandTypeId 类型 ID
     * @return 命令类型定义
     */
    public CommandTypeDefinition requireOwned(String productId, String commandTypeId) {
        return repository
                .findById(commandTypeId)
                .filter(t -> t.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("类型不存在或不属于该产品"));
    }
}

