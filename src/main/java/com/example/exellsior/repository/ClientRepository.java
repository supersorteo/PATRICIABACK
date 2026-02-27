package com.example.exellsior.repository;

import com.example.exellsior.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDni(String dni);
    // List<Client> findByDniList(String dni);
    List<Client> findAllByDni(String dni);

    Optional<Client> findFirstByDniOrderByIdDesc(String dni);
    List<Client> findByEntryTimestampBetween(Date from, Date to);
    List<Client> findByDniOrderByIdDesc(String dni);


    @Query("""
    SELECT COUNT(c)
    FROM Client c
    WHERE c.dni = :dni
      AND (
        (c.entryTimestamp IS NOT NULL AND c.entryTimestamp >= :fromDate AND c.entryTimestamp < :toDate)
        OR
        (c.entryTimestamp IS NULL AND c.exitTimestamp IS NOT NULL AND c.exitTimestamp >= :fromMs AND c.exitTimestamp < :toMs)
      )
""")
    long countMonthlyServicesByDni(
            @Param("dni") String dni,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("fromMs") Long fromMs,
            @Param("toMs") Long toMs
    );

    @Query("""
    SELECT c.dni, COUNT(c)
    FROM Client c
    WHERE c.dni IN :dnis
      AND c.entryTimestamp IS NOT NULL
      AND c.entryTimestamp >= :fromDate
      AND c.entryTimestamp < :toDate
    GROUP BY c.dni
""")
    List<Object[]> countMonthlyServicesByDniUsingEntryTimestamp(
            @Param("dnis") List<String> dnis,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate
    );

    @Query("""
    SELECT c.dni, COUNT(c)
    FROM Client c
    WHERE c.dni IN :dnis
      AND c.entryTimestamp IS NULL
      AND c.exitTimestamp IS NOT NULL
      AND c.exitTimestamp >= :fromMs
      AND c.exitTimestamp < :toMs
    GROUP BY c.dni
""")
    List<Object[]> countMonthlyServicesByDniUsingExitTimestamp(
            @Param("dnis") List<String> dnis,
            @Param("fromMs") Long fromMs,
            @Param("toMs") Long toMs
    );


    @Query(value = """
    SELECT c.*
    FROM clients c
    INNER JOIN (
        SELECT id
        FROM (
            SELECT
                c2.id,
                ROW_NUMBER() OVER (
                    PARTITION BY
                        CASE
                            WHEN c2.dni IS NOT NULL AND TRIM(c2.dni) <> '' THEN CONCAT('dni:', TRIM(c2.dni))
                            WHEN c2.phone_intl IS NOT NULL AND TRIM(c2.phone_intl) <> '' THEN CONCAT('phone:', REGEXP_REPLACE(c2.phone_intl, '[^0-9]', ''))
                            WHEN c2.phone_raw IS NOT NULL AND TRIM(c2.phone_raw) <> '' THEN CONCAT('phone:', REGEXP_REPLACE(c2.phone_raw, '[^0-9]', ''))
                            ELSE CONCAT('name:', LOWER(TRIM(COALESCE(c2.name, ''))))
                        END
                    ORDER BY c2.id DESC
                ) AS rn
            FROM clients c2
        ) ranked
        WHERE ranked.rn = 1
    ) u ON u.id = c.id
    ORDER BY c.id DESC
    """, nativeQuery = true)
    List<Client> findUniqueClientsLatestSnapshot();

}
