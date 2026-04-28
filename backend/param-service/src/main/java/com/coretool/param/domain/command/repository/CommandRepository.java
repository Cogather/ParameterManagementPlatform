package com.coretool.param.domain.command.repository;

import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface CommandRepository {

    Optional<Command> findById(String commandId);

    boolean existsSameNameInProduct(String productId, String commandName, String excludeCommandId);

    /** 同一产品下按名称查找“已删除/未启用”(status=0) 的命令，用于新增时自动恢复。 */
    Optional<Command> findDisabledByNameInProduct(String productId, String commandName);

    void insert(Command command);

    void update(Command command);

    PageSlice<Command> pageByProduct(String productId, int page, int size, String keyword);
}

