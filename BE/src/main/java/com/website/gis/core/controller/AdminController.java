package com.website.gis.core.controller;

import com.website.gis.core.dto.UserCreateRequest;
import com.website.gis.core.dto.UserDto;
import com.website.gis.core.dto.UserUpdateRequest;
import com.website.gis.core.entity.User;
import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import com.website.gis.core.repository.UserRepository;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
        }

        @GetMapping
        public ResponseEntity<List<UserDto>> getAllUsers() {
                List<UserDto> users = userRepository.findAll().stream()
                                .map(user -> UserDto.builder()
                                                .id(user.getId())
                                                .username(user.getUsername())
                                                .fullName(user.getFullName())
                                                .role(user.getRole())
                                                .build())
                                .collect(Collectors.toList());
                return ResponseEntity.ok(users);
        }

        @PostMapping
        public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
                if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                        throw new BadRequestException("Username already exists");
                }

                User user = User.builder()
                                .username(request.getUsername())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .fullName(request.getFullName())
                                .role("VIEWER") // Only allow creating VIEWER accounts via this admin endpoint as per
                                                // requirements
                                .build();

                User savedUser = userRepository.save(user);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(UserDto.builder()
                                                .id(savedUser.getId())
                                                .username(savedUser.getUsername())
                                                .fullName(savedUser.getFullName())
                                                .role(savedUser.getRole())
                                                .build());
        }

        @PutMapping("/{id}")
        public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                        @Valid @RequestBody UserUpdateRequest request) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

                user.setFullName(request.getFullName());

                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                }

                User updatedUser = userRepository.save(user);

                return ResponseEntity.ok(UserDto.builder()
                                .id(updatedUser.getId())
                                .username(updatedUser.getUsername())
                                .fullName(updatedUser.getFullName())
                                .role(updatedUser.getRole())
                                .build());
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<String> deleteUser(@PathVariable Long id) {
                User user = userRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String currentUsername = auth.getName();

                if (user.getUsername().equals(currentUsername)) {
                        throw new BadRequestException("You cannot delete your own account");
                }

                if (user.getRole().equals("ADMIN") && userRepository.countByRole("ADMIN") <= 1) {
                        throw new BadRequestException("Cannot delete the last remaining ADMIN account");
                }

                userRepository.delete(user);
                return ResponseEntity.ok("User deleted successfully");
        }
}