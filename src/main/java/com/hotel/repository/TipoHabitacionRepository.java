package com.hotel.repository;

import com.hotel.modelo.TipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdTipoHabitacionNot(String nombre, Integer idTipoHabitacion);
}
