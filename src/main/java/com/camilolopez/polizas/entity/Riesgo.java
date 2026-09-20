package com.camilolopez.polizas.entity;

import jakarta.persistence.*;

@Entity
@Table(indexes = @Index(name = "idx_riesgo_poliza", columnList = "poliza_id"))
public class Riesgo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    public Poliza poliza;
    @Column(nullable = false, length = 200)
    public String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public EstadoRiesgo estado;

    protected Riesgo() {}
    public Riesgo(Poliza poliza, String descripcion, EstadoRiesgo estado) {
        this.poliza = poliza; this.descripcion = descripcion; this.estado = estado;
    }
}
