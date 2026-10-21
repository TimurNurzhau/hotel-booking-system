package com.example.bookingservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Feign: добавляет заголовок X-Internal-Key ко всем исходящим запросам,
 * чтобы hotel-service мог отличить внутренний вызов от внешнего.
 */
@Configuration
public class FeignConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalKeyInterceptor() {
        return template -> template.header("X-Internal-Key", internalApiKey);
    }
}