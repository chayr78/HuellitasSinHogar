-- Agrega el sexo de la mascota (MACHO / HEMBRA).
-- Es opcional en la tabla porque las mascotas ya publicadas no lo tienen;
-- la API lo exige al crear o editar.
--
-- Ejecutar en el SQL Editor de Supabase ANTES de desplegar el backend
-- (con ddl-auto=validate la app no arranca si falta la columna).

ALTER TABLE mascota
    ADD COLUMN IF NOT EXISTS sexo VARCHAR(10);

ALTER TABLE mascota
    DROP CONSTRAINT IF EXISTS ck_mascota_sexo;

ALTER TABLE mascota
    ADD CONSTRAINT ck_mascota_sexo CHECK (sexo IN ('MACHO', 'HEMBRA'));
