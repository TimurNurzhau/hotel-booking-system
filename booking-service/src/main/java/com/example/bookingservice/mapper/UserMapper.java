package com.example.bookingservice.mapper;

import com.example.bookingservice.dto.UserDto;
import com.example.bookingservice.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);
}