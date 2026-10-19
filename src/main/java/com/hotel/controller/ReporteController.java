package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.service.ReporteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/reservas")
    public ResponseEntity<byte[]> reservas(HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(403).build();
        }
        return pdf("reservas.pdf", reporteService.generarReservas());
    }

    @GetMapping("/ingresos")
    public ResponseEntity<byte[]> ingresos(HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(403).build();
        }
        return pdf("proyeccion-ingresos.pdf", reporteService.generarProyeccionIngresos());
    }

    private boolean esAdministrador(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && "ADMIN".equals(usuario.getRol());
    }

    private ResponseEntity<byte[]> pdf(String nombre, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(contenido);
    }
}
