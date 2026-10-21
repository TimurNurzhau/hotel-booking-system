package com.example.bookingservice.client;

import com.example.bookingservice.config.FeignConfig;
import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.RoomDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hotel-service", configuration = FeignConfig.class)
public interface HotelServiceClient {

    @GetMapping("/api/rooms/recommend")
    @Retry(name = "hotelService")
    @CircuitBreaker(name = "hotelService")
    List<RoomDto> getRecommendedRooms();

    @PostMapping("/api/rooms/{id}/confirm-availability")
    @Retry(name = "hotelService")
    @CircuitBreaker(name = "hotelService")
    Boolean confirmAvailability(@PathVariable Long id, @RequestBody AvailabilityRequest request);

    // ВАЖНО: Убраны @Retry и @CircuitBreaker.
    // Компенсация должна выполняться даже если основная цепь разомкнута.
    @PostMapping("/api/rooms/{id}/release")
    void releaseRoom(@PathVariable Long id, @RequestParam(required = false) String requestId);
}