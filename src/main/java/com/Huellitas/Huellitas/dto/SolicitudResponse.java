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
        String adoptanteFotoUrl,
        // Teléfono (solo dígitos) del publicador. Solo va en la respuesta de POST /api/solicitudes,
        // que recibe el adoptante dueño de la solicitud; en las demás respuestas es null.
        String whatsappPublicador,
        // True si el correo está activado y la confirmación al adoptante va en camino (se envía en segundo
        // plano, así que no garantiza que ya llegó). Solo en POST /api/solicitudes; en las demás es null.
        Boolean correoConfirmacionEnviado
) {}
