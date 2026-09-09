package com.website.gis.core.security;

import com.website.gis.config.CacheConfig;
import com.website.gis.core.entity.User;
import com.website.gis.core.repository.UserRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUser(username, true);
    }

    /**
     * Used only after a JWT signature has already been verified. This cache contains
     * authorization data, never the password hash. Login deliberately uses the
     * uncached UserDetailsService method because Spring Security erases credentials
     * from authenticated principals after a successful login.
     */
    @Cacheable(cacheNames = CacheConfig.USER_DETAILS, key = "#username", sync = true)
    public UserDetails loadUserForToken(String username) throws UsernameNotFoundException {
        return loadUser(username, false);
    }

    private UserDetails loadUser(String username, boolean includePassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        String roleWithPrefix = user.getRole().startsWith("ROLE_") ? user.getRole() : "ROLE_" + user.getRole();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleWithPrefix);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                includePassword ? user.getPassword() : "",
                List.of(authority));
    }
}
