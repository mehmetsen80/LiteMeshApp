package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.service.CodeCacheService;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CodeCacheServiceImpl implements CodeCacheService {
    private static final String CODE_PREFIX = "oauth:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    @Override
    public Mono<Boolean> isCodeUsed(String code) {
        return redisTemplate.hasKey(CODE_PREFIX + code);
    }
    
    @Override
    public Mono<Boolean> markCodeAsUsed(String code) {
        return redisTemplate.opsForValue()
            .setIfAbsent(CODE_PREFIX + code, "used", CODE_TTL);
    }
} 