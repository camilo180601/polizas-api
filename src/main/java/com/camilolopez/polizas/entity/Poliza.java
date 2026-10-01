package com.camilolopez.polizas.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_poliza_tipo_estado_id", columnList = "tipo,estado,id"),
        @Index(name = "idx_poliza_tipo_id", columnList = "tipo,id"),
        @Index(name = "idx_poliza_estado_id", columnList = "estado,id")
})
public class Poliza {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public TipoPoliza tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public EstadoPoliza estado;
    @Column(nullable = false) public LocalDate fechaInicio;
    @Column(nullable = false) public LocalDate fechaFin;
    @Column(nullable = false) public Integer mesesVigenciaInicial;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal canonMensual;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal prima;
    @OneToMany(mappedBy = "poliza", fetch = FetchType.LAZY)
    public List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {}
    public Poliza(TipoPoliza tipo, EstadoPoliza estado, LocalDate inicio, int meses, BigDecimal canon) {
        this.tipo = tipo; this.estado = estado; this.fechaInicio = inicio;
        this.fechaFin = inicio.plusMonths(meses); this.mesesVigenciaInicial = meses;
        this.canonMensual = canon; this.prima = canon.multiply(BigDecimal.valueOf(meses));
    }
}
