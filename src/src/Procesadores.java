package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class Procesadores {

    public static String analizarTexto(String rutaArchivo) throws Exception {
        final var archivo = new File(rutaArchivo);
        if (!archivo.exists() || !archivo.isFile()) {
            throw new Exception("El archivo no existe: " + rutaArchivo);
        }

        long lineas = 0;
        long palabras = 0;
        long caracteres = 0;

        try (final var br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas++;
                caracteres += linea.length();
                final var palabrasLinea = linea.trim().split("\\s+");
                for (final var p : palabrasLinea) {
                    if (!p.isBlank()) palabras++;
                }
            }
        }

        return String.format("Lineas=%d, Palabras=%d, Caracteres=%d", lineas, palabras, caracteres);
    }

    public static String buscarTexto(String rutasArchivos, String termino) throws Exception {
        if (termino == null || termino.isBlank()) {
            throw new Exception("Termino de busqueda vacio.");
        }

        final var rutas = rutasArchivos.split(",");
        final var sb = new StringBuilder();

        for (final var ruta : rutas) {
            final var archivo = new File(ruta.trim());
            if (!archivo.exists() || !archivo.isFile()) {
                throw new Exception("Archivo no encontrado: " + ruta.trim());
            }

            long apariciones = 0;
            try (final var br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    var idx = 0;
                    while ((idx = linea.indexOf(termino, idx)) != -1) {
                        apariciones++;
                        idx += termino.length();
                    }
                }
            }
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append(archivo.getName()).append(": ").append(apariciones).append(" coincidencias");
        }

        return sb.toString();
    }

    public static String ejecutarProcesoHijo(String rutaCsv) throws Exception {
        final var archivo = new File(rutaCsv);
        if (!archivo.exists()) {
            throw new Exception("El archivo CSV no existe: " + rutaCsv);
        }

        final var javaBin = ProcessHandle.current().info().command().orElse("java");

        // Usamos -cp bin para garantizar compatibilidad con el entorno de Eclipse
        final var pb = new ProcessBuilder(javaBin, "-cp", "bin", "taskflow.procesadores.EstadisticasCsvHijo", rutaCsv);
        pb.redirectErrorStream(true);

        final var proceso = pb.start();
        final var sb = new StringBuilder();

        try (final var br = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append(" ");
            }
        }

        if (!proceso.waitFor(10, TimeUnit.SECONDS)) {
            proceso.destroyForcibly();
            throw new Exception("Timeout excedido (10s) en proceso hijo.");
        }

        if (proceso.exitValue() != 0) {
            throw new Exception("Error en proceso hijo (Exit code " + proceso.exitValue() + "): " + sb.toString().trim());
        }

        return sb.toString().trim();
    }
}