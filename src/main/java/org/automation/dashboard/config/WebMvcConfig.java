package org.automation.dashboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC Configuration for serving static resources like screenshots and reports
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        logger.info("WebMvcConfig: Configuring static resource handlers");

        // Serve screenshots from artifacts/screenshots directory
        Path screenshotsPath = Paths.get("artifacts/screenshots").toAbsolutePath();
        if (!Files.exists(screenshotsPath)) {
            try {
                Files.createDirectories(screenshotsPath);
                logger.info("WebMvcConfig: Created screenshots directory at: {}", screenshotsPath);
            } catch (Exception e) {
                logger.error("WebMvcConfig: Error creating screenshots directory: {}", e.getMessage());
            }
        }

        registry.addResourceHandler("/screenshots/**")
                .addResourceLocations("file:" + screenshotsPath.toString() + "/")
                .setCachePeriod(3600);

        logger.info("WebMvcConfig: Registered /screenshots/** handler -> {}", screenshotsPath);

        // Serve reports from artifacts/reports directory
        Path reportsPath = Paths.get("artifacts/reports").toAbsolutePath();
        if (!Files.exists(reportsPath)) {
            try {
                Files.createDirectories(reportsPath);
                logger.info("WebMvcConfig: Created reports directory at: {}", reportsPath);
            } catch (Exception e) {
                logger.error("WebMvcConfig: Error creating reports directory: {}", e.getMessage());
            }
        }

        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:" + reportsPath.toString() + "/")
                .setCachePeriod(3600);

        logger.info("WebMvcConfig: Registered /reports/** handler -> {}", reportsPath);

        // Serve allure-report from allure-report directory
        Path allureReportPath = Paths.get("allure-report").toAbsolutePath();
        if (!Files.exists(allureReportPath)) {
            try {
                Files.createDirectories(allureReportPath);
                logger.info("WebMvcConfig: Created allure-report directory at: {}", allureReportPath);
            } catch (Exception e) {
                logger.error("WebMvcConfig: Error creating allure-report directory: {}", e.getMessage());
            }
        }

        registry.addResourceHandler("/allure-report/**")
                .addResourceLocations("file:" + allureReportPath.toString() + "/")
                .setCachePeriod(3600);

        logger.info("WebMvcConfig: Registered /allure-report/** handler -> {}", allureReportPath);

        // Serve allure results
        Path allurePath = Paths.get("allure-results").toAbsolutePath();
        if (Files.exists(allurePath)) {
            registry.addResourceHandler("/allure-results/**")
                    .addResourceLocations("file:" + allurePath.toString() + "/")
                    .setCachePeriod(3600);

            logger.info("WebMvcConfig: Registered /allure-results/** handler -> {}", allurePath);
        }

        logger.info("WebMvcConfig: Static resource handlers configured successfully");
    }
}
