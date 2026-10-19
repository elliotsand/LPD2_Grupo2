package com.hotel.service;

import com.hotel.modelo.Habitacion;
import com.hotel.repository.HabitacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;

    public HabitacionService(HabitacionRepository habitacionRepository) {
        this.habitacionRepository = habitacionRepository;
    }

    public List<Habitacion> listar() {
        return habitacionRepository.findAll();
    }

    public Optional<Habitacion> buscarPorId(Integer id) {
        return habitacionRepository.findById(id);
    }

    public Habitacion guardar(Habitacion habitacion) {
        return habitacionRepository.save(habitacion);
    }

    public void eliminar(Integer id) {
        habitacionRepository.deleteById(id);
    }
}