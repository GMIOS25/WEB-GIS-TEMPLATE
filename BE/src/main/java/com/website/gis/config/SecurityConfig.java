package com.website.gis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.website.gis.core.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AccessDeniedHandler accessDeniedHandler;
        private final AuthenticationEntryPoint authenticationEntryPoint;

        @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173}")
        private String allowedOriginsProperty;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AccessDeniedHandler accessDeniedHandler,
                        AuthenticationEntryPoint authenticationEntryPoint) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.accessDeniedHandler = accessDeniedHandler;
                this.authenticationEntryPoint = authenticationEntryPoint;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; " +
                                                                                "script-src 'self'; " +
                                                                                "style-src 'self' 'unsafe-inline'; " +
                                                                                "img-src 'self' data: https:; " +
                                                                                "connect-src 'self'; " +
                                                                                "frame-ancestors 'self'; " +
                                                                                "base-uri 'self'; " +
                                                                                "form-action 'self'")))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/index.html", "/favicon.ico", "/vite.svg",
                                                                "/assets/**", "/*.ico", "/*.png", "/*.svg", "/*.js", "/*.css")
                                                .permitAll()
                                                .requestMatchers("/api/auth/login").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().permitAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                List<String> allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isEmpty())
                                .toList();
                if (allowedOrigins.isEmpty()) {
                        throw new IllegalStateException(
                                        "CORS_ALLOWED_ORIGINS cannot be empty. " +
                                                        "Please set CORS_ALLOWED_ORIGINS environment variable with at least one origin "
                                                        +
                                                        "(e.g., 'http://localhost:5173,https://gis.gialai.gov.vn')");
                }
                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(
                                List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
                configuration.setExposedHeaders(List.of());
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
