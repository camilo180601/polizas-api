package com.camilolopez.polizas.service;

import com.camilolopez.polizas.dto.RiesgoResponse;
import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.exception.*;
import com.camilolopez.polizas.integration.CoreClient;
import com.camilolopez.polizas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiesgoService {
    private final PolizaService polizaService;
    private final RiesgoRepository riesgos;
    private final CoreClient core;
    public RiesgoService(PolizaService polizaService, RiesgoRepository riesgos, CoreClient core) {
        this.polizaService = polizaService; this.riesgos = riesgos; this.core = core;
    }
    @Transactional
    public RiesgoResponse crear(Long polizaId, String descripcion) {
        Poliza p = polizaService.lock(polizaId);
        if (p.tipo != TipoPoliza.COLECTIVA || p.estado == EstadoPoliza.CANCELADA)
            throw new BusinessRuleException("No se puede agregar riesgo a esta póliza");
        Riesgo r = riesgos.saveAndFlush(new Riesgo(p, descripcion.trim(), EstadoRiesgo.ACTIVO));
        core.actualizacion(polizaId, "AGREGAR_RIESGO");
        return RiesgoResponse.from(r);
    }
    @Transactional
    public RiesgoResponse cancelar(Long id) {
        Long polizaId = riesgos.findPolizaId(id).orElseThrow(() -> new NotFoundException("Riesgo no encontrado"));
        polizaService.lock(polizaId);
        Riesgo r = riesgos.findById(id).orElseThrow(() -> new NotFoundException("Riesgo no encontrado"));
        if (r.estado == EstadoRiesgo.CANCELADO) return RiesgoResponse.from(r);
        r.estado = EstadoRiesgo.CANCELADO;
        riesgos.saveAndFlush(r);
        core.actualizacion(polizaId, "CANCELAR_RIESGO");
        return RiesgoResponse.from(r);
    }
}
