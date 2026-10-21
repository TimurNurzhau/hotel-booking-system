package com.example.bookingservice.integration;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.exception.ServiceUnavailableException;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест сквозного сценария саги:
 *  1) Поднимаем Spring-контекст с реальной БД H2.
 *  2) Мокаем HotelServiceClient, чтобы эмулировать падение удалённого сервиса.
 *  3) Вызываем BookingService.createBooking напрямую (без HTTP).
 *  4) Проверяем, что сага перевела бронь в CANCELLED и выбросила ServiceUnavailableException.
 *
 * Такой подход стабилен в CI и не зависит от WireMock, TestRestTemplate и порядка
 * запуска тестов. HTTP-слой уже покрыт в BookingControllerTest (MockMvc).
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    void fullSagaFlow_whenHotelServiceFails_shouldCancelBooking() {
        // Эмулируем падение hotel-service при подтверждении доступности
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class)))
                .thenThrow(new RuntimeException("Hotel service is down (simulated)"));

        String requestId = "integration-test-fail-" + UUID.randomUUID();

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(false);
        request.setRoomId(1L);
        request.setRequestId(requestId);

        // Вызываем сервис напрямую (без HTTP) — сага должна упасть с ServiceUnavailableException
        assertThrows(ServiceUnavailableException.class,
                () -> bookingService.createBooking(request, "user"));

        // Проверяем, что бронь сохранена в БД и переведена в CANCELLED
        Optional<Booking> saved = bookingRepository.findByRequestId(requestId);
        assertTrue(saved.isPresent(), "Booking should be saved in DB");
        assertEquals(Booking.BookingStatus.CANCELLED, saved.get().getStatus(),
                "Booking should be CANCELLED after compensation");
    }
}