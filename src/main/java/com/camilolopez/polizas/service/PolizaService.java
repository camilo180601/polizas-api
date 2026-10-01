package com.camilolopez.polizas.service;

import com.camilolopez.polizas.dto.*;
import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.exception.*;
import com.camilolopez.polizas.integration.CoreClient;
import com.camilolopez.polizas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.math.*;
import java.util.List;

@Service
public class PolizaService {
    private final PolizaRepository polizas;
    private final RiesgoRepository riesgos;
    private final CoreClient core;
    public PolizaService(PolizaRepository polizas, RiesgoRepository riesgos, CoreClient core) {
        this.polizas = polizas; this.riesgos = riesgos; this.core = core;
    }
    @Transactional(readOnly = true)
    public CursorPage<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado, Long afterId, int limit) {
        List<Poliza> found = polizas.buscar(tipo, estado, afterId, PageRequest.of(0, limit + 1));
        boolean hasMore = found.size() > limit;
        List<Poliza> current = hasMore ? found.subList(0, limit) : found;
        List<PolizaResponse> items = current.stream().map(PolizaResponse::from).toList();
        Long nextCursor = hasMore ? current.get(current.size() - 1).id : null;
        return new CursorPage<>(items, nextCursor);
    }
    @Transactional(readOnly = true)
    public List<RiesgoResponse> riesgos(Long id) {
        if (!polizas.existsById(id)) throw new NotFoundException("Póliza no encontrada");
        return riesgos.findByPolizaIdOrderByIdAsc(id).stream().map(RiesgoResponse::from).toList();
    }
    @Transactional
    public PolizaResponse renovar(Long id, BigDecimal ipc) {
        if (ipc.scale() > 4) throw new IllegalArgumentException("IPC con más de cuatro decimales");
        Poliza p = lock(id);
        if (p.estado == EstadoPoliza.CANCELADA) throw new BusinessRuleException("No se puede renovar una póliza cancelada");
        BigDecimal factor = BigDecimal.ONE.add(ipc.movePointLeft(2));
        BigDecimal canon = p.canonMensual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal prima = canon.multiply(BigDecimal.valueOf(p.mesesVigenciaInicial));
        if (canon.precision() - canon.scale() > 17 || prima.precision() - prima.scale() > 17)
            throw new IllegalArgumentException("Resultado monetario fuera de rango");
        p.fechaInicio = p.fechaFin;
        p.fechaFin = p.fechaInicio.plusMonths(p.mesesVigenciaInicial);
        p.canonMensual = canon; p.prima = prima; p.estado = EstadoPoliza.RENOVADA;
        polizas.saveAndFlush(p);
        core.actualizacion(id, "RENOVAR");
        return PolizaResponse.from(p);
    }
    @Transactional
    public PolizaResponse cancelar(Long id) {
        Poliza p = lock(id);
        if (p.estado == EstadoPoliza.CANCELADA) return PolizaResponse.from(p);
        p.estado = EstadoPoliza.CANCELADA;
        List<Riesgo> hijos = riesgos.findByPolizaIdOrderByIdAsc(id);
        hijos.forEach(r -> r.estado = EstadoRiesgo.CANCELADO);
        riesgos.saveAllAndFlush(hijos);
        polizas.saveAndFlush(p);
        core.actualizacion(id, "CANCELAR_POLIZA");
        return PolizaResponse.from(p);
    }
    public Poliza lock(Long id) {
        return polizas.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Póliza no encontrada"));
    }
}
