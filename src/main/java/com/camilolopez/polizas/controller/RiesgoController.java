package com.camilolopez.polizas.controller;

import com.camilolopez.polizas.dto.RiesgoResponse;
import com.camilolopez.polizas.service.RiesgoService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated
public class RiesgoController {
    private final RiesgoService riesgos;
    public RiesgoController(RiesgoService riesgos) { this.riesgos = riesgos; }
    @PostMapping("/riesgos/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable @Positive Long id) { return riesgos.cancelar(id); }
}
