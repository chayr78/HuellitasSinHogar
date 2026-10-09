package com.Huellitas.Huellitas.repository;

import com.Huellitas.Huellitas.modelo.MascotaFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/*
 * Consultas a la tabla mascota_foto (fotos adicionales de cada mascota).
 */
public interface MascotaFotoRepository extends JpaRepository<MascotaFoto, Long> {

    // Fotos de una mascota en el orden de la galería
    List<MascotaFoto> findByMascotaIdOrderByOrdenAsc(Long mascotaId);

    // Borra todas las fotos adicionales de una mascota en una sola sentencia
    @Modifying
    @Query("delete from MascotaFoto f where f.mascotaId = :mascotaId")
    void deleteByMascotaId(@Param("mascotaId") Long mascotaId);
}
