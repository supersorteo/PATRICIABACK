package com.example.exellsior.controller;

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
    public List<Client> getAll() {
        return clientService.getAllClients();
    }


    @GetMapping("/unique")
    public ResponseEntity<List<Client>> getUniqueClients() {
        return ResponseEntity.ok(clientService.getUniqueClients());
    }

    @GetMapping("/dni/{dni}/monthly-count")
    public ResponseEntity<Long> getMonthlyCountByDni(
            @PathVariable String dni,
            @RequestParam(required = false) String month
    ) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                : YearMonth.parse(month); // yyyy-MM

        return ResponseEntity.ok(clientService.getMonthlyServiceCountByDni(dni, ym));
    }

    @PostMapping("/monthly-counts")
    public ResponseEntity<Map<String, Long>> getMonthlyCountsByDnis(
            @RequestBody List<String> dnis,
            @RequestParam(required = false) String month
    ) {
        YearMonth ym = (month == null || month.isBlank())
                ? YearMonth.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                : YearMonth.parse(month); // yyyy-MM

        return ResponseEntity.ok(clientService.getMonthlyServiceCountsByDnis(dnis, ym));
    }



    @GetMapping("/{id}")
    public Client getById(@PathVariable Long id) {
        return clientService.getById(id);
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<Client> getByDni(@PathVariable String dni) {
        Client client = clientService.getByDni(dni);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @GetMapping("/dni/{dni}/reservas")
    public ResponseEntity<List<Client>> getReservationsByDni(@PathVariable String dni) {
        List<Client> reservations = clientService.getReservationsByDni(dni);

        return ResponseEntity.ok(reservations);
    }





    @PostMapping
    public Client create(@RequestBody Client client) {
        client.setId(null); // Aseguramos que sea nuevo
        return clientService.saveClient(client);
    }





    @PutMapping("/{id}")
    public Client update(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return clientService.updateClientPartially(id, updates);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/spaces/{spaceKey}/reserve")
    public ResponseEntity<Client> reserveSpace(
            @PathVariable String spaceKey,
            @RequestBody Client client
    ) {
        Client saved = clientService.reserveSpace(spaceKey, client);
        return ResponseEntity.ok(saved);
    }

    // Liberar space y resetear cliente
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
