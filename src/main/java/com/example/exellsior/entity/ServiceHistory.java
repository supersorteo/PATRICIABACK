package com.example.exellsior.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "service_history")
public class ServiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_key", nullable = false, unique = true, length = 255)
    private String serviceKey;

    @Column(name = "source_client_id")
    private Long sourceClientId;

    private String code;
    private String name;
    private String dni;

    @Column(name = "phone_intl")
    private String phoneIntl;

    @Column(name = "phone_raw")
    private String phoneRaw;

    private String plate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "space_key")
    private String spaceKey;

    private String vehicle;
    private String category;
    private Integer price;

    @Column(name = "payment_method")
    private String paymentMethod;

    private Integer clover;

    @Column(name = "entry_timestamp_ms")
    private Long entryTimestamp;

    @Column(name = "exit_timestamp_ms", nullable = false)
    private Long exitTimestamp;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "archived_at", nullable = false)
    private Long archivedAt;

    @Column(name = "archived_by", nullable = false, length = 64)
    private String archivedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public Long getSourceClientId() {
        return sourceClientId;
    }

    public void setSourceClientId(Long sourceClientId) {
        this.sourceClientId = sourceClientId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getPhoneIntl() {
        return phoneIntl;
    }

    public void setPhoneIntl(String phoneIntl) {
        this.phoneIntl = phoneIntl;
    }

    public String getPhoneRaw() {
        return phoneRaw;
    }

    public void setPhoneRaw(String phoneRaw) {
        this.phoneRaw = phoneRaw;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getClover() {
        return clover;
    }

    public void setClover(Integer clover) {
        this.clover = clover;
    }

    public Long getEntryTimestamp() {
        return entryTimestamp;
    }

    public void setEntryTimestamp(Long entryTimestamp) {
        this.entryTimestamp = entryTimestamp;
    }

    public Long getExitTimestamp() {
        return exitTimestamp;
    }

    public void setExitTimestamp(Long exitTimestamp) {
        this.exitTimestamp = exitTimestamp;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public Long getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Long archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivedBy() {
        return archivedBy;
    }

    public void setArchivedBy(String archivedBy) {
        this.archivedBy = archivedBy;
    }
}
