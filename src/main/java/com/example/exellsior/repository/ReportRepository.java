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

    List<Report> findByReportTypeIsNull();

    List<Report> findByPeriodTypeAndPeriodKeyStartingWithAndReportType(
            String periodType, String periodKeyPrefix, String reportType);

    Optional<Report> findByPeriodTypeAndPeriodKeyAndReportType(
            String periodType, String periodKey, String reportType);

    @Query("""
    SELECT MAX(r.periodKey)
    FROM Report r
    WHERE r.periodType = 'DAILY'
      AND r.dailyFinal = true
    """)
    String findMaxClosedDailyPeriodKey();

    @Query("SELECT r FROM Report r WHERE r.periodType = 'DAILY' AND r.dailyFinal = true AND r.periodKey BETWEEN :from AND :to ORDER BY r.periodKey ASC")
    List<Report> findFinalDailyByPeriodKeyRange(@Param("from") String from, @Param("to") String to);

    @Query("""
    SELECT r
    FROM Report r
    WHERE (:reportType IS NULL OR TRIM(:reportType) = '' OR UPPER(r.reportType) = UPPER(TRIM(:reportType)))
      AND (:dateFrom IS NULL OR TRIM(:dateFrom) = '' OR
           CASE WHEN r.periodType = 'MONTHLY' THEN CONCAT(COALESCE(r.periodKey, ''), '-01')
                ELSE COALESCE(r.periodKey, '') END >= TRIM(:dateFrom))
      AND (:dateTo IS NULL OR TRIM(:dateTo) = '' OR
           CASE WHEN r.periodType = 'MONTHLY' THEN CONCAT(COALESCE(r.periodKey, ''), '-01')
                ELSE COALESCE(r.periodKey, '') END <= TRIM(:dateTo))
    ORDER BY r.id DESC
    """)
    Page<Report> findPageByFilters(
            @Param("reportType") String reportType,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo,
            Pageable pageable
    );


}
