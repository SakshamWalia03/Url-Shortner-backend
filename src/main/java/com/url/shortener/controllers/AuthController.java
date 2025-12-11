package com.url.shortener.controllers;

import com.url.shortener.dto.LoginRequest;
import com.url.shortener.dto.RegisterRequest;
import com.url.shortener.models.User;
import com.url.shortener.service.UserService;
import com.url.shortener.security.jwt.JwtAuthenticationResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints: registration and login.
 */
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    // ----------------------------------------------
    // REGISTER USER
    // ----------------------------------------------
    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setUsername(registerRequest.getUsername());
            user.setRole("ROLE_USER");

            userService.registerUser(user);

            return ResponseEntity.ok(
                    ApiResponse.success("User registered successfully")
            );

        } catch (IllegalArgumentException e) {
            // This handles "username/email already exists"
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
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        try {
            JwtAuthenticationResponse jwtResponse = userService.authenticateUser(loginRequest);
            return ResponseEntity.ok(ApiResponse.success(jwtResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }

    // ----------------------------------------------
    // HELPER RESPONSE CLASS
    // ----------------------------------------------
    /**
     * Standard API response wrapper for consistency.
     */
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