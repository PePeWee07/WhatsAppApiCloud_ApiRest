package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final RedisConnectionFactory redisConnectionFactory;


    public CacheController(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * Elimina todas las claves de Redis (FLUSHALL).
     * Devuelve 200 OK si se completó sin errores.
     */
    @DeleteMapping("/flush")
    public ResponseEntity<Void> flushAll() {
        RedisConnection conn = null;
        try {
            conn = redisConnectionFactory.getConnection();
            conn.serverCommands().flushAll();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return ResponseEntity.ok().build();
    }
}
