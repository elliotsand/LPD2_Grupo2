package com.hotel.repository;

import com.hotel.modelo.Habitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Integer> {

    /** Bloquea la fila de la habitación durante la transacción de reserva (evita doble reserva simultánea). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.idHabitacion = :id")
    Optional<Habitacion> bloquearPorId(@Param("id") Integer id);

    long countByEstado(String estado);

    List<Habitacion> findAllByOrderByNumeroAsc();
}
