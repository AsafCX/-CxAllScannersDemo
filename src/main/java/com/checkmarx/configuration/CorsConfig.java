package com.checkmarx.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${front.end.domain}")
    private String frontEndDomain;

    @Value("${front.end.port}")
    private String frontEndPort;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://" + frontEndDomain +":" + frontEndPort)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
        ;
    }
}