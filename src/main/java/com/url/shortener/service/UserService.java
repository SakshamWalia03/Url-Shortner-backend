package com.url.shortener.service;

import com.url.shortener.dto.LoginRequest;
import com.url.shortener.models.RefreshToken;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    private static final String USERNAME_EXISTS = "Username already exists";
    private static final String EMAIL_EXISTS    = "Email already exists";
    private static final String USER_NOT_FOUND  = "Username not found with username: ";

    // ----------------------------------------------
    // REGISTER USER
    // ----------------------------------------------
    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException(USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException(EMAIL_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // ----------------------------------------------
    // AUTHENTICATE USER AND GENERATE TOKENS
    // ----------------------------------------------
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generate access token
        String accessToken = jwtUtils.generateToken(userDetails);

        // Generate refresh token
        User user = findByUsername(userDetails.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new JwtAuthenticationResponse(accessToken, refreshToken.getToken());
    }

    // ----------------------------------------------
    // FIND USER BY USERNAME
    // ----------------------------------------------
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND + username));
    }
}