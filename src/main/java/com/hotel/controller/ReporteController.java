package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.service.ReporteService;
import com.hotel.service.ReservaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/** Reportes PDF (JasperReports): reservas y habitaciones. Solo administrador. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        model.addAttribute("desde", LocalDate.now().withDayOfMonth(1));
        model.addAttribute("hasta", LocalDate.now().plusMonths(1));
        model.addAttribute("estados", List.of(ReservaService.CONFIRMADA, ReservaService.FINALIZADA, ReservaService.CANCELADA));
        return "reportes";
    }

    @GetMapping("/reservas")
    public ResponseEntity<byte[]> reservas(HttpSession session,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) String estado) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(403).build();
        }
        return pdf("reservas.pdf", reporteService.generarReservas(desde, hasta, estado));
    }

    @GetMapping("/habitaciones")
    public ResponseEntity<byte[]> habitaciones(HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(403).build();
        }
        return pdf("habitaciones.pdf", reporteService.generarHabitaciones());
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
