package com.example.exellsior.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

@Entity
@Table(name = "spaces")
public class Space {

    @Id
    @Column(name = "space_key", unique = true)
    private String key;

    @Column(name = "subsuelo_id", nullable = false)
    private String subsueloId;

    @Column(nullable = false)
    private boolean occupied = false;

    @Column(nullable = false)
    private boolean hold = false;

    @Column(name = "client_id")
    private Long clientId;


    // @Column(name = "client_id")
    //private String clientId;

    @Column(name = "start_time")
    private Long startTime;

    @Column(name = "display_name")
    private String displayName;

    // Relación con subsuelo (solo para consultas, no para insertar)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subsuelo_id", insertable = false, updatable = false)
    @JsonIgnore
    private Subsuelo subsuelo;

    // Relación con cliente (solo para consultas)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    //@JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Client client;

    // Constructores
    public Space() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSubsueloId() {
        return subsueloId;
    }

    public void setSubsueloId(String subsueloId) {
        this.subsueloId = subsueloId;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        this.hold = hold;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Subsuelo getSubsuelo() {
        return subsuelo;
    }

    public void setSubsuelo(Subsuelo subsuelo) {
        this.subsuelo = subsuelo;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}