package com.url.shortener.service;

import com.url.shortener.dto.ClickAnalyticsDTO;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.models.User;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for analytics operations.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;

    // ----------------------------------------------
    // ANALYTICS FOR ONE URL (DATE RANGE)
    // ----------------------------------------------
    public List<ClickAnalyticsDTO> getClickEventsByDate(
            String shortUrl,
            LocalDateTime start,
            LocalDateTime end) {

        if (shortUrl == null || shortUrl.isBlank() || start == null || end == null) {
            return List.of();
        }

        UrlMapping mapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (mapping == null) return List.of();

        List<ClickEvent> events =
                clickEventRepository.findByUrlMappingAndClickDateBetween(
                        mapping,
                        start,
                        end
                );

        return groupClicksByDate(events);
    }

    // ----------------------------------------------
    // TOTAL CLICKS FOR USER (GROUPED BY DATE)
    // ----------------------------------------------
    public List<ClickAnalyticsDTO> getTotalClicksByUserAndDate(
            User user,
            LocalDate start,
            LocalDate end) {

        if (user == null || start == null || end == null || end.isBefore(start)) {
            return List.of();
        }

        List<UrlMapping> urls = urlMappingRepository.findByUser(user);

        if (urls == null || urls.isEmpty()) {
            return List.of();
        }

        List<ClickEvent> events =
                clickEventRepository.findByUrlMappingInAndClickDateBetween(
                        urls,
                        start.atStartOfDay(),
                        end.plusDays(1).atStartOfDay()
                );
        System.out.println(events);
        return groupClicksByDate(events);
    }

    // ----------------------------------------------
    // CLICKS PER DAY (ALL TIME)
    // ----------------------------------------------
    public List<ClickAnalyticsDTO> getClicksPerDay(String shortUrl) {

        if (shortUrl == null || shortUrl.isBlank()) return List.of();

        UrlMapping url = urlMappingRepository.findByShortUrl(shortUrl);

        if (url == null) return List.of();

        List<ClickEvent> events = clickEventRepository.findByUrlMapping(url);

        return groupClicksByDate(events);
    }

    // ----------------------------------------------
    // CLICKS PER BROWSER
    // ----------------------------------------------
    public Map<String, Long> getClicksByBrowser(String shortUrl) {

        UrlMapping url = urlMappingRepository.findByShortUrl(shortUrl);

        if (url == null) return Map.of();

        List<ClickEvent> events = clickEventRepository.findByUrlMapping(url);

        Map<String, Long> result = new HashMap<>();

        for (ClickEvent event : events) {

            String browser = Optional.ofNullable(event.getBrowser())
                    .orElse("Unknown");

            result.put(browser, result.getOrDefault(browser, 0L) + 1);
        }

        return result;
    }

    // ----------------------------------------------
    // CLICKS PER DEVICE
    // ----------------------------------------------
    public Map<String, Long> getClicksByDevice(String shortUrl) {

        UrlMapping url = urlMappingRepository.findByShortUrl(shortUrl);

        if (url == null) return Map.of();

        List<ClickEvent> events = clickEventRepository.findByUrlMapping(url);

        Map<String, Long> result = new HashMap<>();

        for (ClickEvent event : events) {

            String device = Optional.ofNullable(event.getDevice())
                    .orElse("Unknown");

            result.put(device, result.getOrDefault(device, 0L) + 1);
        }

        return result;
    }

    // ----------------------------------------------
    // CLICKS PER COUNTRY
    // ----------------------------------------------
    public Map<String, Long> getClicksByCountry(String shortUrl) {

        UrlMapping url = urlMappingRepository.findByShortUrl(shortUrl);

        if (url == null) return Map.of();

        List<ClickEvent> events = clickEventRepository.findByUrlMapping(url);

        Map<String, Long> result = new HashMap<>();

        for (ClickEvent event : events) {

            String country = Optional.ofNullable(event.getCountry())
                    .orElse("Unknown");

            result.put(country, result.getOrDefault(country, 0L) + 1);
        }

        return result;
    }

    // ----------------------------------------------
    // HELPER METHOD
    // ----------------------------------------------
    private List<ClickAnalyticsDTO> groupClicksByDate(List<ClickEvent> events) {

        Map<LocalDate, Integer> counts = new TreeMap<>();

        for (ClickEvent event : events) {

            if (event.getClickDate() == null) continue;

            LocalDate date = event.getClickDate().toLocalDate();

            counts.put(date, counts.getOrDefault(date, 0) + 1);
        }

        List<ClickAnalyticsDTO> result = new ArrayList<>();

        for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
            result.add(new ClickAnalyticsDTO(entry.getKey(), entry.getValue()));
        }

        return result;
    }
}