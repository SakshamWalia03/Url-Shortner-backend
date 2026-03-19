package com.url.shortener.controllers;

import com.url.shortener.dto.LoginRequest;
import com.url.shortener.dto.RegisterRequest;
import com.url.shortener.models.RefreshToken;
import com.url.shortener.models.User;
import com.url.shortener.security.jwt.JwtAuthenticationResponse;
import com.url.shortener.security.jwt.JwtUtils;
import com.url.shortener.service.RefreshTokenService;
import com.url.shortener.service.RateLimitService;
import com.url.shortener.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication endpoints: registration, login, token refresh, logout.
 */
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;

    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds

    // ----------------------------------------------
    // REGISTER USER
    // ----------------------------------------------
    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest,
                                          HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        if (!rateLimitService.isAllowed("register:" + ip, 5, 60)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("Too many registration attempts. Please try again later."));
        }

        try {
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setUsername(registerRequest.getUsername());
            user.setRole("ROLE_USER");

            userService.registerUser(user);

            return ResponseEntity.ok(ApiResponse.success("User registered successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    // ----------------------------------------------
    // LOGIN USER
    // ----------------------------------------------
    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        String ip = request.getRemoteAddr();

        if (!rateLimitService.isAllowed("login:" + ip, 10, 60)) {
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("Too many login attempts. Try again later."));
        }

        try {
            JwtAuthenticationResponse jwtResponse = userService.authenticateUser(loginRequest);

            // Set refresh token as HttpOnly cookie
            ResponseCookie refreshCookie = buildRefreshCookie(
                    jwtResponse.getRefreshToken(), REFRESH_TOKEN_MAX_AGE);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // Return only access token in body
            return ResponseEntity.ok(ApiResponse.success(Map.of("accessToken", jwtResponse.getAccessToken())));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }

    // ----------------------------------------------
    // REFRESH TOKEN
    // ----------------------------------------------
    @PostMapping("/public/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token missing. Please login again."));
        }

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Generate new access token
                    String newAccessToken = jwtUtils.generateTokenFromUsername(
                            user.getUsername(), user.getRole());

                    // Rotate refresh token — issue a new one each time
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

                    ResponseCookie refreshCookie = buildRefreshCookie(
                            newRefreshToken.getToken(), REFRESH_TOKEN_MAX_AGE);
                    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                    return ResponseEntity.ok(ApiResponse.success(Map.of("accessToken", newAccessToken)));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid refresh token. Please login again.")));
    }

    // ----------------------------------------------
    // LOGOUT
    // ----------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        // Invalidate refresh token in DB
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.findByToken(refreshToken)
                    .ifPresent(token -> refreshTokenService.deleteByUser(token.getUser()));
        }

        // Clear the cookie by setting maxAge to 0
        ResponseCookie clearCookie = buildRefreshCookie("", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    // ----------------------------------------------
    // COOKIE BUILDER
    // ----------------------------------------------
    private ResponseCookie buildRefreshCookie(String value, int maxAge) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(maxAge)
                .sameSite("None")
                .build();
    }

    // ----------------------------------------------
    // HELPER RESPONSE CLASS
    // ----------------------------------------------
    @Data
    static class ApiResponse {
        private boolean success;
        private Object data;
        private String message;

        private ApiResponse(boolean success, Object data, String message) {
            this.success = success;
            this.data = data;
            this.message = message;
        }

        public static ApiResponse success(Object data) {
            return new ApiResponse(true, data, null);
        }

        public static ApiResponse success(String message) {
            return new ApiResponse(true, null, message);
        }

        public static ApiResponse error(String message) {
            return new ApiResponse(false, null, message);
        }
    }
}