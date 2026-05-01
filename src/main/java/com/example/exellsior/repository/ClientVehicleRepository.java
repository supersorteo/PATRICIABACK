package com.example.exellsior.repository;

import com.example.exellsior.entity.ClientVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ClientVehicleRepository extends JpaRepository<ClientVehicle, Long> {
    void deleteByClientIdIn(Collection<Long> clientIds);
}
