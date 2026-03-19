package com.url.shortener.service;

import com.url.shortener.dto.ClickEventMessageDTO;
import com.url.shortener.models.ClickEvent;
import com.url.shortener.models.UrlMapping;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    @Scheduled(fixedDelay = 1000)
    public void consumeClickEvents() {

        Object event = redisTemplate.opsForList().rightPop("click-events");
        if (event == null) return;

        ClickEventMessageDTO dto = (ClickEventMessageDTO) event;

        UrlMapping mapping = urlMappingRepository.findByShortUrl(dto.getShortUrl());

        if (mapping == null) return;

        urlMappingRepository.incrementClickCount(dto.getShortUrl());

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setClickDate(dto.getClickDate());
        clickEvent.setReferrer(dto.getReferrer());
        clickEvent.setBrowser(dto.getBrowser());
        clickEvent.setDevice(dto.getDevice());
        clickEvent.setIpAddress(dto.getIpAddress());
        clickEvent.setCountry(dto.getCountry());
        clickEvent.setUrlMapping(mapping);

        clickEventRepository.save(clickEvent);
    }
}