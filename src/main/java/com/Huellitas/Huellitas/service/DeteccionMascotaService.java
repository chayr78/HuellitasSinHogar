package com.Huellitas.Huellitas.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;

/*
 * Revisa si una foto tiene un perro o un gato con el modelo MobileNetV2 (ImageNet, 1000 clases)
 * corriendo en el mismo servidor con ONNX Runtime. Sin API externa ni llaves.
 * El modelo se carga una sola vez al arrancar y la sesión se reutiliza en cada petición
 * (OrtSession admite varias peticiones a la vez).
 */
@Service
public class DeteccionMascotaService {

    private static final Logger log = LoggerFactory.getLogger(DeteccionMascotaService.class);

    private static final String RUTA_MODELO = "/modelos/mobilenetv2-12.onnx";
    private static final int TAMANO_ENTRADA = 224;

    // Clases ImageNet 151 a 293: perros de todas las razas, lobos, coyotes, zorros, gatos y felinos grandes.
    // Se incluyen los parecidos porque en fotos oscuras el modelo confunde perros con lobos o leones.
    private static final int PRIMERA_CLASE_MASCOTA = 151;
    private static final int ULTIMA_CLASE_MASCOTA = 293;

    // Normalización con la que se entrenó el modelo (valores de ImageNet)
    private static final float[] MEDIA = {0.485f, 0.456f, 0.406f};
    private static final float[] DESVIACION = {0.229f, 0.224f, 0.225f};

    private final double umbral;
    private final boolean permitirSiFalla;
    private final OrtEnvironment entorno;
    // null si el modelo no se pudo cargar
    private final OrtSession sesion;

    public DeteccionMascotaService(@Value("${huellitas.deteccion-mascota.umbral:0.30}") double umbral,
                                   @Value("${huellitas.deteccion-mascota.permitir-si-falla:true}") boolean permitirSiFalla) {
        this.umbral = umbral;
        this.permitirSiFalla = permitirSiFalla;
        this.entorno = OrtEnvironment.getEnvironment();
        this.sesion = cargarModelo();
    }

    // true si la foto tiene un perro o gato. Si el modelo falla, decide permitir-si-falla.
    public boolean contieneMascota(byte[] imagen) {
        try {
            double probabilidad = probabilidadMascota(imagen);
            log.debug("Probabilidad de perro o gato: {}", probabilidad);
            return probabilidad >= umbral;
        } catch (Exception e) {
            log.error("No se pudo revisar si la foto tiene una mascota; permitir-si-falla={}", permitirSiFalla, e);
            return permitirSiFalla;
        }
    }

    // Suma de probabilidades (0 a 1) de las clases de perros y gatos
    double probabilidadMascota(byte[] imagen) throws IOException, OrtException {
        if (sesion == null) {
            throw new IllegalStateException("El modelo de detección no está cargado");
        }
        float[] entrada = preprocesar(leerImagen(imagen));
        long[] forma = {1, 3, TAMANO_ENTRADA, TAMANO_ENTRADA};
        String nombreEntrada = sesion.getInputNames().iterator().next();

        try (OnnxTensor tensor = OnnxTensor.createTensor(entorno, FloatBuffer.wrap(entrada), forma);
             OrtSession.Result resultado = sesion.run(Map.of(nombreEntrada, tensor))) {
            float[] logits = ((float[][]) resultado.get(0).getValue())[0];
            return sumarClasesMascota(softmax(logits));
        }
    }

    @PreDestroy
    public void cerrar() throws OrtException {
        if (sesion != null) {
            sesion.close();
        }
    }

    // ---------- Utilidades privadas ----------

    private OrtSession cargarModelo() {
        try (InputStream entrada = DeteccionMascotaService.class.getResourceAsStream(RUTA_MODELO)) {
            if (entrada == null) {
                log.error("No se encontró el modelo {}; la detección de mascotas queda desactivada", RUTA_MODELO);
                return null;
            }
            return entorno.createSession(entrada.readAllBytes(), new OrtSession.SessionOptions());
        } catch (IOException | OrtException e) {
            log.error("No se pudo cargar el modelo {}; la detección de mascotas queda desactivada", RUTA_MODELO, e);
            return null;
        }
    }

    /*
     * Decodifica la imagen. Las fotos grandes se leen saltando píxeles (submuestreo)
     * para no gastar decenas de MB de memoria en una imagen que terminará en 224x224.
     */
    private static BufferedImage leerImagen(byte[] bytes) throws IOException {
        try (ImageInputStream flujo = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(flujo);
            if (!lectores.hasNext()) {
                throw new IOException("Formato de imagen no reconocido");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(flujo, true, true);
                int ladoMenor = Math.min(lector.getWidth(0), lector.getHeight(0));
                // Deja al menos el doble del tamaño final para que la reducción se vea bien
                int salto = Math.max(1, ladoMenor / (TAMANO_ENTRADA * 2));
                ImageReadParam parametros = lector.getDefaultReadParam();
                parametros.setSourceSubsampling(salto, salto, 0, 0);
                return lector.read(0, parametros);
            } finally {
                lector.dispose();
            }
        }
    }

    // Redimensiona a 224x224 en RGB y normaliza en formato NCHW: primero todo el rojo, luego verde y azul
    private static float[] preprocesar(BufferedImage original) {
        BufferedImage rgb = new BufferedImage(TAMANO_ENTRADA, TAMANO_ENTRADA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, TAMANO_ENTRADA, TAMANO_ENTRADA, null);
        g.dispose();

        int pixeles = TAMANO_ENTRADA * TAMANO_ENTRADA;
        float[] datos = new float[3 * pixeles];
        for (int y = 0; y < TAMANO_ENTRADA; y++) {
            for (int x = 0; x < TAMANO_ENTRADA; x++) {
                int color = rgb.getRGB(x, y);
                int i = y * TAMANO_ENTRADA + x;
                datos[i] = (((color >> 16) & 0xFF) / 255f - MEDIA[0]) / DESVIACION[0];
                datos[pixeles + i] = (((color >> 8) & 0xFF) / 255f - MEDIA[1]) / DESVIACION[1];
                datos[2 * pixeles + i] = ((color & 0xFF) / 255f - MEDIA[2]) / DESVIACION[2];
            }
        }
        return datos;
    }

    // Convierte las salidas del modelo en probabilidades que suman 1
    private static double[] softmax(float[] logits) {
        float maximo = Float.NEGATIVE_INFINITY;
        for (float valor : logits) {
            maximo = Math.max(maximo, valor);
        }
        double[] probabilidades = new double[logits.length];
        double suma = 0;
        for (int i = 0; i < logits.length; i++) {
            // Restar el máximo evita desbordes sin cambiar el resultado
            probabilidades[i] = Math.exp(logits[i] - maximo);
            suma += probabilidades[i];
        }
        for (int i = 0; i < probabilidades.length; i++) {
            probabilidades[i] /= suma;
        }
        return probabilidades;
    }

    private static double sumarClasesMascota(double[] probabilidades) {
        double suma = 0;
        for (int i = PRIMERA_CLASE_MASCOTA; i <= ULTIMA_CLASE_MASCOTA; i++) {
            suma += probabilidades[i];
        }
        return suma;
    }
}
