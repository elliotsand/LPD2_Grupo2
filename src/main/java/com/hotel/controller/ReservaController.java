package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.repository.ClienteRepository;
import com.hotel.service.ReservaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Caso de uso transaccional: registrar reserva (y cancelar / finalizar). */
@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final ClienteRepository clienteRepository;

    public ReservaController(ReservaService reservaService,
                             ClienteRepository clienteRepository) {
        this.reservaService = reservaService;
        this.clienteRepository = clienteRepository;
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
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
                         @RequestParam(required = false) Integer habitacionId) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("reservas", reservaService.listar());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("fechaEntrada", fechaEntrada);
        model.addAttribute("fechaSalida", fechaSalida);
        model.addAttribute("habitacionSeleccionada", habitacionId);
        model.addAttribute("fechaMinima", LocalDate.now());

        if (fechaEntrada != null && fechaSalida != null) {
            try {
                model.addAttribute("habitacionesDisponibles",
                        reservaService.disponibles(fechaEntrada, fechaSalida));
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
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

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        try {
            reservaService.cancelar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Reserva " + id + " cancelada; la habitación quedó liberada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas";
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!tienePermiso(session)) {
            return "redirect:/inicio?errorPermiso";
        }
        try {
            reservaService.finalizar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Reserva " + id + " finalizada (check-out).");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservas";
    }
}
