package com.hotel.service;

import com.hotel.modelo.Cliente;
import com.hotel.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(Integer id) {
        return clienteRepository.findById(id);
    }

    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    /** Valida los datos del cliente; devuelve el mensaje de error o null si todo está bien. */
    public String validar(Cliente cliente) {
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            return "Ingresa el nombre del cliente.";
        }
        String documento = cliente.getDocumento() == null ? "" : cliente.getDocumento().trim();
        if (!documento.matches("^[A-Za-z0-9]{8,12}$")) {
            return "El documento debe tener entre 8 y 12 caracteres (DNI de 8 dígitos, CE o pasaporte).";
        }
        cliente.setDocumento(documento.toUpperCase());
        boolean duplicado = cliente.getIdCliente() == null
                ? clienteRepository.existsByDocumento(cliente.getDocumento())
                : clienteRepository.existsByDocumentoAndIdClienteNot(cliente.getDocumento(), cliente.getIdCliente());
        if (duplicado) {
            return "Ese documento ya está registrado. Verifica el número e intenta de nuevo.";
        }
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()
                && !cliente.getTelefono().trim().matches("^\\+?[0-9 ]{7,15}$")) {
            return "El teléfono solo puede contener números (7 a 15 dígitos).";
        }
        if (cliente.getCorreo() != null && !cliente.getCorreo().isBlank()
                && !cliente.getCorreo().trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            return "Ingresa un correo electrónico válido.";
        }
        return null;
    }

    public void eliminar(Integer id) {
        clienteRepository.deleteById(id);
    }
}