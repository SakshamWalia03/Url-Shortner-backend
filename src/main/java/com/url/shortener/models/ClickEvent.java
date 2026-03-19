package com.url.shortener.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entity representing a click on a shortened URL.
 * Stores analytics information such as browser, device,
 * IP address, country, and traffic source.
 */
@Entity
@Data
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime clickDate;
    private String ipAddress;
    private String country;
    private String browser;
    private String device;
    private String referrer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_mapping_id", nullable = false)
    @ToString.Exclude
    private UrlMapping urlMapping;
}