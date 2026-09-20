package com.camilolopez.polizas.integration;

import com.camilolopez.polizas.exception.CoreIntegrationException;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Component
public class HttpCoreClient implements CoreClient {
    private static final Logger log = LoggerFactory.getLogger(HttpCoreClient.class);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private final URI endpoint;
    private final String apiKey;
    public HttpCoreClient(@Value("${core.base-url}") String baseUrl, @Value("${app.api-key}") String apiKey) {
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/core-mock/evento");
        this.apiKey = apiKey;
    }
    @Override public void actualizacion(Long polizaId, String operacion) {
        long start = System.nanoTime();
        String correlation = MDC.get("correlationId");
        log.info("CORE_SEND_ATTEMPT operation={} polizaId={} correlationId={}", operacion, polizaId, correlation);
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .header("api-key", apiKey)
                    .header("X-Correlation-Id", correlation == null ? "" : correlation)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"evento\":\"ACTUALIZACION\",\"polizaId\":" + polizaId + "}"))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long ms = Duration.ofNanos(System.nanoTime() - start).toMillis();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("CORE_SEND_FAILED operation={} polizaId={} status={} durationMs={}", operacion, polizaId, response.statusCode(), ms);
                throw new CoreIntegrationException("HTTP " + response.statusCode(), null);
            }
            log.info("CORE_SEND_OK operation={} polizaId={} status={} durationMs={}", operacion, polizaId, response.statusCode(), ms);
        } catch (CoreIntegrationException ex) { throw ex; }
        catch (Exception ex) {
            log.warn("CORE_SEND_FAILED operation={} polizaId={} durationMs={} reason={}",
                    operacion, polizaId, Duration.ofNanos(System.nanoTime() - start).toMillis(), ex.getClass().getSimpleName());
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new CoreIntegrationException("CORE no disponible", ex);
        }
    }
}
