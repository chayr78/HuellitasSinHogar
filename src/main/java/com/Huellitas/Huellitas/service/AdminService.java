package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.config.UsuarioAutenticado;
import com.Huellitas.Huellitas.dto.UsuarioResponse;
import com.Huellitas.Huellitas.exception.ApiException;
import com.Huellitas.Huellitas.modelo.Constantes;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * Moderación del administrador (RF-15, RF-16).
 */
@Service
public class AdminService {

    private final UsuarioRepository usuarioRepository;

    public AdminService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<UsuarioResponse> obtenerUsuarios() {
        List<UsuarioResponse> respuesta = new ArrayList<>();
        for (Usuario usuario : usuarioRepository.findAllByOrderByFechaRegistroDesc()) {
            respuesta.add(UsuarioResponse.desdeSinTelefono(usuario));
        }
        return respuesta;
    }

    // Dar de baja o reactivar una cuenta en vez de borrarla
    public UsuarioResponse cambiarActivo(Long id, boolean activo, UsuarioAutenticado admin) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            throw ApiException.noEncontrado("No existe un usuario con id: " + id);
        }

        if (!activo && usuario.getId().equals(admin.id())) {
            throw ApiException.badRequest("No puedes desactivar tu propia cuenta");
        }
        if (!activo && Constantes.ROL_ADMINISTRADOR.equals(usuario.getRol())) {
            throw ApiException.badRequest("No puedes desactivar a otro administrador");
        }

        usuario.setActivo(activo);
        return UsuarioResponse.desdeSinTelefono(usuarioRepository.save(usuario));
    }
}
