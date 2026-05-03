package com.example.exellsior.controller;

import com.example.exellsior.dto.ServiceHistoryDTO;
import com.example.exellsior.services.ServiceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service-history")
public class ServiceHistoryController {

    @Autowired
    private ServiceHistoryService serviceHistoryService;

    @PostMapping("/reset")
    public ResponseEntity<Void> resetAll() {
        serviceHistoryService.deleteAll();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<ServiceHistoryDTO>> getByDateRange(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);

        return ResponseEntity.ok(
                serviceHistoryService.getByDateRange(dateFrom, dateTo)
                        .stream()
                        .map(ServiceHistoryDTO::from)
                        .toList()
        );
    }
}
