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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class RoomServiceConcurrencyTest {

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
    void concurrentConfirmAvailability_shouldOnlySucceedOnce() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                latch.await();
                AvailabilityRequest req = new AvailabilityRequest();
                req.setStartDate(LocalDate.now().plusDays(1));
                req.setEndDate(LocalDate.now().plusDays(3));
                req.setRequestId("req-" + Thread.currentThread().getId());

                if (roomService.confirmAvailability(roomId, req)) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                // Ожидаем DataIntegrityViolationException или возврат false
            }
        };

        executor.submit(task);
        executor.submit(task);

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Only one concurrent booking should succeed");
    }
}