package com.Huellitas.Huellitas.repository;

import com.Huellitas.Huellitas.modelo.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/*
 * Consultas a la tabla mascota (Spring Data genera el SQL por el nombre del método).
 */
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findAllByOrderByFechaPublicacionDesc();

    // Catálogo público filtrado por estado (ej. solo "DISPONIBLE")
    List<Mascota> findByEstadoOrderByFechaPublicacionDesc(String estado);

    // Mascotas publicadas por un usuario específico (panel del publicador)
    List<Mascota> findByPublicadorIdOrderByFechaPublicacionDesc(Long publicadorId);
}
