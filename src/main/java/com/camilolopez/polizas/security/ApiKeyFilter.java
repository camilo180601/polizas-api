package com.camilolopez.polizas.security;

import com.camilolopez.polizas.dto.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final String expected;
    private final ObjectMapper mapper;
    public ApiKeyFilter(@Value("${app.api-key}") String expected, ObjectMapper mapper) {
        this.expected = expected; this.mapper = mapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader("X-Correlation-Id");
        String correlation = request.getRequestURI().equals("/core-mock/evento") && incoming != null
                && incoming.matches("[0-9a-fA-F-]{36}") ? incoming : UUID.randomUUID().toString();
        MDC.put("correlationId", correlation);
        response.setHeader("X-Correlation-Id", correlation);
        try {
            List<String> keys = values(request, "api-key");
            List<String> aliases = values(request, "x-api-key");
            boolean valid = (keys.size() + aliases.size() >= 1) && keys.size() <= 1 && aliases.size() <= 1
                    && keys.stream().allMatch(expected::equals) && aliases.stream().allMatch(expected::equals);
            if (!valid) {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(), ApiError.of(401, "UNAUTHORIZED", "Clave API inválida", request.getRequestURI()));
                return;
            }
            chain.doFilter(request, response);
        } finally { MDC.remove("correlationId"); }
    }
    private List<String> values(HttpServletRequest request, String name) {
        return Collections.list(request.getHeaders(name));
    }
}
