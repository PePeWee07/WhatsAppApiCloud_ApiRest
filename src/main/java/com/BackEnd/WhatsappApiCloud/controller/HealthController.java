package com.BackEnd.WhatsappApiCloud.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // ======================================================
    //   Health Check
    // ======================================================
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "API is running.");
        response.put("timestamp", new SimpleDateFormat("M/d/yyyy, h:mm:ss a").format(new Date()));
        return ResponseEntity.ok(response);
    }
    
}
