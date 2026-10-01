package com.camilolopez.polizas.controller;

import com.camilolopez.polizas.dto.*;
import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController @Validated
public class PolizaController {
    private final PolizaService polizas;
    private final RiesgoService riesgos;
    public PolizaController(PolizaService polizas, RiesgoService riesgos) {
        this.polizas = polizas; this.riesgos = riesgos;
    }
    @GetMapping("/polizas")
    public ResponseEntity<List<PolizaResponse>> listar(
            @RequestParam(required = false) TipoPoliza tipo,
            @RequestParam(required = false) EstadoPoliza estado,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @PositiveOrZero Long afterId) {
        CursorPage<PolizaResponse> page = polizas.listar(tipo, estado, afterId, limit);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Has-More", Boolean.toString(page.hasMore()));
        if (page.nextCursor() != null) {
            headers.add("X-Next-Cursor", page.nextCursor().toString());
            String next = ServletUriComponentsBuilder.fromCurrentRequest()
                    .replaceQueryParam("afterId", page.nextCursor())
                    .replaceQueryParam("limit", limit)
                    .build()
                    .toUriString();
            headers.add(HttpHeaders.LINK, "<" + next + ">; rel=\"next\"");
        }
        return ResponseEntity.ok().headers(headers).body(page.items());
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
