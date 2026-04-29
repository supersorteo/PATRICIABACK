package com.example.exellsior.repository;

import com.example.exellsior.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<Space, String> {
    void deleteBySubsueloId(String subsueloId);
    boolean existsBySubsueloIdAndOccupiedTrue(String subsueloId);
    boolean existsByKeyAndSubsueloId(String key, String subsueloId);
    boolean existsByDisplayNameAndSubsueloId(String displayName, String subsueloId);
    boolean existsByOccupiedTrueAndStartTimeLessThan(Long startTime);
    long countByOccupiedTrue();
    List<Space> findByOccupiedTrueOrHoldTrueOrClientIdIsNotNullOrStartTimeIsNotNull();
    List<Space> findByClientIdIn(Collection<Long> clientIds);
}
