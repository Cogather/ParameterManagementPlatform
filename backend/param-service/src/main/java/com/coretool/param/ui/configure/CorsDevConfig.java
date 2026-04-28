package com.coretool.param.ui.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 开发环境允许 Vite 直连后端（未走代理时）。
 */
@Configuration
@Profile("dev")
public class CorsDevConfig {

    /**
     * 开发环境 CORS 过滤器（允许本地 Vite 直连）。
     *
     * @return CORS Filter
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration c = new CorsConfiguration();
        c.addAllowedOriginPattern("http://localhost:*");
        c.addAllowedHeader(CorsConfiguration.ALL);
        c.addAllowedMethod(CorsConfiguration.ALL);
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return new CorsFilter(source);
    }
}
