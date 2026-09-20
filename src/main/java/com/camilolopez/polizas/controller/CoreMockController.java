package com.camilolopez.polizas.controller;

import com.camilolopez.polizas.dto.CoreEventoRequest;
import jakarta.validation.Valid;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class CoreMockController {
    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);
    @PostMapping("/core-mock/evento")
    public ResponseEntity<Void> evento(@Valid @RequestBody CoreEventoRequest body) {
        if (!"ACTUALIZACION".equals(body.evento())) throw new IllegalArgumentException("Evento inválido");
        log.info("CORE_MOCK_RECEIVED evento={} polizaId={} correlationId={}",
                body.evento(), body.polizaId(), MDC.get("correlationId"));
        return ResponseEntity.noContent().build();
    }
}
