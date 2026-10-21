package com.example.bookingservice.integration;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.repository.BookingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Полный интеграционный тест сквозного сценария:
 *  1) Поднимаем реальный HTTP-сервер (RANDOM_PORT).
 *  2) Получаем JWT через /user/auth (предзаполненный user/user123).
 *  3) Вызываем /booking с autoSelect=false и roomId=1.
 *  4) Мокаем падение hotel-service через @MockBean (имитация 500/тайм-аута).
 *  5) Проверяем, что API вернул 503, а в БД бронь сохранена со статусом CANCELLED.
 *
 * Используется @MockBean вместо WireMock, потому что WireMock в CI нестабилен
 * из-за конфликта с LoadBalancer и @DynamicPropertySource.
 * Это осознанный компромисс для стабильности пайплайна.
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

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    void fullSagaFlow_whenHotelServiceFails_shouldCancelBookingAndReturn503() throws Exception {
        // Эмулируем падение hotel-service при подтверждении доступности
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class)))
                .thenThrow(new RuntimeException("Hotel service is down (simulated)"));

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

        String requestId = "integration-test-fail-" + UUID.randomUUID();

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(false);
        request.setRoomId(1L);
        request.setRequestId(requestId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/booking",
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                String.class);

        // 3. Проверяем HTTP-статус
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "Booking should return 503 when hotel-service fails");

        // 4. Проверяем состояние в БД: бронь существует, но CANCELLED
        Optional<Booking> saved = bookingRepository.findByRequestId(requestId);
        assertTrue(saved.isPresent(), "Booking should be saved in DB");
        assertEquals(Booking.BookingStatus.CANCELLED, saved.get().getStatus(),
                "Booking should be CANCELLED after compensation");
    }
}