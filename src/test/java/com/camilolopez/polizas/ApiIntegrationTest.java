package com.camilolopez.polizas;

import com.camilolopez.polizas.entity.*;
import com.camilolopez.polizas.integration.CoreClient;
import com.camilolopez.polizas.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ApiIntegrationTest.FakeCoreConfig.class)
class ApiIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired PolizaRepository polizas;
    @Autowired RiesgoRepository riesgos;
    @Autowired CountingCore core;
    final HttpClient http = HttpClient.newHttpClient();
    Long individual, colectiva, cancelada, riesgoColectivo;

    @TestConfiguration static class FakeCoreConfig {
        @Bean @Primary CountingCore countingCore() { return new CountingCore(); }
    }
    static class CountingCore implements CoreClient {
        final AtomicInteger count = new AtomicInteger();
        Long lastId;
        boolean fail;
        public void actualizacion(Long id, String operation) {
            count.incrementAndGet(); lastId = id;
            if (fail) throw new com.camilolopez.polizas.exception.CoreIntegrationException("fallo", null);
        }
    }
    @BeforeEach void fixture() {
        core.count.set(0); core.fail = false;
        riesgos.deleteAll(); polizas.deleteAll();
        Poliza p1 = polizas.save(new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.ACTIVA,
                LocalDate.of(2026, 1, 31), 1, new BigDecimal("1000000.00")));
        Poliza p2 = polizas.save(new Poliza(TipoPoliza.COLECTIVA, EstadoPoliza.ACTIVA,
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("2000000.00")));
        Poliza p3 = polizas.save(new Poliza(TipoPoliza.INDIVIDUAL, EstadoPoliza.CANCELADA,
                LocalDate.of(2026, 1, 1), 6, new BigDecimal("800000.00")));
        individual = p1.id; colectiva = p2.id; cancelada = p3.id;
        riesgos.save(new Riesgo(p1, "A", EstadoRiesgo.ACTIVO));
        riesgoColectivo = riesgos.save(new Riesgo(p2, "B", EstadoRiesgo.ACTIVO)).id;
    }
    HttpResponse<String> call(String method, String path, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        for (int i = 0; i < headers.length; i += 2) request.header(headers[i], headers[i + 1]);
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void securityFiltersAndMock() throws Exception {
        assertEquals(401, call("GET", "/polizas", null).statusCode());
        assertEquals(401, call("GET", "/polizas", null, "api-key", "wrong").statusCode());
        assertEquals(200, call("GET", "/polizas", null, "api-key", "123456").statusCode());
        assertEquals(200, call("GET", "/polizas", null, "x-api-key", "123456").statusCode());
        assertEquals(401, call("GET", "/polizas", null, "api-key", "123456", "x-api-key", "wrong").statusCode());
        assertEquals(401, call("GET", "/polizas", null, "api-key", "123456", "api-key", "123456").statusCode());
        assertEquals(204, call("POST", "/core-mock/evento", "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}", "api-key", "123456").statusCode());
        assertEquals(401, call("POST", "/core-mock/evento", "{}", new String[]{}).statusCode());
        assertEquals(400, call("POST", "/core-mock/evento", "{\"evento\":\"OTHER\",\"polizaId\":555}", "api-key", "123456").statusCode());
    }
    @Test void listAndValidation() throws Exception {
        assertEquals(200, call("GET", "/polizas?tipo=COLECTIVA&estado=ACTIVA", null, "api-key", "123456").statusCode());
        assertEquals("[]", call("GET", "/polizas?tipo=COLECTIVA&estado=CANCELADA", null, "api-key", "123456").body());
        assertEquals(400, call("GET", "/polizas?tipo=BAD", null, "api-key", "123456").statusCode());
        assertEquals(400, call("GET", "/polizas/abc/riesgos", null, "api-key", "123456").statusCode());
        assertEquals(400, call("GET", "/polizas/0/riesgos", null, "api-key", "123456").statusCode());
        assertEquals(404, call("GET", "/polizas/999999/riesgos", null, "api-key", "123456").statusCode());
        assertEquals(0, core.count.get());
    }
    @Test void renewAndBusinessRules() throws Exception {
        String path = "/polizas/" + individual + "/renovar";
        HttpResponse<String> result = call("POST", path, "{\"ipcPorcentaje\":5.00}", "api-key", "123456");
        assertEquals(200, result.statusCode());
        assertTrue(result.body().contains("1050000.00"));
        assertTrue(result.body().contains("2026-02-28"));
        assertEquals(1, core.count.get());
        assertEquals(400, call("POST", path, "{\"ipcPorcentaje\":-1}", "api-key", "123456").statusCode());
        assertEquals(400, call("POST", path, "{\"ipcPorcentaje\":0.00001}", "api-key", "123456").statusCode());
        assertEquals(409, call("POST", "/polizas/" + cancelada + "/renovar", "{\"ipcPorcentaje\":5}", "api-key", "123456").statusCode());
        assertEquals(409, call("POST", "/polizas/" + individual + "/riesgos", "{\"descripcion\":\"extra\"}", "api-key", "123456").statusCode());
        assertEquals(1, core.count.get());
    }
    @Test void cancelCascadeAndIdempotence() throws Exception {
        String path = "/polizas/" + colectiva + "/cancelar";
        assertEquals(200, call("POST", path, null, "api-key", "123456").statusCode());
        assertEquals(EstadoRiesgo.CANCELADO, riesgos.findById(riesgoColectivo).orElseThrow().estado);
        assertEquals(200, call("POST", path, null, "api-key", "123456").statusCode());
        assertEquals(1, core.count.get());
        assertEquals(409, call("POST", "/polizas/" + colectiva + "/riesgos", "{\"descripcion\":\"C\"}", "api-key", "123456").statusCode());
    }
    @Test void addAndCancelRisk() throws Exception {
        var added = call("POST", "/polizas/" + colectiva + "/riesgos", "{\"descripcion\":\"Nuevo\"}", "api-key", "123456");
        assertEquals(201, added.statusCode());
        assertTrue(added.body().contains("Nuevo"));
        assertEquals(2, riesgos.findByPolizaIdOrderByIdAsc(colectiva).size());
        assertEquals(200, call("POST", "/riesgos/" + riesgoColectivo + "/cancelar", null, "api-key", "123456").statusCode());
        assertEquals(colectiva, core.lastId);
        assertEquals(2, core.count.get());
    }
    @Test void failedCoreRollsBack() throws Exception {
        core.fail = true;
        assertEquals(502, call("POST", "/polizas/" + colectiva + "/cancelar", null, "api-key", "123456").statusCode());
        assertEquals(EstadoPoliza.ACTIVA, polizas.findById(colectiva).orElseThrow().estado);
        assertEquals(EstadoRiesgo.ACTIVO, riesgos.findById(riesgoColectivo).orElseThrow().estado);
    }
    @Test void concurrentAddAndCancelLeavesNoActiveRisk() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<Integer> add = workers.submit(() -> {
                gate.await();
                return call("POST", "/polizas/" + colectiva + "/riesgos", "{\"descripcion\":\"Concurrente\"}",
                        "api-key", "123456").statusCode();
            });
            Future<Integer> cancel = workers.submit(() -> {
                gate.await();
                return call("POST", "/polizas/" + colectiva + "/cancelar", null,
                        "api-key", "123456").statusCode();
            });
            gate.countDown();
            assertTrue(add.get(10, TimeUnit.SECONDS) == 201 || add.get(10, TimeUnit.SECONDS) == 409);
            assertEquals(200, cancel.get(10, TimeUnit.SECONDS));
            assertEquals(EstadoPoliza.CANCELADA, polizas.findById(colectiva).orElseThrow().estado);
            assertTrue(riesgos.findByPolizaIdOrderByIdAsc(colectiva).stream()
                    .allMatch(r -> r.estado == EstadoRiesgo.CANCELADO));
        } finally { workers.shutdownNow(); }
    }
}
