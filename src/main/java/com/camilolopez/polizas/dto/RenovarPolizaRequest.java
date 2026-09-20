package com.camilolopez.polizas.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record RenovarPolizaRequest(@NotNull @DecimalMin("0.0") BigDecimal ipcPorcentaje) {}
