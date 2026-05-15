/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口，负责组件扫描与启动。
 *
 * @since 2026-04-28
 */

@SpringBootApplication
@MapperScan("com.coretool.param.infrastructure.persistence.mapper")
public class ParamApplication {
    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ParamApplication.class, args);
    }
}
