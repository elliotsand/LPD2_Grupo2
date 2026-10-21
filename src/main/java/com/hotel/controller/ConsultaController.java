package com.hotel.controller;

import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.Usuario;
import com.hotel.service.HabitacionService;
import com.hotel.service.ReservaService;
import com.hotel.service.TipoHabitacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/** Casos de uso de consulta: disponibilidad de habitaciones y reservas registradas. */
@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    private final ReservaService reservaService;
    private final TipoHabitacionService tipoHabitacionService;
    private final HabitacionService habitacionService;

    public ConsultaController(ReservaService reservaService, TipoHabitacionService tipoHabitacionService,
                              HabitacionService habitacionService) {
        this.reservaService = reservaService;
        this.tipoHabitacionService = tipoHabitacionService;
        this.habitacionService = habitacionService;
    }

    private boolean tienePermiso(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && ("ADMIN".equals(usuario.getRol())
                || "RECEPCIONISTA".equals(usuario.getRol()));
    }

    /** Consulta 1: disponibilidad de habitaciones por fechas y tipo. */
    @GetMapping("/disponibilidad")
    public String disponibilidad(HttpSession session, Model model,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
                                 @RequestParam(required = false) Integer idTipoHabitacion) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        model.addAttribute("tipos", tipoHabitacionService.listar());
        model.addAttribute("fechaEntrada", fechaEntrada);
        model.addAttribute("fechaSalida", fechaSalida);
        model.addAttribute("idTipoHabitacion", idTipoHabitacion);
        model.addAttribute("fechaMinima", LocalDate.now());

        if (fechaEntrada != null && fechaSalida != null) {
            try {
                List<Habitacion> disponibles = reservaService.disponibles(fechaEntrada, fechaSalida, idTipoHabitacion);
                model.addAttribute("disponibles", disponibles);
                model.addAttribute("noches", java.time.temporal.ChronoUnit.DAYS.between(fechaEntrada, fechaSalida));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "consulta-disponibilidad";
    }

    /** Consulta 2: reservas registradas con filtros por fechas, estado, habitación y cliente. */
    @GetMapping("/reservas")
    public String reservas(HttpSession session, Model model,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(required = false) String estado,
                           @RequestParam(required = false) Integer habitacionId,
                           @RequestParam(required = false) String cliente) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            model.addAttribute("error", "La fecha 'hasta' no puede ser anterior a 'desde'.");
            hasta = null;
        }
        List<Reserva> reservas = reservaService.buscar(desde, hasta, estado, habitacionId, cliente);
        double total = reservas.stream()
                .filter(r -> !ReservaService.CANCELADA.equals(r.getEstado()))
                .mapToDouble(Reserva::getTotal).sum();

        model.addAttribute("reservas", reservas);
        model.addAttribute("total", total);
        model.addAttribute("habitaciones", habitacionService.listar());
        model.addAttribute("estados", List.of(ReservaService.CONFIRMADA, ReservaService.FINALIZADA, ReservaService.CANCELADA));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("estado", estado);
        model.addAttribute("habitacionId", habitacionId);
        model.addAttribute("cliente", cliente);
        return "consulta-reservas";
    }
}
