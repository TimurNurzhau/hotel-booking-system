package com.example.hotelservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фильтр, который защищает внутренние эндпоинты hotel-service
 * (вызываемые только из booking-service через Feign) с помощью кастомного заголовка X-Internal-Key.
 *
 * Работает ДО JwtAuthenticationFilter: если запрос идёт на внутренний путь и ключ неверный —
 * возвращаем 401 и не пускаем дальше.
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api-key:super-secret-internal-key}")
    private String expectedApiKey;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Точные пути, которые считаются "внутренними".
     */
    private static final List<String> INTERNAL_EXACT_PATHS = List.of(
            "/api/rooms/recommend"
    );

    /**
     * Паттерны путей с ID, которые тоже считаются "внутренними".
     */
    private static final List<String> INTERNAL_PATTERNS = List.of(
            "/api/rooms/*/confirm-availability",
            "/api/rooms/*/release"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isInternalPath(path)) {
            String actualKey = request.getHeader("X-Internal-Key");
            if (actualKey == null || !actualKey.equals(expectedApiKey)) {
                log.warn("Blocked unauthorized internal call to {} from {}", path, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized internal call\"}");
                return;
            }
            log.debug("Authorized internal call to {}", path);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInternalPath(String path) {
        if (INTERNAL_EXACT_PATHS.contains(path)) {
            return true;
        }
        return INTERNAL_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}