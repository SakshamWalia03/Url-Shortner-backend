package com.url.shortener.security;

import com.url.shortener.security.jwt.JwtAutenticationFilter;
import com.url.shortener.service.UserDetailsServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration class.
 * Configures authentication, authorization, password encoding, and JWT filtering.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables method-level security annotations (@PreAuthorize)
@AllArgsConstructor
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    // ----------------------------------------------
    // JWT FILTER
    // ----------------------------------------------
    /**
     * Bean for JWT authentication filter.
     * This filter validates JWT tokens for incoming requests.
     */
    @Bean
    public JwtAutenticationFilter jwtAuthenticationFilter() {
        return new JwtAutenticationFilter();
    }

    // ----------------------------------------------
    // PASSWORD ENCODER
    // ----------------------------------------------
    /**
     * Bean for password encoding using BCrypt.
     * Ensures passwords are stored securely in the database.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ----------------------------------------------
    // AUTHENTICATION MANAGER
    // ----------------------------------------------
    /**
     * Bean to expose AuthenticationManager from AuthenticationConfiguration.
     * Needed for manual authentication in services (e.g., login).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // ----------------------------------------------
    // DAO AUTHENTICATION PROVIDER
    // ----------------------------------------------
    /**
     * Configures DaoAuthenticationProvider with custom UserDetailsService
     * and password encoder for authentication.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // ----------------------------------------------
    // SECURITY FILTER CHAIN
    // ----------------------------------------------
    /**
     * Configures HTTP security:
     * - Disables CSRF (for API usage)
     * - Configures endpoint access rules
     * - Adds JWT filter before UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // Allow public endpoints (register/login)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Require authentication for user URL management
                        .requestMatchers("/api/urls/**").authenticated()
                        // Allow redirection endpoints publicly
                        .requestMatchers("/{shortUrl}").permitAll()
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                );

        // Set authentication provider
        http.authenticationProvider(authenticationProvider());

        // Add JWT filter to validate tokens in requests
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}