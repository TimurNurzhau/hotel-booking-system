package com.example.bookingservice.service;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.*;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.BadRequestException;
import com.example.bookingservice.exception.ForbiddenException;
import com.example.bookingservice.exception.NotFoundException;
import com.example.bookingservice.exception.ServiceUnavailableException;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelServiceClient hotelServiceClient;

    @Transactional
    public BookingDto createBooking(BookingRequest request, String username) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("startDate and endDate are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }

        if (request.getRequestId() != null) {
            Optional<Booking> existing = bookingRepository.findByRequestId(request.getRequestId());
            if (existing.isPresent()) {
                log.info("Idempotent request detected. Returning existing booking id={}", existing.get().getId());
                return toDto(existing.get());
            }
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        Long roomId = request.getRoomId();
        if (request.isAutoSelect()) {
            List<RoomDto> recommended;
            try {
                recommended = hotelServiceClient.getRecommendedRooms();
            } catch (Exception e) {
                throw new ServiceUnavailableException("Failed to get recommended rooms: " + e.getMessage());
            }
            if (recommended == null || recommended.isEmpty()) {
                throw new ServiceUnavailableException("No available rooms");
            }
            roomId = recommended.get(0).getId();
        }

        if (roomId == null) {
            throw new BadRequestException("roomId is required when autoSelect=false");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(roomId);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setRequestId(request.getRequestId());
        booking = bookingRepository.save(booking);

        log.info("Booking created in PENDING: id={}, roomId={}", booking.getId(), roomId);

        try {
            AvailabilityRequest availabilityRequest = new AvailabilityRequest();
            availabilityRequest.setStartDate(request.getStartDate());
            availabilityRequest.setEndDate(request.getEndDate());
            availabilityRequest.setRequestId(request.getRequestId());

            Boolean confirmed = hotelServiceClient.confirmAvailability(roomId, availabilityRequest);

            if (Boolean.TRUE.equals(confirmed)) {
                booking.setStatus(Booking.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                log.info("Booking CONFIRMED: id={}", booking.getId());
            } else {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                log.warn("Booking CANCELLED (room not available): id={}", booking.getId());
                throw new ServiceUnavailableException("Room not available");
            }
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.error("Booking CANCELLED due to error: id={}, error={}", booking.getId(), e.getMessage());

            try {
                hotelServiceClient.releaseRoom(roomId);
                log.info("Room released after compensation: roomId={}", roomId);
            } catch (Exception ex) {
                log.error("Failed to release room: {}", ex.getMessage());
            }
            throw new ServiceUnavailableException("Booking failed: " + e.getMessage());
        }

        return toDto(booking);
    }

    public Page<BookingDto> getUserBookings(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookingRepository.findByUserId(user.getId(), pageable).map(this::toDto);
    }

    public BookingDto getBookingById(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: id=" + id));
        if (!booking.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("Access denied to booking id=" + id);
        }
        return toDto(booking);
    }

    public Page<BookingDto> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookingRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public void cancelBooking(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: id=" + id));
        if (!booking.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("Access denied to booking id=" + id);
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking CANCELLED by user: id={}", id);

        try {
            hotelServiceClient.releaseRoom(booking.getRoomId());
            log.info("Room released after cancellation: roomId={}", booking.getRoomId());
        } catch (Exception e) {
            log.error("Failed to release room: {}", e.getMessage());
        }
    }

    private BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getId(),
                booking.getRoomId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus().name(),
                booking.getCreatedAt()
        );
    }
}