package com.zpantry.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
