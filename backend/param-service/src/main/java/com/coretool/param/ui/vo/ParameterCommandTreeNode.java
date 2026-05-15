/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

import java.util.List;

/**
 * 参数页左侧树节点：命令及其下属类型列表（spec-03 §1.2）。
 *
 * @since 2026-04-28
 */
@Data
public class ParameterCommandTreeNode {

    private String commandId;
    private String commandName;
    private List<ParameterTypeNode> types;

    /**
     * 树中「类型」子节点（编码与展示名称）。
     *
     * @since 2026-04-28
     */
    @Data
    public static class ParameterTypeNode {
        private String code;
        private String name;

        /**
         * 无参构造（供序列化框架 / Lombok 使用）。
         */
        public ParameterTypeNode() {}

        /**
         * 使用编码与名称创建类型节点。
         *
         * @param code 类型编码
         * @param name 类型名称
         */
        public ParameterTypeNode(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
