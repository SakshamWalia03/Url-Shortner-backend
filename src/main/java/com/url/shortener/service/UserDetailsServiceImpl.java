package com.url.shortener.service;

import com.url.shortener.models.User;
import com.url.shortener.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of Spring Security's UserDetailsService.
 * Used by Spring Security to load user-specific data during authentication.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // Repository for accessing user data from the database
    private final UserRepository userRepository;

    /**
     * Locates the user based on the username.
     * This method is called by Spring Security during authentication.
     *
     * @param username the username identifying the user whose data is required.
     * @return UserDetails object containing user information and authorities.
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch user from the database using the username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username)
                );

        // Build a UserDetailsImpl object from the user entity
        return UserDetailsImpl.build(user);
    }
}