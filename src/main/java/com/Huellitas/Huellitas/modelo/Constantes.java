package com.Huellitas.Huellitas.modelo;

import java.util.List;

/*
 * Valores fijos que se guardan en la base de datos (roles y estados).
 * Están aquí para escribirlos una sola vez y evitar errores de dedo.
 */
public final class Constantes {

    private Constantes() {}

    // ---------- Roles de usuario (columna usuario.rol) ----------
    public static final String ROL_PUBLICADOR = "PUBLICADOR";
    public static final String ROL_ADOPTANTE = "ADOPTANTE";
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    // ---------- Tipos de publicador (columna usuario.tipo_publicador) ----------
    public static final String TIPO_REFUGIO = "REFUGIO";

    // ---------- Estados de una mascota (columna mascota.estado) ----------
    public static final String MASCOTA_DISPONIBLE = "DISPONIBLE";
    public static final String MASCOTA_EN_PROCESO = "EN_PROCESO";
    public static final String MASCOTA_ADOPTADO = "ADOPTADO";

    // ---------- Estados de una solicitud (columna solicitud_adopcion.estado) ----------
    public static final String SOLICITUD_PENDIENTE = "PENDIENTE";
    public static final String SOLICITUD_CONTACTADA = "CONTACTADA";
    public static final String SOLICITUD_CERRADA = "CERRADA";

    // Una solicitud "activa" es la que todavía está en revisión
    public static final List<String> SOLICITUDES_ACTIVAS = List.of(SOLICITUD_PENDIENTE, SOLICITUD_CONTACTADA);
}
