package com.example.bookingservice.client;

import com.example.bookingservice.dto.AvailabilityRequest;
import com.example.bookingservice.dto.RoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/rooms/recommend")
    List<RoomDto> getRecommendedRooms();

    @PostMapping("/api/rooms/{id}/confirm-availability")
    Boolean confirmAvailability(@PathVariable Long id, @RequestBody AvailabilityRequest request);

    @PostMapping("/api/rooms/{id}/release")
    void releaseRoom(@PathVariable Long id);
}