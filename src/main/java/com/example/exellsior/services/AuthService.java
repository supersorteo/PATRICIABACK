package com.example.exellsior.services;

import com.example.exellsior.entity.AdminUser;
import com.example.exellsior.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, String> activeSessions = new HashMap<>();

    @Value("${app.auth.default-admin.username:ExellssiorSDB}")
    private String defaultAdminUsername;

    @Value("${app.auth.default-admin.password:Exellssior123}")
    private String defaultAdminPassword;

    @Transactional
    public void register(String username, String password) {
        if (adminUserRepository.count() > 0) {
            throw new RuntimeException("Ya existe un usuario administrador");
        }

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username es obligatorio");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password es obligatorio");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(password.trim()));
        adminUserRepository.save(user);
    }

    @Transactional
    public AdminUser ensureDefaultAdminUserIfMissing() {
        String normalizedUsername = defaultAdminUsername != null ? defaultAdminUsername.trim() : "";
        String normalizedPassword = defaultAdminPassword != null ? defaultAdminPassword.trim() : "";

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            throw new RuntimeException("Las credenciales del admin por defecto no estan configuradas");
        }

        Optional<AdminUser> existing = adminUserRepository.findByUsername(normalizedUsername);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (adminUserRepository.count() > 0) {
            throw new RuntimeException("Ya existen administradores en la base de datos");
        }

        AdminUser user = new AdminUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        return adminUserRepository.saveAndFlush(user);
    }

    @Transactional
    public Map<String, String> login(String username, String password) {
        String normalizedUsername = username != null ? username.trim() : "";
        String normalizedPassword = password != null ? password.trim() : "";
        String configuredDefaultUsername = defaultAdminUsername != null ? defaultAdminUsername.trim() : "";
        String configuredDefaultPassword = defaultAdminPassword != null ? defaultAdminPassword.trim() : "";

        Optional<AdminUser> userOpt = adminUserRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()
                && adminUserRepository.count() == 0
                && normalizedUsername.equals(configuredDefaultUsername)
                && normalizedPassword.equals(configuredDefaultPassword)) {
            userOpt = Optional.of(ensureDefaultAdminUserIfMissing());
        }

        if (userOpt.isEmpty() || !passwordEncoder.matches(normalizedPassword, userOpt.get().getPasswordHash())) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, normalizedUsername);

        return Map.of(
                "token", token,
                "username", normalizedUsername
        );
    }

    @Transactional
    public void changePassword(String token, String oldPassword, String newPassword) {
        String username = validateToken(token);
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
    }

    public void logout(String token) {
        activeSessions.remove(token);
    }

    public String validateToken(String token) {
        if (token == null || !activeSessions.containsKey(token)) {
            throw new RuntimeException("Token inválido o sesión expirada");
        }
        return activeSessions.get(token);
    }

    public List<AdminUser> getAllUsers(String token) {
        validateToken(token);
        return adminUserRepository.findAll();
    }

    @Transactional
    public void deleteUser(String token, Long id) {
        validateToken(token);
        if (!adminUserRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        adminUserRepository.deleteById(id);
    }
}
