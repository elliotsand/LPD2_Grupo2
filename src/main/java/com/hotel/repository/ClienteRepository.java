package com.hotel.repository;

import com.hotel.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    boolean existsByDocumento(String documento);
    boolean existsByDocumentoAndIdClienteNot(String documento, Integer idCliente);
}
