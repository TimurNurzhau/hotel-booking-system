package com.example.bookingservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Micrometer Tracing сам прокидывает заголовки трассировки через Feign,
            // если добавлена зависимость feign-micrometer.
        };
    }
}