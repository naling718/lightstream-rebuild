package com.naling.lightstream.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClaimCodeServiceTest {
    private ClaimCodeService service;

    @Mock
    // A fake service, only return 0, null, false, or an empty collection
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOperations;

    @BeforeEach
    void setUp(){
        service = new ClaimCodeService(redisTemplate);
    }

    @Test
    void reserveStoresValueOnceAndReturnItsCode(){
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(),anyString(), any(Duration.class))).thenReturn(true);
        // Act
        String code = service.reserve("hello", Duration.ofMinutes(15));
        // Assert, using all plain values
        verify(valueOperations, times(1)).setIfAbsent("file:" + code, "hello", Duration.ofMinutes(15));
        assertEquals(4, code.length());
    }

    @Test
    void reserveRetriesWhenCodeIsTaken(){
        // Arrange: the first call returns false, the second call returns true
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false, true);
        // Act
        String code = service.reserve("hello", Duration.ofMinutes(15));
        // Assert, using all matchers
        verify(valueOperations, times(2)).setIfAbsent(anyString(), eq("hello"), eq(Duration.ofMinutes(15)));
        assertEquals(4, code.length());
    }

    @Test
    void reserveGivesUpAfterMaxAttempts(){
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        // Act and Assert
        assertThrows(IllegalStateException.class, () -> service.reserve("hello", Duration.ofMinutes(15)));
        verify(valueOperations, times(10)).setIfAbsent(anyString(), eq("hello"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void consumeReturnsStoredValue(){
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("file:AB1D")).thenReturn("hello");
        // Act
        String text = service.consume("AB1D");
        // Assert
        assertEquals("hello", text);
    }

    @Test
    void consumeUnknownCodeReturnsNull(){
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("file:AB1D")).thenReturn(null);
        // Act
        String text = service.consume("AB1D");
        // Assert
        assertNull(text);
    }

    @Test
    void releaseReturnsTrueWhenValueMatches(){
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString())).thenReturn(1L);
        // Act
        boolean res = service.releaseIfMatch("file:A8SH", "hello");
        // Assert
        assertTrue(res);
    }

    @Test
    void releaseReturnsFalseWhenValueNotMatch(){
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString())).thenReturn(0L);
        // Act
        boolean res = service.releaseIfMatch("file:A8SH", "hello");
        // Assert
        assertFalse(res);
    }
}
