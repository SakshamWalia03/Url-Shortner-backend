package com.url.shortener.service;

import com.url.shortener.dto.UrlMappingDTO;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.UrlMappingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing URL mappings and analytics.
 * Click events are processed asynchronously through Redis.
 */
@Service
@AllArgsConstructor
public class UrlMappingService {

    private final UrlMappingRepository urlMappingRepository;

    // ----------------------------------------------
    // CREATE SHORT URL
    // ----------------------------------------------

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

        UrlMapping saved = urlMappingRepository.save(urlMapping);

        return convertToDto(saved);
    }

    // ----------------------------------------------
    // DTO MAPPING
    // ----------------------------------------------

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
    // SHORT URL GENERATOR
    // ----------------------------------------------

    private String generateUniqueShortUrl() {

        String shortUrl;

        do {
            shortUrl = randomShortString();
        } while (urlMappingRepository.findByShortUrl(shortUrl) != null);

        return shortUrl;
    }

    private String randomShortString() {

        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            sb.append(characters.charAt(
                    random.nextInt(characters.length())));
        }

        return sb.toString();
    }

    // ----------------------------------------------
    // GET URLS BY USER
    // ----------------------------------------------

    public List<UrlMappingDTO> getUrlsByUser(User user) {

        if (user == null) return List.of();

        return urlMappingRepository.findByUser(user)
                .stream()
                .map(this::convertToDto)
                .toList();
    }
    // ----------------------------------------------
    // GET ORIGINAL URL
    // ----------------------------------------------

    public UrlMapping getOriginalUrl(String shortUrl) {

        if (shortUrl == null || shortUrl.isBlank())
            return null;

        return urlMappingRepository.findByShortUrl(shortUrl);
    }
}