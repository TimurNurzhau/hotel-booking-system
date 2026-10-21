package com.example.hotelservice.mapper;

import com.example.hotelservice.dto.HotelDto;
import com.example.hotelservice.entity.Hotel;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelDto toDto(Hotel hotel);

    Hotel toEntity(HotelDto hotelDto);

    List<HotelDto> toDtoList(List<Hotel> hotels);
}