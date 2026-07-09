package com.website.gis.controller;

import com.website.gis.dto.LoginRequest;
import com.website.gis.dto.LoginResponse;
import com.website.gis.entity.User;
import com.website.gis.repository.UserRepository;
import com.website.gis.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider tokenProvider;
        private final UserRepository userRepository;

        public AuthController(AuthenticationManager authenticationManager,
                        JwtTokenProvider tokenProvider,
                        UserRepository userRepository) {
                this.authenticationManager = authenticationManager;
                this.tokenProvider = tokenProvider;
                this.userRepository = userRepository;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = tokenProvider.generateToken(authentication);

                User user = userRepository.findByUsername(loginRequest.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

                return ResponseEntity.ok(LoginResponse.builder()
                                .token(jwt)
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .role(user.getRole())
                                .build());
        }
}
