package com.example.bookingservice.integration;

import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.repository.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Полный интеграционный тест сквозного сценария:
 *  1) Поднимаем реальный HTTP-сервер (RANDOM_PORT).
 *  2) Получаем JWT через /user/auth (предзаполненный user/user123).
 *  3) Вызываем /booking с autoSelect=false и roomId=1.
 *  4) WireMock эмулирует падение hotel-service на /confirm-availability (500).
 *  5) Проверяем, что API вернул 503, а в БД бронь сохранена со статусом CANCELLED.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @DynamicPropertySource
    static void overrideFeignUrl(DynamicPropertyRegistry registry) {
        // Переопределяем URL для Feign-клиента hotel-service, чтобы он шёл в WireMock
        registry.add("spring.cloud.openfeign.client.config.hotel-service.url",
                () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        bookingRepository.deleteAll();
    }

    @Test
    void fullSagaFlow_whenHotelServiceFails_shouldCancelBookingAndReturn503() throws Exception {
        // Эмулируем падение hotel-service
        wireMock.stubFor(post(urlPathMatching("/api/rooms/.*/confirm-availability"))
                .willReturn(aResponse().withStatus(500)));

        // 1. Авторизуемся как предзаполненный пользователь user/user123
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        String authBody = "{\"username\":\"user\",\"password\":\"user123\"}";
        ResponseEntity<String> authResp = restTemplate.postForEntity(
                "/user/auth", new HttpEntity<>(authBody, authHeaders), String.class);

        assertEquals(HttpStatus.OK, authResp.getStatusCode(), "Auth should succeed");
        String token = objectMapper.readTree(authResp.getBody()).get("token").asText();

        // 2. Формируем запрос на бронирование с JWT
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(false);
        request.setRoomId(1L);
        request.setRequestId("integration-test-fail-1");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/booking",
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class);

        // 3. Проверяем HTTP-статус
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "Booking should return 503 when hotel-service fails");

        // 4. Проверяем состояние в БД: бронь существует, но CANCELLED
        Optional<Booking> saved = bookingRepository.findByRequestId("integration-test-fail-1");
        assertTrue(saved.isPresent(), "Booking should be saved in DB");
        assertEquals(Booking.BookingStatus.CANCELLED, saved.get().getStatus(),
                "Booking should be CANCELLED after compensation");
    }
}