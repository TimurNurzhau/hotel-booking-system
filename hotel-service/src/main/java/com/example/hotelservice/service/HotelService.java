package com.example.hotelservice.service;

import com.example.hotelservice.dto.HotelDto;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.exception.NotFoundException;
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

    public HotelDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel not found with id: " + id));
        return new HotelDto(hotel.getId(), hotel.getName(), hotel.getAddress());
    }

    public HotelDto updateHotel(Long id, HotelDto hotelDto) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel not found with id: " + id));
        hotel.setName(hotelDto.getName());
        hotel.setAddress(hotelDto.getAddress());
        Hotel updated = hotelRepository.save(hotel);
        return new HotelDto(updated.getId(), updated.getName(), updated.getAddress());
    }

    public void deleteHotel(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new NotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
    }
}