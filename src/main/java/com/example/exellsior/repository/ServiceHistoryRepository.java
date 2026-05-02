package com.example.exellsior.repository;

import com.example.exellsior.entity.ServiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceHistoryRepository extends JpaRepository<ServiceHistory, Long> {
    Optional<ServiceHistory> findByServiceKey(String serviceKey);

    List<ServiceHistory> findByServiceDateBetweenOrderByServiceDateDescExitTimestampDesc(LocalDate from, LocalDate to);

    void deleteByServiceDateIn(Collection<LocalDate> serviceDates);
}
