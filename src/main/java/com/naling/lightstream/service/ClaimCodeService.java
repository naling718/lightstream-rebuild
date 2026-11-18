package com.naling.lightstream.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

@Service
public class ClaimCodeService {
    private final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final String KEY_PREFIX = "file:";
    private final int CODE_LENGTH = 4;
    private final int MAX_ATTEMPT = 10;
    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;

    // Delete the key only if it still holds the expected value
    private final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    public ClaimCodeService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public String reserve(String value, Duration ttl){
        for (int i = 0; i < MAX_ATTEMPT; i++){
            String code = generateCode();
            Boolean res = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + code, value, ttl);
            if (Boolean.TRUE.equals(res)){
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique claim code");

    }

    public String generateCode(){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++){
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public String consume(String code){
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
    }

    // For history part, delete only if it still holds the expected value
    public boolean releaseIfMatch(String code, String expectedValue){
        Long deleted = redisTemplate.execute(RELEASE_SCRIPT, List.of(KEY_PREFIX + code), expectedValue);
        return Long.valueOf(1).equals(deleted);
    }
}
