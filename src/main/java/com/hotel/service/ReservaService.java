package com.hotel.service;

import com.hotel.modelo.Cliente;
import com.hotel.modelo.Habitacion;
import com.hotel.modelo.Reserva;
import com.hotel.repository.ClienteRepository;
import com.hotel.repository.HabitacionRepository;
import com.hotel.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de las reservas.
 * El registro de una reserva es el caso de uso transaccional: valida, guarda la reserva
 * y actualiza el estado de la habitación en una sola transacción (todo o nada).
 */
@Service
public class ReservaService {

    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String FINALIZADA = "FINALIZADA";
    public static final int MAXIMO_NOCHES = 30;

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
        return reservaRepository.findAllByOrderByFechaEntradaDesc();
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

    /** Consulta de reservas registradas con filtros opcionales. */
    public List<Reserva> buscar(LocalDate desde, LocalDate hasta, String estado, Integer habitacionId, String texto) {
        String estadoFiltro = (estado == null || estado.isBlank()) ? null : estado;
        String textoFiltro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return reservaRepository.buscar(desde, hasta, estadoFiltro, habitacionId, textoFiltro);
    }

    public List<Habitacion> disponibles(LocalDate fechaEntrada, LocalDate fechaSalida) {
        return disponibles(fechaEntrada, fechaSalida, null);
    }

    /** Consulta de disponibilidad: habitaciones sin cruce de fechas y que no están en mantenimiento. */
    public List<Habitacion> disponibles(LocalDate fechaEntrada, LocalDate fechaSalida, Integer idTipoHabitacion) {
        validarFechas(fechaEntrada, fechaSalida, false);

        return habitacionRepository.findAll().stream()
                .filter(habitacion -> !"MANTENIMIENTO".equalsIgnoreCase(habitacion.getEstado()))
                .filter(habitacion -> idTipoHabitacion == null || (habitacion.getTipoHabitacion() != null
                        && idTipoHabitacion.equals(habitacion.getTipoHabitacion().getIdTipoHabitacion())))
                .filter(habitacion -> !reservaRepository.existeCruce(
                        habitacion.getIdHabitacion(), fechaEntrada, fechaSalida))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Reserva registrar(LocalDate fechaEntrada, LocalDate fechaSalida,
                             Integer clienteId, Integer habitacionId) {
        validarFechas(fechaEntrada, fechaSalida, true);
        if (clienteId == null || habitacionId == null) {
            throw new IllegalArgumentException("Selecciona un cliente y una habitación.");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un cliente válido."));
        // Bloquea la habitación hasta terminar la transacción para que dos usuarios
        // no puedan reservarla al mismo tiempo.
        Habitacion habitacion = habitacionRepository.bloquearPorId(habitacionId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona una habitación válida."));

        if ("MANTENIMIENTO".equalsIgnoreCase(habitacion.getEstado())) {
            throw new IllegalArgumentException("La habitación está en mantenimiento.");
        }
        if (reservaRepository.existeCruce(habitacionId, fechaEntrada, fechaSalida)) {
            throw new IllegalArgumentException("La habitación ya tiene una reserva en esas fechas.");
        }

        Reserva reserva = new Reserva(fechaEntrada, fechaSalida, CONFIRMADA, cliente, habitacion);
        habitacion.setEstado("RESERVADA");
        habitacionRepository.save(habitacion);
        return reservaRepository.save(reserva);
    }

    /** Cancela una reserva confirmada y libera la habitación si no tiene otra reserva vigente. */
    @Transactional(rollbackFor = Exception.class)
    public Reserva cancelar(Integer idReserva) {
        return cambiarEstado(idReserva, CANCELADA);
    }

    /** Finaliza (check-out) una reserva confirmada y libera la habitación. */
    @Transactional(rollbackFor = Exception.class)
    public Reserva finalizar(Integer idReserva) {
        return cambiarEstado(idReserva, FINALIZADA);
    }

    private Reserva cambiarEstado(Integer idReserva, String nuevoEstado) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe."));
        if (!CONFIRMADA.equals(reserva.getEstado())) {
            throw new IllegalArgumentException("Solo se puede modificar una reserva en estado CONFIRMADA.");
        }
        reserva.setEstado(nuevoEstado);
        Habitacion habitacion = reserva.getHabitacion();
        if ("RESERVADA".equals(habitacion.getEstado())
                && !reservaRepository.tieneOtraReservaVigente(habitacion.getIdHabitacion(), LocalDate.now(), idReserva)) {
            habitacion.setEstado("DISPONIBLE");
            habitacionRepository.save(habitacion);
        }
        return reservaRepository.save(reserva);
    }

    private void validarFechas(LocalDate fechaEntrada, LocalDate fechaSalida, boolean registro) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new IllegalArgumentException("Debes indicar las fechas de entrada y salida.");
        }
        if (registro && fechaEntrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser anterior a hoy.");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        if (ChronoUnit.DAYS.between(fechaEntrada, fechaSalida) > MAXIMO_NOCHES) {
            throw new IllegalArgumentException("Una reserva no puede superar " + MAXIMO_NOCHES + " noches.");
        }
    }
}
