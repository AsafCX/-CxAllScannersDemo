package com.checkmarx.configuration;

import com.checkmarx.controller.exception.RestTemplateResponseErrorHandler;
import com.checkmarx.controller.interceptor.LoggingRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RestTemplateBuilder builder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingRequestInterceptor());
    }

    @Bean
    public RestTemplate restTemplate() {
        return builder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();
    }
}