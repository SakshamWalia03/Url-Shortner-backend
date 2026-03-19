package com.url.shortener.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String getCountryFromIp(String ip) {
        try {

            String url = "http://ip-api.com/json/" + ip;

            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                return (String) response.get("country");
            }

        } catch (Exception e) {
            return "Unknown";
        }

        return "Unknown";
    }
}