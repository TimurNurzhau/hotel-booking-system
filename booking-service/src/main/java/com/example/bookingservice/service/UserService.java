package com.example.bookingservice.service;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.BadRequestException;
import com.example.bookingservice.exception.ConflictException;
import com.example.bookingservice.exception.NotFoundException;
import com.example.bookingservice.repository.UserRepository;
import com.example.bookingservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Username must not be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("User already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // ИСПРАВЛЕНИЕ: Публичная регистрация всегда выдает роль USER.
        // Роль ADMIN выдается только через административные эндпоинты или DataInitializer.
        user.setRole("USER");
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token);
    }

    public UserDto createUser(RegisterRequest request, String role) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Username must not be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("User already exists: " + request.getUsername());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role != null ? role : "USER");
        userRepository.save(user);
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: id=" + id));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }
        userRepository.save(user);
        return new UserDto(user.getId(), user.getUsername(), user.getRole());
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found: id=" + id);
        }
        userRepository.deleteById(id);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());
    }
}