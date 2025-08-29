package org.example.mavendemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;

@SpringBootApplication
public class MavenDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MavenDemoApplication.class, args);
        System.out.println("Application started successfully.");
        System.out.println("Access the API documentation at: http://localhost:8080/ui/");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Test Framework API")
                        .version("1.0")
                        .description("Comprehensive API Documentation for managing TestCases and TestResults. " +
                                "This API provides full CRUD operations for test case management and execution tracking.")
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production Server")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Test Cases")
                                .description("Operations related to test case management"),
                        new Tag()
                                .name("Test Results")
                                .description("Operations related to test result tracking"),
                        new Tag()
                                .name("Health Check")
                                .description("Application health and status endpoints")
                ));
    }
}
