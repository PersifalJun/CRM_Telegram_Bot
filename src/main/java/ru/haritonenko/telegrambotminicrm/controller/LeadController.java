package ru.haritonenko.telegrambotminicrm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.telegrambotminicrm.dto.LeadRequest;
import ru.haritonenko.telegrambotminicrm.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leads")
public class LeadController {

    private final NotificationService notificationService;

    @Value("${lead.api-key:changeme}")
    private String apiKey;

    @PostMapping
    public ResponseEntity<?> acceptLead(
            @RequestHeader("X-Api-Key") String key,
            @Valid @RequestBody LeadRequest lead) {

        if (!apiKey.equals(key)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        boolean created = notificationService.broadcastLead(lead);
        if (!created) {
            return ResponseEntity.status(409).body("Lead with this phone already exists");
        }

        return ResponseEntity.status(201).build();
    }
}
