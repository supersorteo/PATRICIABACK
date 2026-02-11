package com.example.exellsior.services;

import com.example.exellsior.configuration.BCrypt;
import com.example.exellsior.entity.AdminUser;
import com.example.exellsior.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final Map<String, String> activeSessions = new HashMap<>();

   /* @Transactional
    public void register(String username, String password) {
        if (adminUserRepository.count() > 0) {
            throw new RuntimeException("Ya existe un usuario administrador");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPassword(password);  // ← Guardamos en texto plano
        adminUserRepository.save(user);
    }*/

    // Login
    /*public AdminUser login(String username, String password) {
        Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        AdminUser user = userOpt.get();
        if (!user.getPassword().equals(password)) {  // ← Comparación directa
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        return user;
    }*/



    // Cambiar contraseña
   /* @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        AdminUser user = login(username, oldPassword);
        user.setPassword(newPassword);  // ← Guardamos nueva en texto plano
        adminUserRepository.save(user);
    }*/


   /* @Transactional
    public void register(String username, String password) {
        if (adminUserRepository.count() > 0) {
            throw new RuntimeException("Ya existe un usuario administrador");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password)); // Cifrar con BCrypt
        adminUserRepository.save(user);
    }*/



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
        user.setPasswordHash(passwordEncoder.encode(password.trim())); // ← Aquí se setea

        adminUserRepository.save(user);
    }

   /* public String login(String username, String password) {
        Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        // Generar token simple
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, username);

        return token;

    }*/

    public Map<String, String> login(String username, String password) {
        Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, username);

        return Map.of(
                "token", token,
                "username", username  // ← Agregamos esto
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

    // Método para validar token en endpoints protegidos
    public String validateToken(String token) {
        if (token == null || !activeSessions.containsKey(token)) {
            throw new RuntimeException("Token inválido o sesión expirada");
        }
        return activeSessions.get(token);
    }

    // Método para obtener todos los usuarios (protegido)
    public List<AdminUser> getAllUsers(String token) {
        validateToken(token);
        return adminUserRepository.findAll();
    }

    // Método para eliminar usuario (protegido)
    @Transactional
    public void deleteUser(String token, Long id) {
        validateToken(token);
        if (!adminUserRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        adminUserRepository.deleteById(id);
    }

}
