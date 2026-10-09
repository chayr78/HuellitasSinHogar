package com.Huellitas.Huellitas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Entidad JPA mapeada a la tabla "solicitud_adopcion" en Supabase.
 *
 * Esta tabla concentra tanto los datos propios de la solicitud como
 * las respuestas de la encuesta de compatibilidad (opcional, campos
 * nulos si no se usó) y los flags de lectura que reemplazan una
 * tabla de notificaciones separada.
 */
@Entity
@Table(name = "solicitud_adopcion")
public class SolicitudAdopcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mascota_id", nullable = false)
    private Long mascotaId;

    @Column(name = "adoptante_id", nullable = false)
    private Long adoptanteId;

    @Column(name = "nombre_contacto", nullable = false, length = 150)
    private String nombreContacto;

    @Column(name = "telefono_contacto", nullable = false, length = 20)
    private String telefonoContacto;

    @Column(name = "correo_contacto", nullable = false, length = 150)
    private String correoContacto;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    // PENDIENTE / CONTACTADA / CERRADA
    @Column(nullable = false, length = 15)
    private String estado = Constantes.SOLICITUD_PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    // ---------- Campos de la encuesta de compatibilidad (opcional) ----------

    // DEPTO_SIN_PATIO / PATIO_PEQUENO / PATIO_GRANDE
    @Column(name = "tipo_vivienda", length = 30)
    private String tipoVivienda;

    // MENOS_2H / DE_2_A_5H / MAS_5H
    @Column(name = "horas_disponibles", length = 20)
    private String horasDisponibles;

    @Column(name = "experiencia_previa")
    private Boolean experienciaPrevia;

    @Column(name = "ninos_pequenos")
    private Boolean ninosPequenos;

    @Column(name = "otras_mascotas")
    private Boolean otrasMascotas;

    @Column(name = "porcentaje_compatibilidad")
    private Short porcentajeCompatibilidad;

    // ---------- Flags de lectura (reemplazan la tabla notificacion) ----------

    @Column(name = "leida_publicador", nullable = false)
    private Boolean leidaPublicador = false;

    @Column(name = "leida_adoptante", nullable = false)
    private Boolean leidaAdoptante = false;

    public SolicitudAdopcion() {}

    // ---------- Getters y setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(Long mascotaId) {
        this.mascotaId = mascotaId;
    }

    public Long getAdoptanteId() {
        return adoptanteId;
    }

    public void setAdoptanteId(Long adoptanteId) {
        this.adoptanteId = adoptanteId;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getTipoVivienda() {
        return tipoVivienda;
    }

    public void setTipoVivienda(String tipoVivienda) {
        this.tipoVivienda = tipoVivienda;
    }

    public String getHorasDisponibles() {
        return horasDisponibles;
    }

    public void setHorasDisponibles(String horasDisponibles) {
        this.horasDisponibles = horasDisponibles;
    }

    public Boolean getExperienciaPrevia() {
        return experienciaPrevia;
    }

    public void setExperienciaPrevia(Boolean experienciaPrevia) {
        this.experienciaPrevia = experienciaPrevia;
    }

    public Boolean getNinosPequenos() {
        return ninosPequenos;
    }

    public void setNinosPequenos(Boolean ninosPequenos) {
        this.ninosPequenos = ninosPequenos;
    }

    public Boolean getOtrasMascotas() {
        return otrasMascotas;
    }

    public void setOtrasMascotas(Boolean otrasMascotas) {
        this.otrasMascotas = otrasMascotas;
    }

    public Short getPorcentajeCompatibilidad() {
        return porcentajeCompatibilidad;
    }

    public void setPorcentajeCompatibilidad(Short porcentajeCompatibilidad) {
        this.porcentajeCompatibilidad = porcentajeCompatibilidad;
    }

    public Boolean getLeidaPublicador() {
        return leidaPublicador;
    }

    public void setLeidaPublicador(Boolean leidaPublicador) {
        this.leidaPublicador = leidaPublicador;
    }

    public Boolean getLeidaAdoptante() {
        return leidaAdoptante;
    }

    public void setLeidaAdoptante(Boolean leidaAdoptante) {
        this.leidaAdoptante = leidaAdoptante;
    }
}
