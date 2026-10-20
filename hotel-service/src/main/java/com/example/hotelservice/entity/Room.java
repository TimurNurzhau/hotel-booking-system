package com.example.hotelservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String number; // Номер комнаты (например, "101")

    @Column(nullable = false)
    private Boolean available = true; // Операционная доступность (ремонт/вывод из эксплуатации)

    @Column(nullable = false)
    private Integer timesBooked = 0; // Счётчик бронирований для статистики
}