package com.example.bookingservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Берем TraceId из MDC и прокидываем в Hotel Service
            String correlationId = MDC.get(TraceIdFilter.CORRELATION_ID_LOG_VAR);
            if (correlationId != null) {
                template.header("X-Correlation-Id", correlationId);
            }
        };
    }
}