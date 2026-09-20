package com.camilolopez.polizas.dto;
import com.camilolopez.polizas.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PolizaResponse(Long id, TipoPoliza tipo, EstadoPoliza estado, LocalDate fechaInicio,
                             LocalDate fechaFin, Integer mesesVigenciaInicial, BigDecimal canonMensual, BigDecimal prima) {
    public static PolizaResponse from(Poliza p) {
        return new PolizaResponse(p.id, p.tipo, p.estado, p.fechaInicio, p.fechaFin,
                p.mesesVigenciaInicial, p.canonMensual, p.prima);
    }
}
