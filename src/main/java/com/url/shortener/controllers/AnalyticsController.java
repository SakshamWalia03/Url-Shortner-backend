package com.url.shortener.controllers;

import com.url.shortener.dto.ClickAnalyticsDTO;
import com.url.shortener.models.User;
import com.url.shortener.service.AnalyticsService;
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

/**
 * Controller responsible for analytics APIs.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    // ----------------------------------------------
    // Clicks per Day (All time for a short URL)
    // ----------------------------------------------
    @GetMapping("/{shortUrl}/daily")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickAnalyticsDTO>> getClicksPerDay(
            @PathVariable String shortUrl) {

        List<ClickAnalyticsDTO> result =
                analyticsService.getClicksPerDay(shortUrl);

        return ResponseEntity.ok(result);
    }

    // ----------------------------------------------
    // Clicks per Browser
    // ----------------------------------------------
    @GetMapping("/{shortUrl}/browser")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getClicksByBrowser(
            @PathVariable String shortUrl) {

        return ResponseEntity.ok(
                analyticsService.getClicksByBrowser(shortUrl)
        );
    }

    // ----------------------------------------------
    // Clicks per Device
    // ----------------------------------------------
    @GetMapping("/{shortUrl}/device")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getClicksByDevice(
            @PathVariable String shortUrl) {

        return ResponseEntity.ok(
                analyticsService.getClicksByDevice(shortUrl)
        );
    }

    // ----------------------------------------------
    // Clicks per Country
    // ----------------------------------------------
    @GetMapping("/{shortUrl}/country")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getClicksByCountry(
            @PathVariable String shortUrl) {

        return ResponseEntity.ok(
                analyticsService.getClicksByCountry(shortUrl)
        );
    }

    // ----------------------------------------------
    // Analytics for URL within date range
    // ----------------------------------------------
    @GetMapping("/{shortUrl}/range")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUrlAnalytics(
            @PathVariable String shortUrl,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {

            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);

            if (end.isBefore(start)) {
                return ResponseEntity.badRequest()
                        .body("endDate cannot be before startDate");
            }

            List<ClickAnalyticsDTO> result =
                    analyticsService.getClickEventsByDate(shortUrl, start, end);

            return ResponseEntity.ok(result);

        } catch (DateTimeParseException e) {

            return ResponseEntity.badRequest()
                    .body("Invalid date format. Use ISO: yyyy-MM-ddTHH:mm:ss");
        }
    }

    // ----------------------------------------------
    // Total Clicks per Day for Logged-in User
    // ----------------------------------------------
    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getTotalClicksByDate(
            Principal principal,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        try {

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (end.isBefore(start)) {
                return ResponseEntity.badRequest()
                        .body("endDate cannot be before startDate");
            }

            User user = userService.findByUsername(principal.getName());

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body("User not found");
            }

            List<ClickAnalyticsDTO> result =
                    analyticsService.getTotalClicksByUserAndDate(user, start, end);

            return ResponseEntity.ok(result);

        } catch (DateTimeParseException e) {

            return ResponseEntity.badRequest()
                    .body("Invalid date format. Use yyyy-MM-dd");
        }
    }
}