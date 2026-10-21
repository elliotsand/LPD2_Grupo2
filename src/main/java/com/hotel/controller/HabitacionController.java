
package com.hotel.controller;

import com.hotel.modelo.Habitacion;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.modelo.Usuario;
import com.hotel.service.HabitacionService;
import com.hotel.service.TipoHabitacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;
    private final TipoHabitacionService tipoHabitacionService;

    public HabitacionController(
            HabitacionService habitacionService,
            TipoHabitacionService tipoHabitacionService) {
        this.habitacionService = habitacionService;
        this.tipoHabitacionService = tipoHabitacionService;
    }

    private boolean esAdministrador(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && "ADMIN".equals(usuario.getRol());
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("habitaciones", habitacionService.listar());
        return "habitaciones";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("habitacion", new Habitacion());
        model.addAttribute("tipos", tipoHabitacionService.listar());
        model.addAttribute("accion", "Registrar");

        return "habitacion-formulario";
    }

    
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Habitacion habitacion,
            @RequestParam Integer idTipoHabitacion,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        TipoHabitacion tipo = tipoHabitacionService
                .buscarPorId(idTipoHabitacion)
                .orElse(null);

        if (tipo == null) {
            return "redirect:/habitaciones";
        }

        String error = null;
        if (habitacion.getNumero() == null || habitacion.getNumero().isBlank()) {
            error = "Ingresa el número de la habitación.";
        } else if (habitacion.getPiso() == null || habitacion.getPiso() < 1 || habitacion.getPiso() > 50) {
            error = "El piso debe estar entre 1 y 50.";
        } else if (!java.util.List.of("DISPONIBLE", "RESERVADA", "OCUPADA", "MANTENIMIENTO")
                .contains(habitacion.getEstado())) {
            error = "Selecciona un estado válido.";
        } else {
            habitacion.setNumero(habitacion.getNumero().trim());
            boolean numeroDuplicado = habitacionService.listar().stream()
                    .anyMatch(h ->
                            h.getNumero().equalsIgnoreCase(habitacion.getNumero())
                            && !h.getIdHabitacion().equals(habitacion.getIdHabitacion()));
            if (numeroDuplicado) {
                error = "Ya existe una habitación con ese número.";
            }
        }

        if (error != null) {
            habitacion.setTipoHabitacion(tipo);
            model.addAttribute("habitacion", habitacion);
            model.addAttribute("tipos", tipoHabitacionService.listar());
            model.addAttribute("accion",
                    habitacion.getIdHabitacion() == null ? "Registrar" : "Editar");
            model.addAttribute("error", error);
            return "habitacion-formulario";
        }

        habitacion.setTipoHabitacion(tipo);
        habitacionService.guardar(habitacion);

        return "redirect:/habitaciones";
    }
 

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        Habitacion habitacion = habitacionService
                .buscarPorId(id)
                .orElse(null);

        if (habitacion == null) {
            return "redirect:/habitaciones";
        }

        model.addAttribute("habitacion", habitacion);
        model.addAttribute("tipos", tipoHabitacionService.listar());
        model.addAttribute("accion", "Editar");

        return "habitacion-formulario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Integer id,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        try {
            habitacionService.eliminar(id);
        } catch (DataIntegrityViolationException e) {
            return "redirect:/habitaciones?error=reservas";
        }

        return "redirect:/habitaciones";
    }
}
