package com.url.shortener.controllers;

import com.url.shortener.dto.ClickEventDTO;
import com.url.shortener.dto.UrlMappingDTO;
import com.url.shortener.models.User;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlMappingController {

    private final UrlMappingService urlMappingService;
    private final UserService userService;

    // ==============================
    // Create Short URL
    // ==============================
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> createShortUrl(@RequestBody Map<String, String> request,
                                                        Principal principal) {

        String originalUrl = request.get("originalUrl");
        if (originalUrl == null || originalUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User user = userService.findByUsername(principal.getName());
        UrlMappingDTO result = urlMappingService.createShortUrl(originalUrl, user);

        return ResponseEntity.ok(result);
    }

    // ==============================
    // Fetch User URLs
    // ==============================
    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal) {

        User user = userService.findByUsername(principal.getName());
        List<UrlMappingDTO> urls = urlMappingService.getUrlsByUser(user);

        return ResponseEntity.ok(urls);
    }

    // ==============================
    // Analytics for a single short URL
    // ==============================
    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUrlAnalytics(@PathVariable String shortUrl,
                                             @RequestParam String startDate,
                                             @RequestParam String endDate) {

        LocalDateTime start;
        LocalDateTime end;

        try {
            start = LocalDateTime.parse(startDate);
            end   = LocalDateTime.parse(endDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO: yyyy-MM-ddTHH:mm:ss");
        }

        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body("endDate cannot be before startDate");
        }

        List<ClickEventDTO> result = urlMappingService.getClickEventsByDate(shortUrl, start, end);
        return ResponseEntity.ok(result);
    }

    // ==============================
    // Analytics: Total Clicks per Day for User
    // ==============================
    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTotalClicksByDate(Principal principal,
                                                  @RequestParam String startDate,
                                                  @RequestParam String endDate) {

        LocalDate start;
        LocalDate end;

        try {
            start = LocalDate.parse(startDate);
            end   = LocalDate.parse(endDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO: yyyy-MM-dd");
        }

        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body("endDate cannot be before startDate");
        }

        User user = userService.findByUsername(principal.getName());

        Map<LocalDate, Long> result = urlMappingService.getTotalClicksByUserAndDate(user, start, end);
        return ResponseEntity.ok(result);
    }
}