package com.url.shortener.controllers;

import com.url.shortener.dto.ClickEventMessageDTO;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.service.ClickProducer;
import com.url.shortener.service.GeoLocationService;
import com.url.shortener.service.RateLimitService;
import com.url.shortener.service.UrlMappingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlMappingService urlMappingService;
    private final ClickProducer clickProducer;
    private final GeoLocationService geoLocationService;
    private final RateLimitService rateLimitService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int HOT_THRESHOLD = 50;

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortUrl,
            HttpServletRequest request) {

        // Fetch URL mapping (Redis cache handled in service)
        UrlMapping urlMapping = urlMappingService.getOriginalUrl(shortUrl);

        if (urlMapping == null) {
            return ResponseEntity.notFound().build();
        }

        // Extract real client IP — handles Render load balancer + other reverse proxies
        String ipAddress = getClientIp(request);

        // Rate limiting — keyed on real client IP
        if (!rateLimitService.isAllowed("redirect:" + ipAddress, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // --------------------------------
        // Hot URL tracking
        // --------------------------------
        String counterKey = "click_count:" + shortUrl;

        Long clicks = redisTemplate.opsForValue().increment(counterKey);
        redisTemplate.expire(counterKey, Duration.ofHours(1));

        // Track popularity ranking
        redisTemplate.opsForZSet().incrementScore("hot_urls", shortUrl, 1);

        // Cache if hot
        if (clicks != null && clicks >= HOT_THRESHOLD) {
            redisTemplate.opsForValue().set(
                    "url:" + shortUrl,
                    urlMapping.getOriginalUrl(),
                    Duration.ofHours(2)
            );
        }

        // --------------------------------
        // Collect analytics metadata
        // --------------------------------
        String userAgent = request.getHeader("User-Agent");
        String referrer  = request.getHeader("Referer");

        String browser = detectBrowser(userAgent);
        String device  = detectDevice(userAgent);
        String country = geoLocationService.getCountryFromIp(ipAddress);

        ClickEventMessageDTO event = new ClickEventMessageDTO(
                shortUrl,
                LocalDateTime.now(),
                referrer,
                browser,
                device,
                ipAddress,
                country
        );

        // Send async analytics event
        clickProducer.sendClickEvent(event);

        // --------------------------------
        // Redirect
        // --------------------------------
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, urlMapping.getOriginalUrl());

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {

        // 1. Cloudflare sets this — most trustworthy when behind CF
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (isValidIp(cfIp)) return cfIp.trim();

        // 2. Standard reverse proxy header (Render, AWS ELB, Nginx, etc.)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String clientIp = xForwardedFor.split(",")[0].trim();
            if (isValidIp(clientIp)) return clientIp;
        }

        // 3. Nginx commonly sets this
        String xRealIp = request.getHeader("X-Real-IP");
        if (isValidIp(xRealIp)) return xRealIp.trim();

        // 4. Some proxies use this
        String xClientIp = request.getHeader("X-Client-IP");
        if (isValidIp(xClientIp)) return xClientIp.trim();

        // 5. Direct connection fallback — strip port if present (IPv4)
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && remoteAddr.contains(":") && !remoteAddr.startsWith("[")) {
            return remoteAddr.split(":")[0];
        }

        return remoteAddr;
    }

    private boolean isValidIp(String ip) {
        return ip != null
                && !ip.isBlank()
                && !ip.equalsIgnoreCase("unknown")
                && !ip.equalsIgnoreCase("undefined");
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Edg"))    return "Edge";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
        return "Other";
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }
}