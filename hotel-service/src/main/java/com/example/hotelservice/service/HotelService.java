package com.example.hotelservice.service;

import com.example.hotelservice.dto.HotelDto;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelDto createHotel(HotelDto hotelDto) {
        Hotel hotel = new Hotel();
        hotel.setName(hotelDto.getName());
        hotel.setAddress(hotelDto.getAddress());
        Hotel saved = hotelRepository.save(hotel);
        return new HotelDto(saved.getId(), saved.getName(), saved.getAddress());
    }

    public List<HotelDto> getAllHotels() {
        return hotelRepository.findAll().stream()
                .map(h -> new HotelDto(h.getId(), h.getName(), h.getAddress()))
                .collect(Collectors.toList());
    }
}