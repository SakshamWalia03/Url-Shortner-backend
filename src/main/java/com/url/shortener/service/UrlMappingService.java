package com.url.shortener.service;

import com.url.shortener.dto.ClickEventDTO;
import com.url.shortener.dto.UrlMappingDTO;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing URL mappings, clicks, and analytics.
 */
@Service
@AllArgsConstructor
public class UrlMappingService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    // ----------------------------------------------
    // CREATE SHORT URL
    // ----------------------------------------------
    /**
     * Creates a new short URL for a given original URL and user.
     *
     * @param originalUrl Original URL to shorten.
     * @param user        User creating the short URL.
     * @return UrlMappingDTO containing short URL details.
     * @throws IllegalArgumentException if originalUrl or user is null/invalid.
     */
    public UrlMappingDTO createShortUrl(String originalUrl, User user) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("Original URL cannot be empty");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String shortUrl = generateUniqueShortUrl();

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setClickCount(0);
        urlMapping.setCreatedDate(LocalDateTime.now());

        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
        return convertToDto(savedUrlMapping);
    }

    // ----------------------------------------------
    // DTO MAPPING
    // ----------------------------------------------
    /**
     * Converts a UrlMapping entity to a UrlMappingDTO.
     *
     * @param urlMapping UrlMapping entity.
     * @return UrlMappingDTO or null if input is null.
     */
    private UrlMappingDTO convertToDto(UrlMapping urlMapping) {
        if (urlMapping == null) return null;

        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setClickCount(urlMapping.getClickCount());
        dto.setCreatedDate(urlMapping.getCreatedDate());

        if (urlMapping.getUser() != null) {
            dto.setUsername(urlMapping.getUser().getUsername());
        }
        return dto;
    }

    // ----------------------------------------------
    // SHORT URL GENERATOR (ENSURES UNIQUENESS)
    // ----------------------------------------------
    private String generateUniqueShortUrl() {
        String shortUrl;
        do {
            shortUrl = randomShortString();
        } while (urlMappingRepository.findByShortUrl(shortUrl) != null);
        return shortUrl;
    }

    /**
     * Generates a random 8-character alphanumeric string for short URLs.
     */
    private String randomShortString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    // ----------------------------------------------
    // GET ALL URLS BY USER
    // ----------------------------------------------
    /**
     * Retrieves all URLs created by a specific user.
     *
     * @param user User entity.
     * @return List of UrlMappingDTO; empty list if user is null or no URLs.
     */
    public List<UrlMappingDTO> getUrlsByUser(User user) {
        if (user == null) return List.of();

        return urlMappingRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .toList();
    }

    // ----------------------------------------------
    // GET ANALYTICS FOR ONE SHORT URL
    // ----------------------------------------------
    /**
     * Retrieves click events for a short URL within a date range.
     *
     * @param shortUrl Short URL string.
     * @param start    Start date-time.
     * @param end      End date-time.
     * @return List of ClickEventDTO grouped by date.
     */
    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
        if (shortUrl == null || shortUrl.isBlank()) return List.of();

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if (urlMapping == null) return List.of();

        if (start == null || end == null || end.isBefore(start)) return List.of();

        List<ClickEvent> events =
                clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping, start, end);

        if (events.isEmpty()) return List.of();

        // Group events by date and convert to DTO
        return events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClickDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ClickEventDTO(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    // ----------------------------------------------
    // GET TOTAL CLICKS ACROSS ALL USER URLs
    // ----------------------------------------------
    /**
     * Retrieves total clicks for all URLs of a user within a date range.
     *
     * @param user  User entity.
     * @param start Start date.
     * @param end   End date.
     * @return Map of date to click count; TreeMap ensures sorted order.
     */
    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        if (user == null || start == null || end == null) return new TreeMap<>();
        if (end.isBefore(start)) return new TreeMap<>();

        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        if (urlMappings.isEmpty()) return new TreeMap<>();

        List<ClickEvent> clickEvents =
                clickEventRepository.findByUrlMappingInAndClickDateBetween(
                        urlMappings,
                        start.atStartOfDay(),
                        end.plusDays(1).atStartOfDay()
                );

        return clickEvents.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClickDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    // ----------------------------------------------
    // REDIRECT AND RECORD CLICK
    // ----------------------------------------------
    /**
     * Retrieves original URL for a short URL and records a click event.
     *
     * @param shortUrl Short URL string.
     * @return UrlMapping entity; null if short URL does not exist.
     */
    public UrlMapping getOriginalUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isBlank()) return null;

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
        if (urlMapping != null) {
            // Increment click count
            urlMapping.setClickCount(urlMapping.getClickCount() + 1);
            urlMappingRepository.save(urlMapping);

            // Record click event
            ClickEvent click = new ClickEvent();
            click.setClickDate(LocalDateTime.now());
            click.setUrlMapping(urlMapping);
            clickEventRepository.save(click);
        }

        return urlMapping;
    }
}