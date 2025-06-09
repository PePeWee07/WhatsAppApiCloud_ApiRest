package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/glpi")
public class GlpiController {
    
    @Autowired
    private GlpiService glpiService;

    @GetMapping("/ticket/{ticketId}")
    public String getInfoTicketById(@PathVariable String ticketId) {
        return glpiService.getInfoTicketById(ticketId);
    }
    
}
