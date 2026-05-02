package com.example.exellsior.repository;

import com.example.exellsior.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByPeriodTypeAndPeriodKey(String periodType, String periodKey);
    Optional<Report> findByPeriodTypeAndPeriodKey(String periodType, String periodKey);
    List<Report> findByPeriodTypeAndPeriodKeyStartingWith(String periodType, String periodPrefix);

    List<Report> findByPeriodTypeAndPeriodKeyStartingWithAndDailyFinalTrue(String periodType, String periodPrefix);
    List<Report> findAllByPeriodTypeAndPeriodKey(String periodType, String periodKey);

    @Query("SELECT r FROM Report r WHERE r.periodType = 'DAILY' AND r.dailyFinal = true AND r.periodKey BETWEEN :from AND :to ORDER BY r.periodKey ASC")
    List<Report> findFinalDailyByPeriodKeyRange(@Param("from") String from, @Param("to") String to);

    @Query("""
    SELECT r
    FROM Report r
    WHERE (:periodType IS NULL OR TRIM(:periodType) = '' OR UPPER(r.periodType) = UPPER(TRIM(:periodType)))
      AND (
        :search IS NULL
        OR TRIM(:search) = ''
        OR LOWER(COALESCE(r.periodKey, '')) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
        OR LOWER(COALESCE(r.timestamp, '')) LIKE LOWER(CONCAT('%', TRIM(:search), '%'))
      )
    """)
    Page<Report> findPageByFilters(
            @Param("periodType") String periodType,
            @Param("search") String search,
            Pageable pageable
    );


}
