package org.tms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tms.audit.AuditService;
import org.tms.service.CustomUserDetailsService;
import org.tms.service.JwtService;
import org.tms.service.JwtUtil;
import org.tms.service.UserService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, AuditService auditService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("path", request.getRequestURI());
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No Bearer token, proceeding without authentication");
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);
            username = jwtUtil.getUsernameFromToken(jwt);
            if (username != null) {
                MDC.put("user", username);
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt)) {
                    log.debug("JWT valid for user: {}", username);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("Invalid JWT for user: {}", username);
                    auditService.audit("JWT_INVALID", username, "Invalid token");
                    sendErrorResponse(response, "Invalid token");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
            auditService.audit("JWT_EXPIRED", null, "Token expired");
            auditService.auditDb(
                    "JWT_EXPIRED",
                    null,
                    null,
                    "Token expired",
                    request.getRequestURI(),
                    request.getMethod(),
                    MDC.get("requestId")
            );
            sendTokenExpiredResponse(response);
        } catch (Exception e) {
            log.error("JWT processing error: {}", e.getMessage());
            auditService.audit("JWT_ERROR", null, e.getClass().getSimpleName());
            auditService.auditDb(
                    "JWT_ERROR",
                    null,
                    null,
                    e.getClass().getSimpleName(),
                    request.getRequestURI(),
                    request.getMethod(),
                    MDC.get("requestId")
            );
            sendErrorResponse(response, "Invalid token");
        } finally {
            MDC.clear();
        }
    }

    private void sendTokenExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Token expired\", \"status\": 401, \"timestamp\": \"" + LocalDateTime.now() + "\"}");
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\", \"status\": 401}");
    }
}
