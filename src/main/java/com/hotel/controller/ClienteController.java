package com.hotel.controller;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.Usuario;
import com.hotel.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
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

        model.addAttribute("clientes", clienteService.listar());
        return "clientes";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        model.addAttribute("cliente", new Cliente());
        model.addAttribute("accion", "Registrar");
        return "cliente-formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Cliente cliente,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        String errorValidacion = clienteService.validar(cliente);
        if (errorValidacion != null) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("accion", cliente.getIdCliente() == null ? "Registrar" : "Editar");
            model.addAttribute("error", errorValidacion);
            return "cliente-formulario";
        }

        try {
            clienteService.guardar(cliente);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("accion", cliente.getIdCliente() == null ? "Registrar" : "Editar");
            model.addAttribute("error", "Ese documento ya está registrado. Verifica el número e intenta de nuevo.");
            return "cliente-formulario";
        }
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        Cliente cliente = clienteService.buscarPorId(id).orElse(null);

        if (cliente == null) {
            return "redirect:/clientes";
        }

        model.addAttribute("cliente", cliente);
        model.addAttribute("accion", "Editar");
        return "cliente-formulario";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Integer id,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return "redirect:/inicio?errorPermiso";
        }

        try {
            clienteService.eliminar(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return "redirect:/clientes?error=reservas";
        }

        return "redirect:/clientes";
    }
}
