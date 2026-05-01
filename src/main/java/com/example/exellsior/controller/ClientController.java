package com.example.exellsior.controller;

import com.example.exellsior.dto.ClientDTO;
import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Client;
import com.example.exellsior.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping
    public List<ClientDTO> getAll() {
        return clientService.getAllClients().stream().map(ClientDTO::fromSummary).toList();
    }

    @GetMapping("/unique")
    public ResponseEntity<List<ClientDTO>> getUniqueClients() {
        return ResponseEntity.ok(clientService.getUniqueClients().stream().map(ClientDTO::fromSummary).toList());
    }

    @GetMapping("/unique/page")
    public ResponseEntity<PagedResponse<ClientDTO>> getUniqueClientsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String search
    ) {
        PagedResponse<Client> response = clientService.getUniqueClientsPage(search, page, size);
        List<ClientDTO> dtos = response.getContent().stream().map(ClientDTO::fromSummary).toList();
        return ResponseEntity.ok(new PagedResponse<>(dtos, response.getPage(), response.getSize(),
                response.getTotalElements(), response.getTotalPages(), response.isFirst(), response.isLast()));
    }

    @GetMapping("/dni/{dni}/monthly-count")
    public ResponseEntity<Long> getMonthlyCountByDni(
            @PathVariable String dni,
            @RequestParam(required = false) String month
    ) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                : YearMonth.parse(month);
        return ResponseEntity.ok(clientService.getMonthlyServiceCountByDni(dni, ym));
    }

    @PostMapping("/monthly-counts")
    public ResponseEntity<Map<String, Long>> getMonthlyCountsByDnis(
            @RequestBody List<String> dnis,
            @RequestParam(required = false) String month
    ) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                : YearMonth.parse(month);
        return ResponseEntity.ok(clientService.getMonthlyServiceCountsByDnis(dnis, ym));
    }

    @GetMapping("/{id}")
    public ClientDTO getById(@PathVariable Long id) {
        return ClientDTO.from(clientService.getById(id));
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<ClientDTO>> getByDateRange(
            @RequestParam String from,
            @RequestParam String to
    ) {
        java.time.LocalDate dateFrom = java.time.LocalDate.parse(from);
        java.time.LocalDate dateTo = java.time.LocalDate.parse(to);
        return ResponseEntity.ok(
                clientService.getByDateRange(dateFrom, dateTo)
                        .stream().map(ClientDTO::fromSummary).toList()
        );
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<ClientDTO> getByDni(@PathVariable String dni) {
        Client client = clientService.getByDni(dni);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ClientDTO.fromSummary(client));
    }

    @GetMapping("/dni/{dni}/reservas")
    public ResponseEntity<List<ClientDTO>> getReservationsByDni(@PathVariable String dni) {
        return ResponseEntity.ok(
                clientService.getReservationsByDni(dni).stream().map(ClientDTO::from).toList()
        );
    }

    @PostMapping
    public ClientDTO create(@RequestBody Client client) {
        client.setId(null);
        return ClientDTO.from(clientService.saveClient(client));
    }

    @PutMapping("/{id}")
    public ClientDTO update(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return ClientDTO.from(clientService.updateClientPartially(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/spaces/{spaceKey}/reserve")
    public ResponseEntity<ClientDTO> reserveSpace(
            @PathVariable String spaceKey,
            @RequestBody Client client
    ) {
        return ResponseEntity.ok(ClientDTO.from(clientService.reserveSpace(spaceKey, client)));
    }

    @PutMapping("/spaces/{spaceKey}/release")
    public ResponseEntity<Void> releaseSpace(@PathVariable String spaceKey) {
        clientService.releaseSpace(spaceKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Void> resetAllData() {
        clientService.resetAllData();
        return ResponseEntity.noContent().build();
    }
}
