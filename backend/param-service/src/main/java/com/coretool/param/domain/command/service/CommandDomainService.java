package com.coretool.param.domain.command.service;

import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.command.repository.CommandRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 命令领域服务：封装唯一性、归属校验、状态变更等规则。 */
public class CommandDomainService {

    public record CreateCommand(
            String productId,
            String commandId,
            String commandName,
            String ownerList,
            Integer commandStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String commandId,
            String commandName,
            String ownerList,
            Integer commandStatus,
            String updaterId,
            LocalDateTime now) {}

    private final CommandRepository repository;

    /**
     * 创建命令领域服务。
     *
     * @param repository 命令仓储
     */
    public CommandDomainService(CommandRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新命令（含同产品下命令名称唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新命令
     */
    public Command createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (repository.existsSameNameInProduct(input.productId(), input.commandName(), null)) {
            throw new DomainRuleException("CMD_DUPLICATE_NAME: 同一产品下命令名称已存在");
        }
        return Command.registerNew(
                new Command.Registration(
                        input.productId(),
                        input.commandId(),
                        input.commandName(),
                        input.creatorId(),
                        input.updaterId(),
                        input.ownerList(),
                        input.commandStatus(),
                        input.now()));
    }

    /**
     * 更新既有命令（含归属校验与名称唯一性校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的命令
     */
    public Command updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        Command existing = requireOwned(input.productId(), input.commandId());
        if (StringUtils.isNotBlank(input.commandName())
                && !StringUtils.equals(input.commandName(), existing.getCommandName())) {
            if (repository.existsSameNameInProduct(input.productId(), input.commandName(), input.commandId())) {
                throw new DomainRuleException("CMD_DUPLICATE_NAME: 同一产品下命令名称已存在");
            }
        }
        existing.applyEditablePatch(
                new Command.EditablePatch(
                        input.commandName(),
                        input.ownerList(),
                        input.commandStatus(),
                        input.updaterId(),
                        input.now()));
        return existing;
    }

    /**
     * 禁用命令（含归属校验）。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @param now       当前时间
     * @return 禁用后的命令
     */
    public Command disable(String productId, String commandId, LocalDateTime now) {
        Command existing = requireOwned(productId, commandId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验命令归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @return 命令
     */
    public Command requireOwned(String productId, String commandId) {
        return repository
                .findById(commandId)
                .filter(c -> c.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("命令不存在或不属于该产品"));
    }
}

