package com.url.shortener.repository;

import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for analytics queries on ClickEvent.
 * Returns raw click events (no aggregation).
 */
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    // --------------------------------------------
    // All clicks for a specific URL
    // --------------------------------------------
    List<ClickEvent> findByUrlMapping(UrlMapping urlMapping);

    // --------------------------------------------
    // Clicks for a URL within date range
    // --------------------------------------------
    List<ClickEvent> findByUrlMappingAndClickDateBetween(
            UrlMapping urlMapping,
            LocalDateTime start,
            LocalDateTime end
    );

    // --------------------------------------------
    // Clicks for multiple URLs within date range
    // --------------------------------------------
    List<ClickEvent> findByUrlMappingInAndClickDateBetween(
            List<UrlMapping> urlMappings,
            LocalDateTime start,
            LocalDateTime end
    );
}