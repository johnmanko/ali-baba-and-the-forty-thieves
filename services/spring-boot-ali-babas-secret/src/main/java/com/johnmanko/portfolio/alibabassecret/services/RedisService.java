package com.johnmanko.portfolio.alibabassecret.services;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Write to Redis
    @Async
    public void saveToRedis(String key, Integer value, long timeoutInSeconds) {
        redisTemplate.opsForValue().set(key, value.toString(), timeoutInSeconds, TimeUnit.SECONDS);
    }

    // Read from Redis
    @Async
    public CompletableFuture<Optional<Integer>> getFromRedis(String key) {
        return CompletableFuture.supplyAsync(() -> {
            String v = redisTemplate.opsForValue().get(key);
            if (v == null) {
                return Optional.empty();
            }
            return Optional.of(Integer.valueOf(v));
        });
    }

    // Delete from Redis
    @Async
    public void deleteFromRedis(String key) {
        redisTemplate.delete(key);
    }
}