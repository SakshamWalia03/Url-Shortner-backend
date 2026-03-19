package com.url.shortener.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpamProtectionService {
    private static final List<String> BLOCKED_DOMAINS = List.of(
            "malware.com",
            "phishing.com",
            "spamlink.net"
    );

    public boolean isBlocked(String url) {
        return BLOCKED_DOMAINS.stream()
                .anyMatch(url::contains);
    }
}