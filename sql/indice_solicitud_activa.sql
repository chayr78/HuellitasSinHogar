-- Evita que un adoptante tenga dos solicitudes activas (PENDIENTE o CONTACTADA)
-- para la misma mascota, incluso con peticiones simultáneas (doble clic).
-- Las solicitudes CERRADA no cuentan: se puede volver a solicitar después.
--
-- Ejecutar en el SQL Editor de Supabase.

-- 1. Revisar si ya hay duplicados (el índice no se puede crear si existen).
--    Si esta consulta devuelve filas, cierra las sobrantes antes del paso 2, por ejemplo:
--    UPDATE solicitud_adopcion SET estado = 'CERRADA' WHERE id IN (...);
SELECT mascota_id, adoptante_id, array_agg(id ORDER BY fecha_solicitud) AS solicitudes
FROM solicitud_adopcion
WHERE estado IN ('PENDIENTE', 'CONTACTADA')
GROUP BY mascota_id, adoptante_id
HAVING count(*) > 1;

-- 2. Crear el índice. El nombre lo usa SolicitudService para reconocer el error.
CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitud_activa
    ON solicitud_adopcion (mascota_id, adoptante_id)
    WHERE estado IN ('PENDIENTE', 'CONTACTADA');
