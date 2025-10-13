package com.example.automatedtestingframework;

import org.springframework.boot.SpringApplication;
import com.example.automatedtestingframework.config.ClerkProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableConfigurationProperties(ClerkProperties.class)
public class AutomatedTestingFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomatedTestingFrameworkApplication.class, args);
    }
}
