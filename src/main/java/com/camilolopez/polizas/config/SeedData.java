package com.camilolopez.polizas.config;

import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.repository.*;
import org.slf4j.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration @Profile("demo")
public class SeedData {
    private static final Logger log = LoggerFactory.getLogger(SeedData.class);
    @Bean CommandLineRunner seed(PolizaRepository polizas, RiesgoRepository riesgos) {
        return args -> {
            if (polizas.count() != 0) return;
            Poliza p1 = polizas.save(new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA,
                    LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000.00")));
            Poliza p2 = polizas.save(new Poliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA,
                    LocalDate.of(2026, 2, 1), 12, new BigDecimal("2000000.00")));
            Poliza p3 = polizas.save(new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.CANCELADA,
                    LocalDate.of(2026, 3, 1), 6, new BigDecimal("800000.00")));
            Poliza p4 = polizas.save(new Poliza(TipoPoliza.COLECTIVA, EstadoPoliza.RENOVADA,
                    LocalDate.of(2026, 4, 1), 12, new BigDecimal("1500000.00")));
            riesgos.save(new Riesgo(p1, "Apartamento 101", EstadoRiesgo.ACTIVO));
            riesgos.save(new Riesgo(p2, "Apartamento 201", EstadoRiesgo.ACTIVO));
            riesgos.save(new Riesgo(p2, "Apartamento 202", EstadoRiesgo.ACTIVO));
            riesgos.save(new Riesgo(p3, "Apartamento 301", EstadoRiesgo.CANCELADO));
            riesgos.save(new Riesgo(p4, "Apartamento 401", EstadoRiesgo.ACTIVO));
            riesgos.save(new Riesgo(p4, "Apartamento 402", EstadoRiesgo.CANCELADO));
            log.info("Seed created polizaIds={},{},{},{}", p1.id, p2.id, p3.id, p4.id);
        };
    }
}
