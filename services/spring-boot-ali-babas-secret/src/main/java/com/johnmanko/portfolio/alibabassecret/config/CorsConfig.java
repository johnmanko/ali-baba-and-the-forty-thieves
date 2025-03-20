package com.johnmanko.portfolio.alibabassecret.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Allow localhost origins
 * References:
 * https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
 */
@Configuration
@ConditionalOnProperty(
        value = "app.config.server.cors.enabled",
        havingValue = "false"
)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource cors = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("http://127.0.0.1:8080");
        config.addAllowedOrigin("http://127.0.0.1:4200");
        config.setAllowCredentials(true);
        cors.registerCorsConfiguration("/**", config);
        return cors;
    }

}
