package com.example.exellsior.controller;

import com.example.exellsior.entity.AdminUser;
import com.example.exellsior.repository.AdminUserRepository;
import com.example.exellsior.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Username y password son obligatorios");
        }

        try {
            authService.register(username, password);
            return ResponseEntity.ok("Usuario creado exitosamente");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Ya existe")) {
                return ResponseEntity.status(409).body("Ya existe un usuario administrador");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        try {
            Map<String, String> response = authService.login(body.get("username"), body.get("password"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestHeader("Authorization") String authHeader,
                                                 @RequestBody Map<String, String> body) {
        String token = authHeader.replace("Bearer ", "");
        authService.changePassword(token, body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok("Contraseña cambiada");
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUser>> getAllUsers() {
        return ResponseEntity.ok(adminUserRepository.findAll());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUser> getUserById(@PathVariable Long id) {
        Optional<AdminUser> userOpt = adminUserRepository.findById(Objects.requireNonNull(id));
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!adminUserRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        adminUserRepository.deleteById(id);
        return ResponseEntity.ok("Usuario eliminado correctamente");
    }
}
