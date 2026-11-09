package co.edu.uceva.aulasmart.users.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/** Defensa CSRF para los únicos endpoints que autentican por cookie. */
final class AuthOriginFilter extends OncePerRequestFilter {
    private final List<String> origins;
    AuthOriginFilter(List<String> origins) { this.origins=origins; }

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/auth/") && !request.getMethod().equals("OPTIONS")) {
            if (!request.getMethod().equals("POST") || request.getHeader("Origin") == null || !origins.contains(request.getHeader("Origin"))
                    || !"1".equals(request.getHeader("X-AulaSmart-CSRF"))) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Origen de autenticación no permitido.\"}");
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
