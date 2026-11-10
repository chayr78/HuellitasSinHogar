package com.Huellitas.Huellitas.service;

import com.Huellitas.Huellitas.modelo.Mascota;
import com.Huellitas.Huellitas.modelo.SolicitudAdopcion;
import com.Huellitas.Huellitas.modelo.Usuario;
import com.Huellitas.Huellitas.repository.MascotaRepository;
import com.Huellitas.Huellitas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/*
 * Con el correo desactivado no se consulta nada ni se llama a Brevo, y el HTML de los correos
 * escapa lo que escriben los usuarios (nadie puede meter etiquetas en el correo de otro).
 */
class EmailServiceTest {

    private static EmailService servicio(boolean habilitado, String apiKey, String remitente,
                                         MascotaRepository mascotas, UsuarioRepository usuarios) {
        return new EmailService(habilitado, apiKey, remitente, "Huellitas Sin Hogar",
                "https://huellitas.test/", mascotas, usuarios);
    }

    private static SolicitudAdopcion solicitud() {
        SolicitudAdopcion s = new SolicitudAdopcion();
        s.setId(1L);
        s.setMascotaId(1L);
        s.setAdoptanteId(20L);
        s.setNombreContacto("Ana <script>alert(1)</script>");
        s.setTelefonoContacto("5512345678");
        s.setCorreoContacto("ana@correo.com");
        s.setMensaje("Me encantan los \"perros\" & gatos");
        s.setPorcentajeCompatibilidad((short) 75);
        s.setExperienciaPrevia(true);
        return s;
    }

    private static Mascota mascota(String fotoUrl) {
        Mascota m = new Mascota();
        m.setId(1L);
        m.setNombre("Rocky");
        m.setPublicadorId(7L);
        m.setEspecie("PERRO");
        m.setTamano("MEDIANO");
        m.setEdadAproximada("2 años");
        m.setFotoUrl(fotoUrl);
        return m;
    }

    @Test
    void desactivadoNoConsultaNiEnvia() {
        MascotaRepository mascotas = mock(MascotaRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        EmailService email = servicio(false, "clave", "remitente@huellitas.test", mascotas, usuarios);

        assertFalse(email.estaActivo());
        assertDoesNotThrow(() -> {
            email.enviarConfirmacionAdoptante(solicitud());
            email.enviarSolicitudPublicador(solicitud());
            email.enviarCambioEstadoAdoptante(solicitud());
            email.enviarRestablecimiento("ana@correo.com", "Ana", "https://huellitas.test/restablecer?token=x", 30);
        });
        verifyNoInteractions(mascotas, usuarios);
    }

    @Test
    void sinClaveOSinRemitenteNoEstaActivo() {
        assertFalse(servicio(true, "", "remitente@huellitas.test", null, null).estaActivo());
        assertFalse(servicio(true, "clave", " ", null, null).estaActivo());
        assertTrue(servicio(true, "clave", "remitente@huellitas.test", null, null).estaActivo());
    }

    @Test
    void unaClaveSmtpNoActivaElCorreo() {
        // La API de Brevo rechaza las claves SMTP: el sitio no debe prometer un correo que no saldrá
        assertFalse(servicio(true, "xsmtpsib-abc123", "remitente@huellitas.test", null, null).estaActivo());
        assertTrue(servicio(true, "xkeysib-abc123", "remitente@huellitas.test", null, null).estaActivo());
    }

    @Test
    void elCambioDeEstadoNoSuponeQuienPublica() {
        SolicitudAdopcion contactada = solicitud();
        contactada.setEstado("CONTACTADA");
        String html = EmailService.htmlCambioEstado(contactada, mascota(null), "https://huellitas.test/mis-solicitudes");
        assertFalse(html.contains("de él"));
        assertTrue(html.contains("<strong>contactada</strong>"));
    }

    @Test
    void elHtmlEscapaElTextoDeLosUsuarios() {
        Usuario publicador = new Usuario();
        publicador.setNombre("Refugio <b>Patitas</b>");
        String adoptante = EmailService.htmlConfirmacionAdoptante(solicitud(), mascota(null), publicador);
        String aviso = EmailService.htmlSolicitudPublicador(solicitud(), mascota(null), "https://huellitas.test/panel");

        for (String html : new String[]{adoptante, aviso}) {
            assertFalse(html.contains("<script>"));
            assertTrue(html.contains("&lt;script&gt;"));
            assertTrue(html.contains("&quot;perros&quot; &amp; gatos"));
        }
        assertFalse(adoptante.contains("<b>Patitas</b>"));
        assertTrue(adoptante.contains("El publicador se pondrá en contacto contigo"));
        assertTrue(aviso.contains("https://huellitas.test/panel"));
    }

    @Test
    void laFotoSoloApareceSiLaMascotaTiene() {
        Usuario publicador = new Usuario();
        publicador.setNombre("Refugio");
        String conFoto = EmailService.htmlConfirmacionAdoptante(solicitud(),
                mascota("https://ejemplo.supabase.co/storage/v1/object/public/mascotas/7/foto.jpg"), publicador);
        String sinFoto = EmailService.htmlConfirmacionAdoptante(solicitud(), mascota(null), publicador);

        assertTrue(conFoto.contains("<img src=\"https://ejemplo.supabase.co/storage/v1/object/public/mascotas/7/foto.jpg\""));
        assertTrue(conFoto.contains("max-width:400px"));
        assertFalse(sinFoto.contains("<img"));
    }
}
