package com.camilolopez.polizas;

import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.integration.*;
import com.camilolopez.polizas.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FullFlowTest.CoreConfig.class)
class FullFlowTest {
    @Value("${local.server.port}") int port;
    @Autowired PolizaRepository polizas;
    @TestConfiguration static class CoreConfig {
        @Bean @Primary CoreClient localCore(Environment env) {
            return (id, operation) -> new HttpCoreClient(
                    "http://localhost:" + env.getProperty("local.server.port"), "123456").actualizacion(id, operation);
        }
    }
    @Test void renewUsesRealHttpMockAndCommits() throws Exception {
        Poliza p = polizas.save(new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA,
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000.00")));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/polizas/" + p.id + "/renovar"))
                .header("api-key", "123456").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"ipcPorcentaje\":5}")).build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(new BigDecimal("1050000.00"), polizas.findById(p.id).orElseThrow().canonMensual);
    }
}
