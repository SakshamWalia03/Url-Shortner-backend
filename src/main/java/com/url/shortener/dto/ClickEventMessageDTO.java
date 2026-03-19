package com.url.shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO representing a single click event.
 * This object is pushed to Redis for asynchronous processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessageDTO implements Serializable {

    private String shortUrl;
    private LocalDateTime clickDate;
    private String referrer;
    private String browser;
    private String device;
    private String ipAddress;
    private String country;
}