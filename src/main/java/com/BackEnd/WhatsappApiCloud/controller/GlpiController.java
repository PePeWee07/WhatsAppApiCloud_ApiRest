package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.CreateTicket;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.responseCreateTicketSuccess;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.SolutionDecisionRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/glpi")
public class GlpiController {
    
    @Autowired
    private GlpiService glpiService;

    @GetMapping("/ticket/{ticketId}")
    public TicketInfoDto getInfoTicketById(@PathVariable String ticketId) {
        return glpiService.getInfoTicketById(ticketId);
    }

    @PostMapping("/ticket")
    public ResponseEntity<responseCreateTicketSuccess> createTicket(
            @RequestBody CreateTicket payload,
            @RequestParam("whatsappPhone") String whatsAppPhone) {

        responseCreateTicketSuccess resp = glpiService.createTicket(payload, whatsAppPhone);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resp);
    }

   @PostMapping("/ticket/solution/decision")
    public ResponseEntity<Object> refusedOrAcceptedTicketSolution(@RequestBody SolutionDecisionRequest request,  @RequestParam("whatsappPhone") String whatsAppPhone) {
        Object resp = glpiService.refusedOrAcceptedSolutionTicket(request, whatsAppPhone);
        return ResponseEntity.status(HttpStatus.OK).body(resp);
    }
    
}
