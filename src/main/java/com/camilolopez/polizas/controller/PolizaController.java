package com.camilolopez.polizas.controller;

import com.camilolopez.polizas.dto.*;
import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated
public class PolizaController {
    private final PolizaService polizas;
    private final RiesgoService riesgos;
    public PolizaController(PolizaService polizas, RiesgoService riesgos) {
        this.polizas = polizas; this.riesgos = riesgos;
    }
    @GetMapping("/polizas")
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                        @RequestParam(required = false) EstadoPoliza estado) {
        return polizas.listar(tipo, estado);
    }
    @GetMapping("/polizas/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable @Positive Long id) { return polizas.riesgos(id); }
    @PostMapping("/polizas/{id}/renovar")
    public PolizaResponse renovar(@PathVariable @Positive Long id, @Valid @RequestBody RenovarPolizaRequest body) {
        return polizas.renovar(id, body.ipcPorcentaje());
    }
    @PostMapping("/polizas/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable @Positive Long id) { return polizas.cancelar(id); }
    @PostMapping("/polizas/{id}/riesgos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public RiesgoResponse crearRiesgo(@PathVariable @Positive Long id, @Valid @RequestBody CrearRiesgoRequest body) {
        return riesgos.crear(id, body.descripcion());
    }
}
