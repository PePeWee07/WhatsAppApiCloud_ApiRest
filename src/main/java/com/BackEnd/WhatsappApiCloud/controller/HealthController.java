package com.BackEnd.WhatsappApiCloud.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // ======================================================
    // Health Check
    // ======================================================
    @CrossOrigin(origins = "https://ia-sp-backoffice.ucatolica.cue.ec")
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "API is running.");

        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy, h:mm:ss a");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Guayaquil"));
        
        response.put("timestamp", sdf.format(new Date()));

        return ResponseEntity.ok(response);
    }

}
