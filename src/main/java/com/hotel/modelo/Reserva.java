package com.hotel.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReserva;

    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private String estado;

    @ManyToOne
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "idHabitacion")
    private Habitacion habitacion;

    public Reserva() {
    }

    public Reserva(LocalDate fechaEntrada, LocalDate fechaSalida, String estado,
                   Cliente cliente, Habitacion habitacion) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = estado;
        this.cliente = cliente;
        this.habitacion = habitacion;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public void setHabitacion(Habitacion habitacion) {
        this.habitacion = habitacion;
    }

    /** Número de noches de la reserva (no se guarda en la BD). */
    @Transient
    public long getNoches() {
        if (fechaEntrada == null || fechaSalida == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /** Importe estimado: noches x tarifa del tipo de habitación. */
    @Transient
    public double getTotal() {
        if (habitacion == null || habitacion.getTipoHabitacion() == null
                || habitacion.getTipoHabitacion().getPrecio() == null) {
            return 0;
        }
        return getNoches() * habitacion.getTipoHabitacion().getPrecio();
    }
}
