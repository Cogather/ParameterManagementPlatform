package com.coretool.param.ui.vo;

import lombok.Data;

import java.util.List;

/** 参数页左树：命令 → 类型（spec-03 §1.2）。 */
@Data
public class ParameterCommandTreeNode {

    private String commandId;
    private String commandName;
    private List<ParameterTypeNode> types;

    @Data
    public static class ParameterTypeNode {
        private String code;
        private String name;

        /**
         * 创建空类型节点。
         */
        public ParameterTypeNode() {}

        /**
         * 创建类型节点。
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
