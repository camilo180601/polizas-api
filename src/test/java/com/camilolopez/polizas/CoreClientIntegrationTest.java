package com.camilolopez.polizas;

import com.camilolopez.polizas.exception.CoreIntegrationException;
import com.camilolopez.polizas.integration.HttpCoreClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class CoreClientIntegrationTest {
    @Test void sendsExactRequestAndRejectsFailure() throws Exception {
        AtomicReference<String> request = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/core-mock/evento", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("api-key") + " " + body);
            exchange.sendResponseHeaders(body.contains("999") ? 500 : 204, -1);
            exchange.close();
        });
        server.start();
        try {
            HttpCoreClient core = new HttpCoreClient("http://localhost:" + server.getAddress().getPort(), "123456");
            core.actualizacion(555L, "TEST");
            assertEquals("POST /core-mock/evento 123456 {\"evento\":\"ACTUALIZACION\",\"polizaId\":555}", request.get());
            assertThrows(CoreIntegrationException.class, () -> core.actualizacion(999L, "TEST"));
        } finally { server.stop(0); }
    }
}
