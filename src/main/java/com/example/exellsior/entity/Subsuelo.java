package com.example.exellsior.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subsuelos")
public class Subsuelo {

    @Id
    private String id;

    @Column(nullable = false)
    private String label;

    // QUITAMOS la relación con spaces del JSON (no la necesitamos al crear subsuelo)
    // La relación queda solo para consultas internas
    @OneToMany(mappedBy = "subsuelo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Space> spaces = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Space> getSpaces() {
        return spaces;
    }

    public void setSpaces(List<Space> spaces) {
        this.spaces = spaces;
    }
}
