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
        when(habitacionRepository.findById(1)).thenReturn(Optional.of(habitacion));
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
        when(habitacionRepository.findById(1)).thenReturn(Optional.of(habitacion()));
        when(reservaRepository.existeCruce(1, entrada, salida)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.registrar(entrada, salida, 1, 1));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    private Habitacion habitacion() {
        TipoHabitacion tipo = new TipoHabitacion("Simple", 120.0);
        Habitacion habitacion = new Habitacion("101", 1, "DISPONIBLE", tipo);
        habitacion.setIdHabitacion(1);
        return habitacion;
    }
}
