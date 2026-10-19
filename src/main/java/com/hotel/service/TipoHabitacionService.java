package com.hotel.service;

import com.hotel.modelo.TipoHabitacion;
import com.hotel.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TipoHabitacionService {

    private final TipoHabitacionRepository tipoHabitacionRepository;

    public TipoHabitacionService(TipoHabitacionRepository tipoHabitacionRepository) {
        this.tipoHabitacionRepository = tipoHabitacionRepository;
    }

    public List<TipoHabitacion> listar() {
        return tipoHabitacionRepository.findAll();
    }

    public Optional<TipoHabitacion> buscarPorId(Integer id) {
        return tipoHabitacionRepository.findById(id);
    }

    public TipoHabitacion guardar(TipoHabitacion tipoHabitacion) {
        return tipoHabitacionRepository.save(tipoHabitacion);
    }

    public void eliminar(Integer id) {
        tipoHabitacionRepository.deleteById(id);
    }
}