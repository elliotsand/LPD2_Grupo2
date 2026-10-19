package com.hotel.service;

import com.hotel.modelo.Reserva;
import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final HabitacionRepository habitacionRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          ClienteRepository clienteRepository,
                          HabitacionRepository habitacionRepository) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.habitacionRepository = habitacionRepository;
    }

    public List<Reserva> listar() {
        return reservaRepository.findAll();
    }

    public Optional<Reserva> buscarPorId(Integer id) {
        return reservaRepository.findById(id);
    }

    public Reserva guardar(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public void eliminar(Integer id) {
        reservaRepository.deleteById(id);
    }

    public List<Habitacion> disponibles(LocalDate fechaEntrada, LocalDate fechaSalida) {
        if (fechaEntrada == null || fechaSalida == null || !fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }

        return habitacionRepository.findAll().stream()
                .filter(habitacion -> !"MANTENIMIENTO".equalsIgnoreCase(habitacion.getEstado()))
                .filter(habitacion -> !reservaRepository.existeCruce(
                        habitacion.getIdHabitacion(), fechaEntrada, fechaSalida))
                .collect(Collectors.toList());
    }

    @Transactional
    public Reserva registrar(LocalDate fechaEntrada, LocalDate fechaSalida,
                             Integer clienteId, Integer habitacionId) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new IllegalArgumentException("Debes indicar las fechas de entrada y salida.");
        }
        if (fechaEntrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser anterior a hoy.");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un cliente válido."));
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona una habitación válida."));

        if ("MANTENIMIENTO".equalsIgnoreCase(habitacion.getEstado())) {
            throw new IllegalArgumentException("La habitación está en mantenimiento.");
        }
        if (reservaRepository.existeCruce(habitacionId, fechaEntrada, fechaSalida)) {
            throw new IllegalArgumentException("La habitación ya tiene una reserva en esas fechas.");
        }

        Reserva reserva = new Reserva(fechaEntrada, fechaSalida, "CONFIRMADA", cliente, habitacion);
        habitacion.setEstado("RESERVADA");
        habitacionRepository.save(habitacion);
        return reservaRepository.save(reserva);
    }
}
