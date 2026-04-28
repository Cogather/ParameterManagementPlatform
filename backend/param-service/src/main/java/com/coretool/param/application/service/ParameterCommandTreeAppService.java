package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.ui.vo.ParameterCommandTreeNode;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 参数页左树数据（spec-03 §1.2）。无命令主数据时提供演示命令 + 固定类型（与 spec-01 类型编码对齐）。
 */
@Service
public class ParameterCommandTreeAppService {

    private final EntityCommandMappingMapper commandMapper;
    private final CommandTypeDefinitionMapper typeMapper;

    public ParameterCommandTreeAppService(
            EntityCommandMappingMapper commandMapper, CommandTypeDefinitionMapper typeMapper) {
        this.commandMapper = commandMapper;
        this.typeMapper = typeMapper;
    }

    /**
     * 获取参数页左侧命令-类型树数据。
     *
     * @param productId 产品 ID
     * @return 树节点列表
     */
    public List<ParameterCommandTreeNode> treeForProduct(String productId) {
        if (StringUtils.isBlank(productId)) {
            return demo();
        }

        List<EntityCommandMappingPo> commands =
                commandMapper.selectList(
                        new LambdaQueryWrapper<EntityCommandMappingPo>()
                                .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                                .ne(EntityCommandMappingPo::getCommandStatus, 0)
                                .orderByAsc(EntityCommandMappingPo::getCommandName));
        if (commands == null || commands.isEmpty()) {
            return demo();
        }

        List<CommandTypeDefinitionPo> types =
                typeMapper.selectList(
                        new LambdaQueryWrapper<CommandTypeDefinitionPo>()
                                .eq(CommandTypeDefinitionPo::getOwnedProductId, productId)
                                .ne(CommandTypeDefinitionPo::getCommandTypeStatus, 0));

        Map<String, Map<String, ParameterCommandTreeNode.ParameterTypeNode>> byCommand =
                new LinkedHashMap<>();
        if (types != null) {
            for (CommandTypeDefinitionPo t : types) {
                String ownedCmd = StringUtils.defaultString(t.getOwnedCommandId()).trim();
                if (ownedCmd.isEmpty()) {
                    continue;
                }
                String codeRaw = StringUtils.defaultString(t.getCommandType()).trim();
                if (codeRaw.isEmpty()) {
                    continue;
                }
                String code = codeRaw.toUpperCase(Locale.ROOT);
                String name = StringUtils.defaultIfBlank(t.getCommandTypeName(), code).trim();
                ParameterCommandTreeNode.ParameterTypeNode node = new ParameterCommandTreeNode.ParameterTypeNode(code, name);
                byCommand.computeIfAbsent(ownedCmd, k -> new LinkedHashMap<>()).putIfAbsent(code, node);
            }
        }

        List<ParameterCommandTreeNode> out = new ArrayList<>();
        for (EntityCommandMappingPo c : commands) {
            ParameterCommandTreeNode n = new ParameterCommandTreeNode();
            n.setCommandId(StringUtils.defaultString(c.getCommandId()).trim());
            n.setCommandName(StringUtils.defaultIfBlank(c.getCommandName(), c.getCommandId()).trim());
            Map<String, ParameterCommandTreeNode.ParameterTypeNode> tm = byCommand.get(n.getCommandId());
            if (tm == null || tm.isEmpty()) {
                n.setTypes(List.of());
            } else {
                List<ParameterCommandTreeNode.ParameterTypeNode> list = new ArrayList<>(tm.values());
                list.sort(Comparator.comparing(ParameterCommandTreeNode.ParameterTypeNode::getCode));
                n.setTypes(list);
            }
            out.add(n);
        }
        return out;
    }

    private static List<ParameterCommandTreeNode> demo() {
        ParameterCommandTreeNode root = new ParameterCommandTreeNode();
        root.setCommandId("command_demo");
        root.setCommandName("演示命令");
        root.setTypes(
                List.of(
                        new ParameterCommandTreeNode.ParameterTypeNode("BIT", "BIT"),
                        new ParameterCommandTreeNode.ParameterTypeNode("BYTE", "BYTE"),
                        new ParameterCommandTreeNode.ParameterTypeNode("DWORD", "DWORD"),
                        new ParameterCommandTreeNode.ParameterTypeNode("STRING", "STRING")));
        return List.of(root);
    }
}
