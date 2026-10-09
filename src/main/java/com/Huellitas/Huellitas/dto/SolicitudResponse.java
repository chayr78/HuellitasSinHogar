package com.Huellitas.Huellitas.dto;

import java.time.LocalDateTime;

/*
 * Respuesta de los endpoints de solicitudes: todos los datos de la solicitud
 * más el nombre y la foto de la mascota, el nombre del publicador y el nombre y la foto del adoptante.
 * Es un record: clase de solo datos; Java crea el constructor y un método por campo, p. ej. nombre().
 */
public record SolicitudResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        String mascotaFotoUrl,
        String publicadorNombre,
        Long adoptanteId,
        String nombreContacto,
        String telefonoContacto,
        String correoContacto,
        String mensaje,
        String estado,
        LocalDateTime fechaSolicitud,
        String tipoVivienda,
        String horasDisponibles,
        Boolean experienciaPrevia,
        Boolean ninosPequenos,
        Boolean otrasMascotas,
        Short porcentajeCompatibilidad,
        Boolean leidaPublicador,
        Boolean leidaAdoptante,
        String adoptanteNombre,
        String adoptanteFotoUrl
) {}
