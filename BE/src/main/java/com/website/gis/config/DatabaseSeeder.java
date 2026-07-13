package com.website.gis.config;

import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Quản trị viên Gia Lai")
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);

            User viewer = User.builder()
                    .username("viewer")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Người xem bản đồ")
                    .role("VIEWER")
                    .build();
            userRepository.save(viewer);

            System.out.println("Database seeded with default accounts (admin / viewer)");
        }
    }
}
