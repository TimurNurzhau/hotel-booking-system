package com.example.hotelservice.service;

import com.example.hotelservice.dto.AvailabilityRequest;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomBookingRepository;
import com.example.hotelservice.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class RoomServiceTest {

    @Autowired private RoomService roomService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private HotelRepository hotelRepository;
    @Autowired private RoomBookingRepository roomBookingRepository;

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomBookingRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel = hotelRepository.save(hotel);

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber("T1");
        room.setAvailable(true);
        room.setTimesBooked(0);
        room = roomRepository.save(room);
        roomId = room.getId();
    }

    @Test
    void confirmAvailability_whenOverlapping_shouldReturnFalse() {
        // Первое бронирование (1-5 декабря)
        AvailabilityRequest req1 = new AvailabilityRequest();
        req1.setStartDate(LocalDate.of(2026, 12, 1));
        req1.setEndDate(LocalDate.of(2026, 12, 5));
        req1.setRequestId("req-overlap-1");
        assertTrue(roomService.confirmAvailability(roomId, req1));

        // Второе бронирование (3-7 декабря) — пересекается
        AvailabilityRequest req2 = new AvailabilityRequest();
        req2.setStartDate(LocalDate.of(2026, 12, 3));
        req2.setEndDate(LocalDate.of(2026, 12, 7));
        req2.setRequestId("req-overlap-2");

        // Должно вернуть false, так как даты пересекаются
        assertFalse(roomService.confirmAvailability(roomId, req2));
    }
}