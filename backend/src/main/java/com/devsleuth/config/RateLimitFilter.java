package com.devsleuth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter per IP.
 * ponytail: upgrade to Redis-backed or bucket4j if multi-instance deployment needed.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int WEBHOOK_MAX_PER_MINUTE = 120;
    /** Cap tracked IPs so a flood of distinct source IPs can't exhaust memory. */
    private static final int MAX_TRACKED_IPS = 100_000;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String ip = getClientIp(req);
        String path = req.getRequestURI();

        int limit = path.startsWith("/api/webhooks") ? WEBHOOK_MAX_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        // ponytail: bounded in-memory map with bulk eviction; upgrade to a TTL cache
        // (Caffeine) or Redis for multi-instance deployments. Clearing when full drops
        // stale windows and prevents unbounded growth from IP-spoofed floods.
        if (counters.size() >= MAX_TRACKED_IPS && !counters.containsKey(ip)) {
            counters.clear();
        }
        WindowCounter counter = counters.computeIfAbsent(ip, k -> new WindowCounter());

        if (counter.incrementAndCheck(limit)) {
            chain.doFilter(request, response);
        } else {
            res.setStatus(429);
            res.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean incrementAndCheck(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
