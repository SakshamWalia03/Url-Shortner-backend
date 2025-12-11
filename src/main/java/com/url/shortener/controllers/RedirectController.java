package com.url.shortener.controllers;

import com.url.shortener.models.UrlMapping;
import com.url.shortener.service.UrlMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling URL redirection.
 * When a short URL is accessed, it redirects to the original URL
 * and records the click in the database.
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlMappingService urlMappingService;

    /**
     * Redirects to the original URL associated with the given short URL.
     * Increments click count and records a ClickEvent.
     *
     * @param shortUrl The short URL identifier
     * @return 302 Found with Location header if URL exists, 404 Not Found otherwise
     */
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl) {
        // Retrieve the original URL from the service
        UrlMapping urlMapping = urlMappingService.getOriginalUrl(shortUrl);

        if (urlMapping != null) {
            // Set the Location header to redirect
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, urlMapping.getOriginalUrl());

            // Return 302 Found to redirect the client
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        }

        // Return 404 if the short URL does not exist
        return ResponseEntity.notFound().build();
    }
}