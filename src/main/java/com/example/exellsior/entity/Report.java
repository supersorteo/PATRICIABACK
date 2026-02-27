package com.example.exellsior.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;

@Entity
@Table(name = "reports")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String timestamp; // ISO string from frontend

    @Column(name = "period_type", nullable = false)
    private String periodType; // DAILY | MONTHLY

    @Column(name = "period_key", nullable = false)
    private String periodKey; // DAILY: yyyy-MM-dd | MONTHLY: yyyy-MM

    @Column(name = "total_spaces")
    private int totalSpaces;

    @Column(name = "occupied_spaces")
    private int occupiedSpaces;

    @Column(name = "free_spaces")
    private int freeSpaces;

    @Column(name = "occupancy_rate")
    private int occupancyRate;

    @Lob
    @Column(length = Integer.MAX_VALUE, name = "subsuelo_stats") // Fuerza LONGTEXT
    private String subsueloStats;

    @Lob
    @Column(length = Integer.MAX_VALUE, name = "time_stats") // Fuerza LONGTEXT
    private String timeStats;

    @Lob
    @Column(length = Integer.MAX_VALUE, name = "filtered_clients") // Fuerza LONGTEXT para array largo
    private String filteredClients;

    @Lob
    @Column(length = Integer.MAX_VALUE, name = "payment_amounts")
    private String paymentAmounts;

    @Column(name = "total_cobrado")
    private Long totalCobrado;

    @Column(name = "daily_final")
    private Boolean dailyFinal = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }


    public int getTotalSpaces() {
        return totalSpaces;
    }

    public void setTotalSpaces(int totalSpaces) {
        this.totalSpaces = totalSpaces;
    }

    public int getOccupiedSpaces() {
        return occupiedSpaces;
    }

    public void setOccupiedSpaces(int occupiedSpaces) {
        this.occupiedSpaces = occupiedSpaces;
    }

    public int getFreeSpaces() {
        return freeSpaces;
    }

    public void setFreeSpaces(int freeSpaces) {
        this.freeSpaces = freeSpaces;
    }

    public int getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(int occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public String getSubsueloStats() {
        return subsueloStats;
    }

    public void setSubsueloStats(String subsueloStats) {
        this.subsueloStats = subsueloStats;
    }

    public String getTimeStats() {
        return timeStats;
    }

    public void setTimeStats(String timeStats) {
        this.timeStats = timeStats;
    }

    public String getFilteredClients() {
        return filteredClients;
    }

    public void setFilteredClients(String filteredClients) {
        this.filteredClients = filteredClients;
    }

    public String getPaymentAmounts() {
        return paymentAmounts;
    }

    public void setPaymentAmounts(String paymentAmounts) {
        this.paymentAmounts = paymentAmounts;
    }

    public Long getTotalCobrado() {
        return totalCobrado;
    }

    public void setTotalCobrado(Long totalCobrado) {
        this.totalCobrado = totalCobrado;
    }

    public Boolean getDailyFinal() {
        return dailyFinal;
    }

    public void setDailyFinal(Boolean dailyFinal) {
        this.dailyFinal = dailyFinal;
    }
}
