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
                .map(r -> new RoomDto(r.getId(), r.getHotel().getId(), r.getNumber(), r.getAvailable(), r.getTimesBooked()))
                .collect(Collectors.toList());
    }

    public List<RoomDto> getRecommendedRooms() {
        return roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc().stream()
                .map(r -> new RoomDto(r.getId(), r.getHotel().getId(), r.getNumber(), r.getAvailable(), r.getTimesBooked()))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, AvailabilityRequest request) {
        // Пессимистичная блокировка: второй параллельный запрос ждёт, пока первый не завершит транзакцию
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
}