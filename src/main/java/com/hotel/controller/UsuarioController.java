
package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    public String loginInicial(HttpSession session) {
        if (session.getAttribute("usuario") != null) {
            return "redirect:/inicio";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        for (Usuario usuario : usuarioService.listar()) {
            if (usuario.getUsername().equals(username)
                    && usuario.getPassword().equals(password)) {
                session.setAttribute("usuario", usuario);
                return "redirect:/inicio";
            }
        }

        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/inicio")
    public String mostrarInicio(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String errorPermiso) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("esAdmin", "ADMIN".equals(usuario.getRol()));

        if (errorPermiso != null) {
            model.addAttribute("errorPermiso",
                    "No tienes permiso para acceder a esa sección.");
        }

        return "inicio";
    }

    @GetMapping("/mantenimientos")
    public String mantenimientos(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/";
        }

        if (!"ADMIN".equals(usuario.getRol())) {
            return "redirect:/inicio?errorPermiso";
        }

        return "mantenimientos";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
