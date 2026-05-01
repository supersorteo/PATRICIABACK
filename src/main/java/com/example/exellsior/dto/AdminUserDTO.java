package com.example.exellsior.dto;

import com.example.exellsior.entity.AdminUser;

public record AdminUserDTO(Long id, String username) {
    public static AdminUserDTO from(AdminUser user) {
        return new AdminUserDTO(user.getId(), user.getUsername());
    }
}
