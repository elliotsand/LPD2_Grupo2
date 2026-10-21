package com.hotel.repository;

import com.hotel.modelo.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /** Hay cruce si otra reserva CONFIRMADA de la misma habitación se superpone con el intervalo. */
    @Query("select count(r) > 0 from Reserva r " +
            "where r.habitacion.idHabitacion = :habitacionId " +
            "and r.estado = 'CONFIRMADA' " +
            "and r.fechaEntrada < :fechaSalida " +
            "and r.fechaSalida > :fechaEntrada")
    boolean existeCruce(@Param("habitacionId") Integer habitacionId,
                        @Param("fechaEntrada") LocalDate fechaEntrada,
                        @Param("fechaSalida") LocalDate fechaSalida);

    /** ¿La habitación tiene otra reserva confirmada que aún no termina? */
    @Query("select count(r) > 0 from Reserva r " +
            "where r.habitacion.idHabitacion = :habitacionId " +
            "and r.estado = 'CONFIRMADA' " +
            "and r.fechaSalida > :hoy " +
            "and r.idReserva <> :excluirId")
    boolean tieneOtraReservaVigente(@Param("habitacionId") Integer habitacionId,
                                    @Param("hoy") LocalDate hoy,
                                    @Param("excluirId") Integer excluirId);

    /** Consulta de reservas registradas con filtros opcionales. */
    @Query("select r from Reserva r " +
            "where (:desde is null or r.fechaEntrada >= :desde) " +
            "and (:hasta is null or r.fechaEntrada <= :hasta) " +
            "and (:estado is null or r.estado = :estado) " +
            "and (:habitacionId is null or r.habitacion.idHabitacion = :habitacionId) " +
            "and (:texto is null or lower(r.cliente.nombre) like lower(concat('%', :texto, '%')) " +
            "     or r.cliente.documento like concat('%', :texto, '%')) " +
            "order by r.fechaEntrada desc, r.idReserva desc")
    List<Reserva> buscar(@Param("desde") LocalDate desde,
                         @Param("hasta") LocalDate hasta,
                         @Param("estado") String estado,
                         @Param("habitacionId") Integer habitacionId,
                         @Param("texto") String texto);

    List<Reserva> findAllByOrderByFechaEntradaDesc();

    long countByEstadoAndFechaSalidaAfter(String estado, LocalDate fecha);

    long countByHabitacionIdHabitacionAndEstadoNot(Integer habitacionId, String estado);
}
