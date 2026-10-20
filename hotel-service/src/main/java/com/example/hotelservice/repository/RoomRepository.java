package com.example.hotelservice.repository;

import com.example.hotelservice.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // ИСПРАВЛЕНИЕ: Добавлен @EntityGraph, чтобы решить проблему N+1 при получении отелей
    @EntityGraph(attributePaths = {"hotel"})
    List<Room> findByAvailableTrue();

    @EntityGraph(attributePaths = {"hotel"})
    List<Room> findByAvailableTrueOrderByTimesBookedAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);
}