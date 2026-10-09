package com.Huellitas.Huellitas.repository;

import com.Huellitas.Huellitas.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/*
 * Consultas a la tabla usuario. Spring Data crea la consulta a partir
 * del nombre del método (por ejemplo findByCorreo = WHERE correo = ?).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Usado en el login: buscar un usuario por su correo
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRol(String rol);

    Optional<Usuario> findByResetTokenHash(String resetTokenHash);

    List<Usuario> findAllByOrderByFechaRegistroDesc();
}
