package com.example.automatedtestingframework;

import org.springframework.boot.SpringApplication;
import com.example.automatedtestingframework.config.ClerkProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;


@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableConfigurationProperties(ClerkProperties.class)
public class AutomatedTestingFrameworkApplication {

    public static void main(String[] args) {
        WebServerApplicationContext context =
                (WebServerApplicationContext) SpringApplication.run(AutomatedTestingFrameworkApplication.class, args);

        int port = context.getWebServer().getPort();
        System.out.println("------------------------------------------------");
        System.out.println("✅ Application started successfully!");
        System.out.println("🌐 Running on: http://localhost:" + port);
        System.out.println("------------------------------------------------");
    }
}
