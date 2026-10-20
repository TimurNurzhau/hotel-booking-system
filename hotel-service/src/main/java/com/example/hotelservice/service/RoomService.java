package com.example.hotelservice.service;

import com.example.hotelservice.dto.AvailabilityRequest;
import com.example.hotelservice.dto.RoomDto;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomDto createRoom(Long hotelId, RoomDto roomDto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + hotelId));

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(roomDto.getNumber());
        room.setAvailable(true);
        room.setTimesBooked(0);

        Room saved = roomRepository.save(room);
        return new RoomDto(saved.getId(), hotelId, saved.getNumber(), saved.getAvailable(), saved.getTimesBooked());
    }

    public List<RoomDto> getAllAvailableRooms() {
        return roomRepository.findByAvailableTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        return toDto(room);
    }

    public List<RoomDto> getRecommendedRooms() {
        return roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoomDto updateRoom(Long id, RoomDto roomDto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        room.setNumber(roomDto.getNumber());
        if (roomDto.getAvailable() != null) {
            room.setAvailable(roomDto.getAvailable());
        }
        Room updated = roomRepository.save(room);
        return toDto(updated);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, AvailabilityRequest request) {
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

        if (!room.getAvailable()) {
            return false;
        }

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);
        return true;
    }

    @Transactional
    public void releaseRoom(Long roomId) {
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
        if (room.getTimesBooked() > 0) {
            room.setTimesBooked(room.getTimesBooked() - 1);
            roomRepository.save(room);
        }
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getHotel().getId(), room.getNumber(), room.getAvailable(), room.getTimesBooked());
    }
}