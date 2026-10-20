package com.example.hotelservice.repository;

import com.example.hotelservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    // Получить все свободные (операционно) номера
    List<Room> findByAvailableTrue();

    // Получить свободные номера, отсортированные по timesBooked (для рекомендаций)
    List<Room> findByAvailableTrueOrderByTimesBookedAscIdAsc();
}