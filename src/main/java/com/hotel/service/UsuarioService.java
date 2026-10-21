package com.hotel.service;

import com.hotel.modelo.Usuario;
import com.hotel.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    /** Guarda el usuario cifrando la contraseña con BCrypt si aún no lo está. */
    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !esHashBcrypt(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public void eliminar(Integer id) {
        usuarioRepository.deleteById(id);
    }

    /**
     * Valida usuario y contraseña. Las contraseñas se comparan con BCrypt.
     * Si la base aún tiene la contraseña en texto plano (datos iniciales del SQL),
     * se acepta una vez y se reemplaza por su hash automáticamente.
     */
    @Transactional
    public Optional<Usuario> autenticar(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(username.trim()).filter(usuario -> {
            String guardada = usuario.getPassword();
            if (guardada == null) {
                return false;
            }
            if (esHashBcrypt(guardada)) {
                return passwordEncoder.matches(password, guardada);
            }
            if (guardada.equals(password)) {
                usuario.setPassword(passwordEncoder.encode(password));
                usuarioRepository.save(usuario);
                return true;
            }
            return false;
        });
    }

    private boolean esHashBcrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
    }
}
