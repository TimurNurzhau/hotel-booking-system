package com.example.bookingservice.config;

import com.example.bookingservice.entity.User;
import com.example.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping data initialization");
            return;
        }

        createUser("admin", "admin123", "ADMIN");
        createUser("user", "user123", "USER");

        log.info("Initialized {} users", userRepository.count());
    }

    private void createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        log.info("Created user: {} (role={})", username, role);
    }
}