package com.url.shortener.controllers;

import com.url.shortener.dto.UrlMappingDTO;
import com.url.shortener.models.User;
import com.url.shortener.service.RateLimitService;
import com.url.shortener.service.SpamProtectionService;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing URL operations.
 */
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlMappingController {

    private final UrlMappingService urlMappingService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final SpamProtectionService spamProtectionService;

    // ==============================
    // Create Short URL
    // ==============================
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request,
                                            Principal principal) {

        String originalUrl = request.get("originalUrl");

        if (originalUrl == null || originalUrl.isBlank()) {
            return ResponseEntity.badRequest().body("URL cannot be empty");
        }

        User user = userService.findByUsername(principal.getName());

        // ============================
        // Rate Limiting
        // Allow max 20 URLs per minute
        // ============================
        String rateLimitKey = "shorten:" + user.getId();

        if (!rateLimitService.isAllowed(rateLimitKey, 20, 60)) {
            return ResponseEntity
                    .status(429)
                    .body("Too many URLs created. Please try again later.");
        }

        // ============================
        // Spam Protection
        // ============================
        if (spamProtectionService.isBlocked(originalUrl)) {
            return ResponseEntity
                    .badRequest()
                    .body("Blocked URL detected");
        }

        UrlMappingDTO result = urlMappingService.createShortUrl(originalUrl, user);
        return ResponseEntity.ok(result);
    }

    // ==============================
    // Fetch URLs created by user
    // ==============================
    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal) {

        User user = userService.findByUsername(principal.getName());
        List<UrlMappingDTO> urls = urlMappingService.getUrlsByUser(user);

        return ResponseEntity.ok(urls);
    }
}