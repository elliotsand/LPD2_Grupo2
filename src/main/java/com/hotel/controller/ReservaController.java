package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.service.ReservaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final ClienteRepository clienteRepository;
    private final HabitacionRepository habitacionRepository;

    public ReservaController(ReservaService reservaService,
                             ClienteRepository clienteRepository,
                             HabitacionRepository habitacionRepository) {
        this.reservaService = reservaService;
        this.clienteRepository = clienteRepository;
        this.habitacionRepository = habitacionRepository;
    }

    private boolean tienePermiso(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && ("ADMIN".equals(usuario.getRol())
                || "RECEPCIONISTA".equals(usuario.getRol()));
    }

    @GetMapping
    public String listar(HttpSession session, Model model,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("reservas", reservaService.listar());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("habitaciones", habitacionRepository.findAll());
        model.addAttribute("fechaEntrada", fechaEntrada);
        model.addAttribute("fechaSalida", fechaSalida);
        model.addAttribute("fechaMinima", LocalDate.now());

        if (fechaEntrada != null && fechaSalida != null) {
            if (!fechaSalida.isAfter(fechaEntrada)) {
                model.addAttribute("error", "La fecha de salida debe ser posterior a la fecha de entrada.");
            } else {
                model.addAttribute("habitacionesDisponibles",
                        reservaService.disponibles(fechaEntrada, fechaSalida));
            }
        }
        return "reservas";
    }

    @PostMapping("/guardar")
    public String guardar(HttpSession session,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
                          @RequestParam Integer clienteId,
                          @RequestParam Integer habitacionId,
                          RedirectAttributes redirectAttributes) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        try {
            reservaService.registrar(fechaEntrada, fechaSalida, clienteId, habitacionId);
            redirectAttributes.addFlashAttribute("mensaje", "Reserva registrada correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas";
    }
}
