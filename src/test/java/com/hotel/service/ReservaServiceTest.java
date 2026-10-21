package com.hotel.service;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.modelo.TipoHabitacion;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservaServiceTest {

    private ReservaRepository reservaRepository;
    private ClienteRepository clienteRepository;
    private HabitacionRepository habitacionRepository;
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        reservaRepository = mock(ReservaRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        habitacionRepository = mock(HabitacionRepository.class);
        reservaService = new ReservaService(
                reservaRepository, clienteRepository, habitacionRepository);
    }

    @Test
    void rechazaFechasDondeSalidaNoEsPosterior() {
        LocalDate entrada = LocalDate.now().plusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.disponibles(entrada, entrada));
        verifyNoInteractions(habitacionRepository, reservaRepository);
    }

    @Test
    void excluyeHabitacionesConReservasSuperpuestas() {
        Habitacion habitacion = habitacion();
        when(habitacionRepository.findAll()).thenReturn(List.of(habitacion));
        when(reservaRepository.existeCruce(
                eq(1), any(LocalDate.class), any(LocalDate.class))).thenReturn(true);

        List<Habitacion> disponibles = reservaService.disponibles(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertTrue(disponibles.isEmpty());
    }

    @Test
    void registraReservaYActualizaEstadoDeHabitacion() {
        Cliente cliente = new Cliente("Ana Perez", "12345678", "999999999", "ana@example.com");
        Habitacion habitacion = habitacion();
        LocalDate entrada = LocalDate.now().plusDays(1);
        LocalDate salida = entrada.plusDays(2);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(habitacionRepository.bloquearPorId(1)).thenReturn(Optional.of(habitacion));
        when(reservaRepository.existeCruce(1, entrada, salida)).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = reservaService.registrar(entrada, salida, 1, 1);

        assertEquals("CONFIRMADA", reserva.getEstado());
        assertSame(cliente, reserva.getCliente());
        assertSame(habitacion, reserva.getHabitacion());
        assertEquals("RESERVADA", habitacion.getEstado());
        verify(habitacionRepository).save(habitacion);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void rechazaRegistroCuandoExisteCruce() {
        LocalDate entrada = LocalDate.now().plusDays(1);
        LocalDate salida = entrada.plusDays(2);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(
                new Cliente("Ana Perez", "12345678", null, null)));
        when(habitacionRepository.bloquearPorId(1)).thenReturn(Optional.of(habitacion()));
        when(reservaRepository.existeCruce(1, entrada, salida)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.registrar(entrada, salida, 1, 1));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void rechazaReservasDeMasDeTreintaNoches() {
        LocalDate entrada = LocalDate.now().plusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.registrar(entrada, entrada.plusDays(31), 1, 1));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void cancelarLiberaLaHabitacionSiNoTieneOtraReserva() {
        Habitacion habitacion = habitacion();
        habitacion.setEstado("RESERVADA");
        Reserva reserva = new Reserva(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                "CONFIRMADA", new Cliente("Ana Perez", "12345678", null, null), habitacion);
        reserva.setIdReserva(7);
        when(reservaRepository.findById(7)).thenReturn(Optional.of(reserva));
        when(reservaRepository.tieneOtraReservaVigente(eq(1), any(LocalDate.class), eq(7))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.cancelar(7);

        assertEquals("CANCELADA", cancelada.getEstado());
        assertEquals("DISPONIBLE", habitacion.getEstado());
        verify(habitacionRepository).save(habitacion);
    }

    @Test
    void noPermiteCancelarUnaReservaYaFinalizada() {
        Reserva reserva = new Reserva(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1),
                "FINALIZADA", new Cliente("Ana Perez", "12345678", null, null), habitacion());
        when(reservaRepository.findById(8)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalArgumentException.class, () -> reservaService.cancelar(8));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void calculaNochesEImporteDeLaReserva() {
        Reserva reserva = new Reserva(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 13),
                "CONFIRMADA", null, habitacion());

        assertEquals(3, reserva.getNoches());
        assertEquals(360.0, reserva.getTotal(), 0.001);
    }

    private Habitacion habitacion() {
        TipoHabitacion tipo = new TipoHabitacion("Simple", 120.0);
        Habitacion habitacion = new Habitacion("101", 1, "DISPONIBLE", tipo);
        habitacion.setIdHabitacion(1);
        return habitacion;
    }
}
