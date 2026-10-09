package com.Huellitas.Huellitas.modelo;

import jakarta.persistence.*;

/*
 * Entidad JPA mapeada a la tabla "mascota_foto" en Supabase.
 * Fotos adicionales de una mascota; la portada sigue en mascota.foto_url.
 */
@Entity
@Table(name = "mascota_foto")
public class MascotaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mascota_id", nullable = false)
    private Long mascotaId;

    @Column(name = "foto_url", nullable = false, length = 255)
    private String fotoUrl;

    // Posición de la foto en la galería: 0, 1, 2...
    @Column(nullable = false)
    private Integer orden = 0;

    public MascotaFoto() {}

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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
