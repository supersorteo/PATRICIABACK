package com.example.exellsior.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "dni")
    private String dni;

    @Column(name = "phone_intl")
    private String phoneIntl;

    @Column(name = "phone_raw")
    private String phoneRaw;

    private String plate;
    private String notes;

    @Column(name = "space_key")
    private String spaceKey;

    // Campos informativos (snapshot) del vehiculo
    private String vehicle;
    private String category;
    private Integer price;



    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonManagedReference
    private List<ClientVehicle> clientVehicles = new ArrayList<>();


    private String paymentMethod;  // "efectivo", "credito", "prepago"

    @Digits(integer = 4, fraction = 0, message = "Clover debe tener exactamente 4 dígitos")
    private Integer clover;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entry_timestamp")
    private Date entryTimestamp;

    @Column(name = "exit_timestamp")
    private Long exitTimestamp;

    @Column(name = "last_day_closed")
    private Long lastDayClosed;

    public Client() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<ClientVehicle> getClientVehicles() {
        return clientVehicles;
    }

    public void setClientVehicles(List<ClientVehicle> clientVehicles) {
        this.clientVehicles = clientVehicles;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public @Digits(integer = 4, fraction = 0, message = "Clover debe tener exactamente 4 dígitos") Integer getClover() {
        return clover;
    }

    public void setClover(@Digits(integer = 4, fraction = 0, message = "Clover debe tener exactamente 4 dígitos") Integer clover) {
        this.clover = clover;
    }

    public Date getEntryTimestamp() {
        return entryTimestamp;
    }

    public void setEntryTimestamp(Date entryTimestamp) {
        this.entryTimestamp = entryTimestamp;
    }

    public Long getExitTimestamp() {
        return exitTimestamp;
    }

    public void setExitTimestamp(Long exitTimestamp) {
        this.exitTimestamp = exitTimestamp;
    }

    public Long getLastDayClosed() {
        return lastDayClosed;
    }

    public void setLastDayClosed(Long lastDayClosed) {
        this.lastDayClosed = lastDayClosed;
    }
}