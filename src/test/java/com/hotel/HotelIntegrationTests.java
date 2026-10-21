package com.hotel;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.modelo.Usuario;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import com.hotel.repository.TipoHabitacionRepository;
import com.hotel.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HotelIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private HabitacionRepository habitacionRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private TipoHabitacionRepository tipoHabitacionRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Cliente cliente;
    private Habitacion habitacion;

    @BeforeEach
    void prepararDatos() {
        reservaRepository.deleteAll();
        clienteRepository.deleteAll();
        habitacionRepository.deleteAll();
        tipoHabitacionRepository.deleteAll();

        TipoHabitacion tipo = tipoHabitacionRepository.save(
                new TipoHabitacion("Simple", 120.0));
        cliente = clienteRepository.save(
                new Cliente("Ana Perez", "12345678", "999999999", "ana@example.com"));
        habitacion = habitacionRepository.save(
                new Habitacion("101", 1, "DISPONIBLE", tipo));
    }

    @Test
    void permiteListarClientesSoloAAdministradores() throws Exception {
        mvc.perform(get("/clientes").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes"));

        mvc.perform(get("/clientes").session(sesion("RECEPCIONISTA")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio?errorPermiso"));

        mvc.perform(get("/habitaciones").session(sesion("RECEPCIONISTA")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio?errorPermiso"));
    }

    @Test
    void muestraErrorAlRegistrarDocumentoDuplicado() throws Exception {
        mvc.perform(post("/clientes/guardar").session(sesion("ADMIN"))
                        .param("nombre", "Otra persona")
                        .param("documento", "12345678")
                        .param("telefono", "111111111")
                        .param("correo", "otra@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-formulario"))
                .andExpect(model().attribute("error", containsString("documento")));
    }

    @Test
    void validaQueElPrecioDelTipoSeaPositivo() throws Exception {
        mvc.perform(post("/tipos-habitacion/guardar").session(sesion("ADMIN"))
                        .param("nombre", "Suite")
                        .param("precio", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("tipo-habitacion-formulario"))
                .andExpect(model().attribute("error", containsString("mayor que cero")));
    }

    @Test
    void listaReservasYRegistraReservaActualizandoHabitacion() throws Exception {
        LocalDate entrada = LocalDate.now().plusDays(2);
        LocalDate salida = entrada.plusDays(3);

        mvc.perform(get("/reservas").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas"));

        mvc.perform(post("/reservas/guardar").session(sesion("RECEPCIONISTA"))
                        .param("fechaEntrada", entrada.toString())
                        .param("fechaSalida", salida.toString())
                        .param("clienteId", cliente.getIdCliente().toString())
                        .param("habitacionId", habitacion.getIdHabitacion().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attribute("mensaje", containsString("correctamente")));

        assertEquals(1, reservaRepository.count());
        assertEquals("RESERVADA", habitacionRepository.findById(
                habitacion.getIdHabitacion()).orElseThrow().getEstado());
    }

    @Test
    void rechazaUnaReservaQueSeCruzaConOtraExistente() throws Exception {
        LocalDate entradaExistente = LocalDate.now().plusDays(2);
        LocalDate salidaExistente = entradaExistente.plusDays(3);
        reservaRepository.save(new Reserva(entradaExistente, salidaExistente,
                "CONFIRMADA", cliente, habitacion));

        mvc.perform(post("/reservas/guardar").session(sesion("ADMIN"))
                        .param("fechaEntrada", entradaExistente.plusDays(1).toString())
                        .param("fechaSalida", salidaExistente.plusDays(1).toString())
                        .param("clienteId", cliente.getIdCliente().toString())
                        .param("habitacionId", habitacion.getIdHabitacion().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", containsString("reserva")));

        assertEquals(1, reservaRepository.count());
    }

    @Test
    void reportesPdfEstanDisponiblesSoloParaAdministradores() throws Exception {
        byte[] pdf = mvc.perform(get("/reportes/reservas").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));

        mvc.perform(get("/reportes/ingresos").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        mvc.perform(get("/reportes/reservas").session(sesion("RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/reservas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginCifraLaContrasenaYGuardaElUsuarioEnSesion() throws Exception {
        usuarioRepository.deleteAll();
        usuarioRepository.save(new Usuario("Recepción", "recepcion", "123456", "RECEPCIONISTA"));

        mvc.perform(post("/login").param("username", "recepcion").param("password", "mala"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", containsString("incorrectos")));

        mvc.perform(post("/login").param("username", "recepcion").param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"))
                .andExpect(request().sessionAttribute("usuario", org.hamcrest.Matchers.notNullValue()));

        String guardada = usuarioRepository.findByUsername("recepcion").orElseThrow().getPassword();
        assertTrue(guardada.startsWith("$2"), "La contraseña debe quedar cifrada con BCrypt");

        mvc.perform(post("/login").param("username", "recepcion").param("password", "123456"))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void consultasDeDisponibilidadYReservasFuncionanParaRecepcion() throws Exception {
        LocalDate entrada = LocalDate.now().plusDays(5);
        reservaRepository.save(new Reserva(entrada, entrada.plusDays(2), "CONFIRMADA", cliente, habitacion));

        mvc.perform(get("/consultas/disponibilidad").session(sesion("RECEPCIONISTA"))
                        .param("fechaEntrada", entrada.toString())
                        .param("fechaSalida", entrada.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("consulta-disponibilidad"))
                .andExpect(model().attribute("disponibles", org.hamcrest.Matchers.empty()));

        mvc.perform(get("/consultas/reservas").session(sesion("RECEPCIONISTA"))
                        .param("cliente", "ana"))
                .andExpect(status().isOk())
                .andExpect(view().name("consulta-reservas"))
                .andExpect(model().attribute("reservas", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void cancelarReservaLiberaLaHabitacion() throws Exception {
        LocalDate entrada = LocalDate.now().plusDays(2);
        mvc.perform(post("/reservas/guardar").session(sesion("ADMIN"))
                .param("fechaEntrada", entrada.toString())
                .param("fechaSalida", entrada.plusDays(2).toString())
                .param("clienteId", cliente.getIdCliente().toString())
                .param("habitacionId", habitacion.getIdHabitacion().toString()));
        Reserva reserva = reservaRepository.findAll().get(0);

        mvc.perform(post("/reservas/" + reserva.getIdReserva() + "/cancelar").session(sesion("RECEPCIONISTA")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", containsString("cancelada")));

        assertEquals("CANCELADA", reservaRepository.findById(reserva.getIdReserva()).orElseThrow().getEstado());
        assertEquals("DISPONIBLE", habitacionRepository.findById(habitacion.getIdHabitacion()).orElseThrow().getEstado());
    }

    @Test
    void reporteDeHabitacionesGeneraPdf() throws Exception {
        byte[] pdf = mvc.perform(get("/reportes/habitaciones").session(sesion("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
    }

    @Test
    void rechazaDocumentoConFormatoInvalido() throws Exception {
        mvc.perform(post("/clientes/guardar").session(sesion("ADMIN"))
                        .param("nombre", "Luis Rojas")
                        .param("documento", "123")
                        .param("telefono", "987654321")
                        .param("correo", "luis@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-formulario"))
                .andExpect(model().attribute("error", containsString("documento")));
    }

    private MockHttpSession sesion(String rol) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", new Usuario("Usuario de prueba", "prueba", "x", rol));
        return session;
    }
}
