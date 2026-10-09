package com.Huellitas.Huellitas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/*
 * Entidad JPA mapeada a la tabla "mascota" en Supabase.
 */
@Entity
@Table(name = "mascota")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "publicador_id", nullable = false)
    private Long publicadorId;

    // PERRO / GATO
    @Column(nullable = false, length = 10)
    private String especie;

    @Column(name = "edad_aproximada", length = 30)
    private String edadAproximada;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    // CHICO / MEDIANO / GRANDE
    @Column(nullable = false, length = 10)
    private String tamano;

    // BAJA / MEDIA / ALTA
    @Column(name = "nivel_energia", nullable = false, length = 10)
    private String nivelEnergia;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // DISPONIBLE / EN_PROCESO / ADOPTADO
    @Column(nullable = false, length = 15)
    private String estado = Constantes.MASCOTA_DISPONIBLE;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    public Mascota() {}

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

    public Long getPublicadorId() {
        return publicadorId;
    }

    public void setPublicadorId(Long publicadorId) {
        this.publicadorId = publicadorId;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getEdadAproximada() {
        return edadAproximada;
    }

    public void setEdadAproximada(String edadAproximada) {
        this.edadAproximada = edadAproximada;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getTamano() {
        return tamano;
    }

    public void setTamano(String tamano) {
        this.tamano = tamano;
    }

    public String getNivelEnergia() {
        return nivelEnergia;
    }

    public void setNivelEnergia(String nivelEnergia) {
        this.nivelEnergia = nivelEnergia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
}
