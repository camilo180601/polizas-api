package com.camilolopez.polizas.dto;
import com.camilolopez.polizas.entity.*;
public record RiesgoResponse(Long id, Long polizaId, String descripcion, EstadoRiesgo estado) {
    public static RiesgoResponse from(Riesgo r) {
        return new RiesgoResponse(r.id, r.poliza.id, r.descripcion, r.estado);
    }
}
