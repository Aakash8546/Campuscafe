package com.campuscafe.backend.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to intercept all incoming HTTP requests and log key details
 * (HTTP Method, URI, Query Parameters, Response Status, Client IP, and latency).
 */
@Component
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        
        // Skip actuator endpoints from logging if you want to keep logs cleaner
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        String method = httpRequest.getMethod();
        String queryString = httpRequest.getQueryString();
        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        String clientIp = getClientIp(httpRequest);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            log.info("HTTP {} {} | Status: {} | IP: {} | Latency: {}ms",
                    method, fullUri, status, clientIp, duration);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
