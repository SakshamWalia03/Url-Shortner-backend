package com.url.shortener.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class to enable Cross-Origin Resource Sharing (CORS)
 * for the application, allowing requests from the frontend.
 */
@Configuration
public class WebConfig {

    /**
     * Frontend URL allowed to make requests (from application.properties).
     */
    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Configures CORS mappings for all endpoints.
     *
     * @return WebMvcConfigurer with CORS configuration.
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Allow CORS requests from frontendUrl for all paths
                registry.addMapping("/**")
                        .allowedOrigins(frontendUrl)         // Allow frontend URL only
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Explicit allowed methods
                        .allowCredentials(true)              // Allow cookies/auth headers
                        .maxAge(3600);                       // Cache preflight response for 1 hour
            }
        };
    }
}