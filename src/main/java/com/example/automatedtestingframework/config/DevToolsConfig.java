package com.example.automatedtestingframework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * DevTools configuration for hot reload support.
 * This configuration ensures static resources are served without caching
 * during development.
 */
@Configuration
@Profile("!prod") // Only active in non-production environments
public class DevToolsConfig implements WebMvcConfigurer {

    /**
     * Configure resource handlers to disable caching for static resources.
     * This ensures that changes to CSS, JS, and images are immediately visible.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Disable caching for all static resources
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(0);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(0);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(0);
    }
}
