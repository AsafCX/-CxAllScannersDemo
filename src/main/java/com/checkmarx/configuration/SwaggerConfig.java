package com.checkmarx.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "CX Integrations Repos Manager REST API",
                "CX Integrations Repos Manager REST API.",
                "API TOS",
                "Terms of service",
                new Contact("Checkmarx", "www.checkmarx.com", "repos-manager@checkmarx.com"),
                "", "", Collections.emptyList());
    }
}
