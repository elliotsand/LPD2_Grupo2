package com.hotel.config;

import com.hotel.modelo.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Gestion de sesiones: toda pagina (excepto el login) exige una sesion activa.
 * Si la sesion expiro o no existe, redirige al login con un aviso.
 */
@Component
public class SesionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            boolean expirada = request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
            response.sendRedirect(request.getContextPath() + (expirada ? "/?expirada" : "/"));
            return false;
        }
        // Evita que el navegador muestre paginas privadas desde la cache tras cerrar sesion
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        return true;
    }
}
