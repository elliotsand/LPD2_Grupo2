package com.hotel.controller;

import com.hotel.modelo.Usuario;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import com.hotel.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
public class UsuarioController {

    /** Minutos de inactividad antes de que expire la sesión. */
    private static final int MINUTOS_SESION = 20;

    private final UsuarioService usuarioService;
    private final ClienteRepository clienteRepository;
    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;

    public UsuarioController(UsuarioService usuarioService, ClienteRepository clienteRepository,
                             HabitacionRepository habitacionRepository, ReservaRepository reservaRepository) {
        this.usuarioService = usuarioService;
        this.clienteRepository = clienteRepository;
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
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
            HttpServletRequest request,
            Model model) {

        Optional<Usuario> autenticado = usuarioService.autenticar(username, password);
        if (autenticado.isEmpty()) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }

        // Se descarta la sesión anterior para evitar la fijación de sesión
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(MINUTOS_SESION * 60);
        session.setAttribute("usuario", autenticado.get());
        session.setAttribute("horaIngreso",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return "redirect:/inicio";
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
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalHabitaciones", habitacionRepository.count());
        model.addAttribute("habitacionesDisponibles", habitacionRepository.countByEstado("DISPONIBLE"));
        model.addAttribute("reservasActivas", reservaRepository.countByEstadoAndFechaSalidaAfter("CONFIRMADA", LocalDate.now()));

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
        return "redirect:/?salir";
    }
}
