package org.example.mavendemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@SpringBootApplication
public class    MavenDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MavenDemoApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Test Framework API")
                        .version("1.0")
                        .description("API Documentation for managing TestCases and TestResults"));
    }
}
