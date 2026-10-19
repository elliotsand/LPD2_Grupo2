
package com.hotel.controller;

import com.hotel.modelo.TipoHabitacion;
import com.hotel.modelo.Usuario;
import com.hotel.service.TipoHabitacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tipos-habitacion")
public class TipoHabitacionController {

    private final TipoHabitacionService tipoHabitacionService;

    public TipoHabitacionController(
            TipoHabitacionService tipoHabitacionService) {
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

        model.addAttribute(
                "tipos",
                tipoHabitacionService.listar()
        );

        return "tipos-habitacion";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("tipo", new TipoHabitacion());
        model.addAttribute("accion", "Registrar");

        return "tipo-habitacion-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute TipoHabitacion tipo,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        tipoHabitacionService.guardar(tipo);

        return "redirect:/tipos-habitacion";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        TipoHabitacion tipo = tipoHabitacionService
                .buscarPorId(id)
                .orElse(null);

        if (tipo == null) {
            return "redirect:/tipos-habitacion";
        }

        model.addAttribute("tipo", tipo);
        model.addAttribute("accion", "Editar");

        return "tipo-habitacion-formulario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Integer id,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        try {
            tipoHabitacionService.eliminar(id);
        } catch (DataIntegrityViolationException e) {
            return "redirect:/tipos-habitacion?error=habitaciones";
        }

        return "redirect:/tipos-habitacion";
    }
}
