package com.Huellitas.Huellitas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Entidad JPA mapeada a la tabla "usuario" en Supabase.
 * Representa tanto a publicadores (refugio/particular) como
 * a adoptantes y administradores, diferenciados por el campo "rol".
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 20)
    private String telefono;

    // PUBLICADOR / ADOPTANTE / ADMINISTRADOR
    @Column(nullable = false, length = 20)
    private String rol;

    // REFUGIO / PARTICULAR (solo aplica si rol = PUBLICADOR)
    @Column(name = "tipo_publicador", length = 20)
    private String tipoPublicador;

    @Column(length = 150)
    private String ubicacion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    // Restablecimiento de contraseña: SHA-256 (hex) del token enviado por correo y su expiración
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expira")
    private LocalDateTime resetTokenExpira;

    // URL pública de la foto de perfil en Supabase Storage
    @Column(name = "foto_perfil_url", length = 255)
    private String fotoPerfilUrl;

    public Usuario() {}

    // ---------- Getters y setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTipoPublicador() {
        return tipoPublicador;
    }

    public void setTipoPublicador(String tipoPublicador) {
        this.tipoPublicador = tipoPublicador;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public LocalDateTime getResetTokenExpira() {
        return resetTokenExpira;
    }

    public void setResetTokenExpira(LocalDateTime resetTokenExpira) {
        this.resetTokenExpira = resetTokenExpira;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }
}
