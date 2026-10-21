package com.example.bookingservice.service;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.RoomDto;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.ServiceUnavailableException;
import com.example.bookingservice.mapper.BookingMapper;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceUnitTest {

    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private HotelServiceClient hotelServiceClient;
    private BookingMapper bookingMapper;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        hotelServiceClient = mock(HotelServiceClient.class);
        bookingMapper = mock(BookingMapper.class);

        bookingService = new BookingService(
                bookingRepository, userRepository, hotelServiceClient, bookingMapper);
    }

    @Test
    void createBooking_whenDbFailsAfterLock_shouldCompensate() {
        User user = new User();
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(hotelServiceClient.getRecommendedRooms())
                .thenReturn(List.of(new RoomDto(1L, 1L, "101", true, 0)));
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class)))
                .thenReturn(true);

        // Первый save (PENDING) — успешно, второй (CONFIRMED) — падает
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking b = invocation.getArgument(0);
                    if (b.getStatus() == Booking.BookingStatus.CONFIRMED) {
                        throw new RuntimeException("DB error");
                    }
                    b.setId(1L);
                    return b;
                });

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(true);
        request.setRequestId("req-compensate-1");

        assertThrows(ServiceUnavailableException.class,
                () -> bookingService.createBooking(request, "testuser"));

        // Компенсация должна быть вызвана, так как блокировка была установлена
        verify(hotelServiceClient, times(1)).releaseRoom(eq(1L), eq("req-compensate-1"));
    }
}