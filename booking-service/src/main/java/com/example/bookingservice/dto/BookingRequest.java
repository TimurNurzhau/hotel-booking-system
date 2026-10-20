package com.example.bookingservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequest {
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean autoSelect; // Если true, roomId игнорируется
    private String requestId;   // Для идемпотентности
}