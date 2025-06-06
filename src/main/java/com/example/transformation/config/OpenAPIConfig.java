package com.example.transformation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flat File Transformation API")
                        .version("1.0.0")
                        .description("API for transforming XML data into flat files based on JSON configuration.")
                        .termsOfService("http://swagger.io/terms/") // Example
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))); // Example
    }
}
