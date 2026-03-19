package com.url.shortener.service;

import com.url.shortener.dto.ClickEventMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer responsible for sending click events to Redis queue.
 * These events are later processed asynchronously by a consumer.
 */
@Service
@RequiredArgsConstructor
public class ClickProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis list used as a queue for click events.
     */
    private static final String CLICK_QUEUE = "click-events";

    /**
     * Push a click event message to Redis queue.
     *
     * @param clickEvent click event message DTO
     */
    public void sendClickEvent(ClickEventMessageDTO clickEvent) {
        redisTemplate.opsForList().rightPush(CLICK_QUEUE, clickEvent);
    }
}