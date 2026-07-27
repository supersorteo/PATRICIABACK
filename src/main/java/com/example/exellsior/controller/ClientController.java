package com.example.exellsior.controller;

import com.example.exellsior.dto.ClientDTO;
import com.example.exellsior.dto.ClientImportItemDTO;
import com.example.exellsior.dto.ClientImportResultDTO;
import com.example.exellsior.dto.PagedResponse;
import com.example.exellsior.entity.Client;
import com.example.exellsior.services.ClientService;
import com.example.exellsior.services.OperationalTimeService;
import com.example.exellsior.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private OperationalTimeService operationalTimeService;

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
                ? YearMonth.now(operationalTimeService.getBusinessZone())
                : YearMonth.parse(month);
        return ResponseEntity.ok(clientService.getMonthlyServiceCountByDni(dni, ym));
    }

    @PostMapping("/monthly-counts")
    public ResponseEntity<Map<String, Long>> getMonthlyCountsByDnis(
            @RequestBody List<String> dnis,
            @RequestParam(required = false) String month
    ) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(operationalTimeService.getBusinessZone())
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
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);
        LocalDate today = LocalDate.now(operationalTimeService.getBusinessZone());

        List<ClientDTO> result = new ArrayList<>();

        // Días pasados: reporte final → si no existe, ServiceHistory
        if (dateFrom.isBefore(today)) {
            LocalDate pastTo = dateTo.isBefore(today) ? dateTo : today.minusDays(1);
            result.addAll(reportService.getClientsFromDailyReportsByDateRange(dateFrom, pastTo));
        }

        // Hoy: clientes vivos en la BD
        if (!dateTo.isBefore(today) && !dateFrom.isAfter(today)) {
            clientService.getByDateRange(today, today)
                    .stream().map(ClientDTO::fromSummary).forEach(result::add);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<ClientDTO> getByDni(@PathVariable String dni) {
        Client client = clientService.getByDni(dni);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ClientDTO.fromSummary(client));
    }

    @PutMapping("/dni/{dni}/vehicles")
    public ResponseEntity<Void> updateVehiclesByDni(
            @PathVariable String dni,
            @RequestBody List<Map<String, Object>> vehiclesPayload
    ) {
        clientService.updateVehiclesForAllByDni(dni, vehiclesPayload);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/import")
    public ResponseEntity<ClientImportResultDTO> importClients(
            @RequestBody List<ClientImportItemDTO> clients,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "true") boolean skipExisting
    ) {
        return ResponseEntity.ok(clientService.importBaseClients(clients, dryRun, skipExisting));
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

    @DeleteMapping("/{id}/service")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        clientService.deleteService(id);
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
