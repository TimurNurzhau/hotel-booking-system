package com.example.hotelservice.repository;

import com.example.hotelservice.entity.RoomBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, Long> {

    @Query("SELECT COUNT(rb) > 0 FROM RoomBooking rb WHERE rb.roomId = :roomId " +
            "AND rb.startDate < :endDate AND rb.endDate > :startDate")
    boolean existsOverlapping(@Param("roomId") Long roomId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    Optional<RoomBooking> findByRequestId(String requestId);
}