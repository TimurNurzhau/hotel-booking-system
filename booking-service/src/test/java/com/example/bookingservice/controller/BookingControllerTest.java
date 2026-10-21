package com.example.bookingservice.controller;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.BookingRequest;
import com.example.bookingservice.dto.RoomDto;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("USER");
        userRepository.save(user);

        RoomDto room = new RoomDto(1L, 1L, "101", true, 0);
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(List.of(room));
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class))).thenReturn(true);
        doNothing().when(hotelServiceClient).releaseRoom(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_shouldReturn201AndConfirmed() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(true);
        request.setRequestId("req-test-1");

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_invalidDates_shouldReturn400() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 5));
        request.setEndDate(LocalDate.of(2026, 12, 1));
        request.setAutoSelect(true);

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getUserBookings_shouldReturn200AndPage() throws Exception {
        mockMvc.perform(get("/bookings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void createBooking_withoutAuth_shouldReturn401() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(true);

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_whenHotelServiceFails_shouldCancelAndReturn503() throws Exception {
        // Падение Feign-клиента при подтверждении. Блокировка не устанавливается,
        // поэтому компенсация (releaseRoom) не требуется по логике саги.
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class)))
                .thenThrow(new RuntimeException("Hotel service is down"));

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(false);
        request.setRoomId(1L);
        request.setRequestId("req-fail-1");

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());

        Optional<Booking> cancelledBooking = bookingRepository.findByRequestId("req-fail-1");
        assertTrue(cancelledBooking.isPresent());
        assertEquals(Booking.BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_whenHotelServiceTimesOut_shouldCancelAndReturn503() throws Exception {
        // Тайм-аут. Блокировка не устанавливается, компенсация не нужна.
        when(hotelServiceClient.confirmAvailability(anyLong(), any(AvailabilityRequest.class)))
                .thenThrow(new RuntimeException("Read timed out"));

        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(false);
        request.setRoomId(1L);
        request.setRequestId("req-timeout-1");

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());

        Optional<Booking> cancelledBooking = bookingRepository.findByRequestId("req-timeout-1");
        assertTrue(cancelledBooking.isPresent());
        assertEquals(Booking.BookingStatus.CANCELLED, cancelledBooking.get().getStatus());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createBooking_withSameRequestId_shouldReturnExisting() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 12, 5));
        request.setAutoSelect(true);
        request.setRequestId("req-idempotent-1");

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals(1, bookingRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void cancelBooking_shouldReturn200AndReleaseRoom() throws Exception {
        User user = userRepository.findByUsername("testuser").orElseThrow();
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(1L);
        booking.setStartDate(LocalDate.of(2026, 12, 1));
        booking.setEndDate(LocalDate.of(2026, 12, 5));
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCreatedAt(java.time.LocalDateTime.now());
        booking.setRequestId("req-cancel-1");
        booking = bookingRepository.save(booking);

        doNothing().when(hotelServiceClient).releaseRoom(anyLong(), anyString());

        mockMvc.perform(delete("/booking/" + booking.getId()))
                .andExpect(status().isOk());

        Optional<Booking> cancelled = bookingRepository.findById(booking.getId());
        assertTrue(cancelled.isPresent());
        assertEquals(Booking.BookingStatus.CANCELLED, cancelled.get().getStatus());

        verify(hotelServiceClient, times(1)).releaseRoom(eq(1L), eq("req-cancel-1"));
    }
}