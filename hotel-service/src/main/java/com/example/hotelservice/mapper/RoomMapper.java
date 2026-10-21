package com.example.hotelservice.mapper;

import com.example.hotelservice.dto.RoomDto;
import com.example.hotelservice.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomDto toDto(Room room);

    List<RoomDto> toDtoList(List<Room> rooms);
}