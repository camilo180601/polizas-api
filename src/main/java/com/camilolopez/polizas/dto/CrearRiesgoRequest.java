package com.camilolopez.polizas.dto;
import jakarta.validation.constraints.*;
public record CrearRiesgoRequest(@NotBlank @Size(max = 200) String descripcion) {}
