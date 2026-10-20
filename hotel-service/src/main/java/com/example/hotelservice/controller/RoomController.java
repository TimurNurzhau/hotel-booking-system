package com.example.hotelservice.controller;

import com.example.hotelservice.dto.AvailabilityRequest;
import com.example.hotelservice.dto.RoomDto;
import com.example.hotelservice.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@RequestParam Long hotelId, @RequestBody RoomDto roomDto) {
        return new ResponseEntity<>(roomService.createRoom(hotelId, roomDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllAvailableRooms() {
        return ResponseEntity.ok(roomService.getAllAvailableRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms() {
        return ResponseEntity.ok(roomService.getRecommendedRooms());
    }

    @GetMapping("/stats")
    public ResponseEntity<List<RoomDto>> getRoomStats() {
        return ResponseEntity.ok(roomService.getRoomStats());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable Long id, @RequestBody RoomDto roomDto) {
        return ResponseEntity.ok(roomService.updateRoom(id, roomDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-availability")
    public ResponseEntity<Boolean> confirmAvailability(@PathVariable Long id, @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(roomService.confirmAvailability(id, request));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseRoom(@PathVariable Long id, @RequestParam(required = false) String requestId) {
        roomService.releaseRoom(id, requestId);
        return ResponseEntity.ok().build();
    }
}