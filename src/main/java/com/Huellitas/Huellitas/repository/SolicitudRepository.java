package com.Huellitas.Huellitas.repository;

import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

/*
 * Consultas a la tabla solicitud_adopcion. Las que tienen @Query están escritas
 * a mano en JPQL; el resto las genera Spring Data por el nombre del método.
 */
public interface SolicitudRepository extends JpaRepository<SolicitudAdopcion, Long> {

    // Solicitudes recibidas para una mascota (panel del publicador)
    List<SolicitudAdopcion> findByMascotaIdOrderByFechaSolicitudDesc(Long mascotaId);

    List<SolicitudAdopcion> findByMascotaIdAndEstadoIn(Long mascotaId, Collection<String> estados);

    // Solicitudes enviadas por un adoptante
    List<SolicitudAdopcion> findByAdoptanteIdOrderByFechaSolicitudDesc(Long adoptanteId);

    // Para la advertencia de "ya tienes una solicitud activa"
    boolean existsByAdoptanteIdAndEstadoIn(Long adoptanteId, Collection<String> estados);

    // Evita solicitudes duplicadas para la misma mascota
    boolean existsByMascotaIdAndAdoptanteIdAndEstadoIn(Long mascotaId, Long adoptanteId, Collection<String> estados);

    // Notificaciones del adoptante
    List<SolicitudAdopcion> findByAdoptanteIdAndLeidaAdoptanteFalseOrderByFechaSolicitudDesc(Long adoptanteId);

    // Notificaciones del publicador: solicitudes no leídas de sus mascotas
    @Query("""
            select s from SolicitudAdopcion s
            where s.leidaPublicador = false
              and s.mascotaId in (select m.id from Mascota m where m.publicadorId = :publicadorId)
            order by s.fechaSolicitud desc
            """)
    List<SolicitudAdopcion> findNoLeidasPorPublicador(@Param("publicadorId") Long publicadorId);

    @Modifying
    @Query("delete from SolicitudAdopcion s where s.mascotaId = :mascotaId")
    void deleteByMascotaId(@Param("mascotaId") Long mascotaId);
}
