package com.url.shortener.service;

import com.url.shortener.dto.LoginRequest;
import com.url.shortener.models.User;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.security.jwt.JwtAuthenticationResponse;
import com.url.shortener.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for user management, registration, authentication,
 * and JWT token generation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // Error messages as constants for consistency
    private static final String USERNAME_EXISTS = "Username already exists";
    private static final String EMAIL_EXISTS = "Email already exists";
    private static final String USER_NOT_FOUND = "Username not found with username: ";

    // ----------------------------------------------
    // REGISTER USER
    // ----------------------------------------------
    /**
     * Registers a new user with encoded password.
     * Checks for existing username and email to prevent duplicates.
     *
     * @param user User entity to register.
     * @return Saved User entity.
     * @throws IllegalArgumentException if username or email already exists.
     */
    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException(USERNAME_EXISTS);
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException(EMAIL_EXISTS);
        }

        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // ----------------------------------------------
    // AUTHENTICATE USER AND GENERATE JWT
    // ----------------------------------------------
    /**
     * Authenticates a user using Spring Security and returns a JWT token.
     *
     * @param loginRequest LoginRequest containing username and password.
     * @return JwtAuthenticationResponse containing JWT token.
     */
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        // Perform authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get authenticated user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generate JWT token
        String jwt = jwtUtils.generateToken(userDetails);

        return new JwtAuthenticationResponse(jwt);
    }

    // ----------------------------------------------
    // FIND USER BY USERNAME
    // ----------------------------------------------
    /**
     * Retrieves a user by username.
     *
     * @param username Username of the user.
     * @return User entity.
     * @throws UsernameNotFoundException if user is not found.
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND + username));
    }
}