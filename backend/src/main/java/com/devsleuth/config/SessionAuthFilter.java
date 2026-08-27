package com.devsleuth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces authentication for /api/** endpoints based on the login session.
 *
 * <p>Login sets {@code userId} on the HttpSession (see AuthController). Previously the
 * security config left /api/** open, so every read endpoint was publicly reachable and
 * only some controllers happened to check the session. This filter centralizes the gate:
 * one guard for all API routes instead of per-controller checks that were easy to forget.
 *
 * <p>Public exceptions: the OAuth endpoints (/api/auth/**) and the GitHub webhook
 * (/api/webhooks/**, verified separately via HMAC).
 */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Let CORS preflight through untouched — it carries no credentials.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (!requiresAuth(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Object userId = session != null ? session.getAttribute("userId") : null;
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication required\",\"status\":401}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean requiresAuth(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }
        return !path.startsWith("/api/auth/") && !path.startsWith("/api/webhooks/");
    }
}
