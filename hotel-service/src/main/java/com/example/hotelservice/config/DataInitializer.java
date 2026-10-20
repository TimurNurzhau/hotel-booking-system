package com.example.hotelservice.config;

import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) {
            log.info("Hotels already exist, skipping data initialization");
            return;
        }

        Hotel grand = new Hotel();
        grand.setName("Grand Hotel");
        grand.setAddress("123 Main St");
        grand = hotelRepository.save(grand);

        Hotel plaza = new Hotel();
        plaza.setName("Plaza Inn");
        plaza.setAddress("456 Side Ave");
        plaza = hotelRepository.save(plaza);

        createRoom(grand, "101");
        createRoom(grand, "102");
        createRoom(plaza, "201");
        createRoom(plaza, "202");

        log.info("Initialized {} hotels and {} rooms",
                hotelRepository.count(), roomRepository.count());
    }

    private void createRoom(Hotel hotel, String number) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setAvailable(true);
        room.setTimesBooked(0);
        roomRepository.save(room);
    }
}