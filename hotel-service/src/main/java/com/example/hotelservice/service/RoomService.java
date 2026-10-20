package com.example.hotelservice.service;

import com.example.hotelservice.dto.AvailabilityRequest;
import com.example.hotelservice.dto.RoomDto;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.entity.RoomBooking;
import com.example.hotelservice.exception.NotFoundException;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomBookingRepository;
import com.example.hotelservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomBookingRepository roomBookingRepository;

    public RoomDto createRoom(Long hotelId, RoomDto roomDto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Hotel not found with id: " + hotelId));

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(roomDto.getNumber());
        room.setAvailable(true);
        room.setTimesBooked(0);

        Room saved = roomRepository.save(room);
        return toDto(saved);
    }

    public List<RoomDto> getAllAvailableRooms() {
        return roomRepository.findByAvailableTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoomDto getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
        return toDto(room);
    }

    public List<RoomDto> getRecommendedRooms() {
        return roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getRoomStats() {
        return roomRepository.findAll(Sort.by(Sort.Direction.DESC, "timesBooked")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RoomDto updateRoom(Long id, RoomDto roomDto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + id));
        room.setNumber(roomDto.getNumber());
        if (roomDto.getAvailable() != null) {
            room.setAvailable(roomDto.getAvailable());
        }
        Room updated = roomRepository.save(room);
        return toDto(updated);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, AvailabilityRequest request) {
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + roomId));

        if (!room.getAvailable()) {
            return false;
        }

        if (request.getRequestId() != null) {
            if (roomBookingRepository.findByRequestId(request.getRequestId()).isPresent()) {
                log.info("Idempotent confirmAvailability: requestId={} already processed", request.getRequestId());
                return true;
            }
        }

        boolean overlapping = roomBookingRepository.existsOverlapping(
                roomId, request.getStartDate(), request.getEndDate());
        if (overlapping) {
            log.warn("Room {} is already booked for dates {} - {}", roomId, request.getStartDate(), request.getEndDate());
            return false;
        }

        RoomBooking roomBooking = new RoomBooking();
        roomBooking.setRoomId(roomId);
        roomBooking.setStartDate(request.getStartDate());
        roomBooking.setEndDate(request.getEndDate());
        roomBooking.setRequestId(request.getRequestId());
        roomBookingRepository.save(roomBooking);

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);

        log.info("Room {} booked for dates {} - {}, requestId={}",
                roomId, request.getStartDate(), request.getEndDate(), request.getRequestId());
        return true;
    }

    @Transactional
    public void releaseRoom(Long roomId, String requestId) {
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + roomId));

        if (requestId != null) {
            roomBookingRepository.deleteByRoomIdAndRequestId(roomId, requestId);
        }

        if (room.getTimesBooked() > 0) {
            room.setTimesBooked(room.getTimesBooked() - 1);
            roomRepository.save(room);
        }

        log.info("Room {} released, requestId={}", roomId, requestId);
    }

    private RoomDto toDto(Room room) {
        return new RoomDto(room.getId(), room.getHotel().getId(), room.getNumber(), room.getAvailable(), room.getTimesBooked());
    }
}