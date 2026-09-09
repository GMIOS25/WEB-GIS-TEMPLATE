package com.website.gis.core.security;

import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loginLookupIncludesPasswordButTokenLookupDoesNotCacheCredentials() {
        User user = User.builder()
                .username("viewer")
                .password("bcrypt-hash")
                .role("VIEWER")
                .build();
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(user));

        UserDetails loginUser = service.loadUserByUsername("viewer");
        UserDetails tokenUser = service.loadUserForToken("viewer");

        assertEquals("bcrypt-hash", loginUser.getPassword());
        assertEquals("", tokenUser.getPassword());
        assertEquals("ROLE_VIEWER", tokenUser.getAuthorities().iterator().next().getAuthority());
    }
}
