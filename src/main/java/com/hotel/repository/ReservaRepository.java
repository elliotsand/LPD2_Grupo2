package com.hotel.repository;

import com.hotel.modelo.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query("select count(r) > 0 from Reserva r " +
            "where r.habitacion.idHabitacion = :habitacionId " +
            "and r.estado <> 'CANCELADA' " +
            "and r.fechaEntrada < :fechaSalida " +
            "and r.fechaSalida > :fechaEntrada")
    boolean existeCruce(@Param("habitacionId") Integer habitacionId,
                        @Param("fechaEntrada") LocalDate fechaEntrada,
                        @Param("fechaSalida") LocalDate fechaSalida);
}
