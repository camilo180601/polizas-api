package com.camilolopez.polizas.dto;
import jakarta.validation.constraints.*;
public record CoreEventoRequest(@NotBlank String evento, @NotNull @Positive Long polizaId) {}
