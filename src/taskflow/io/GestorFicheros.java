package taskflow.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import taskflow.concurrencia.GestorColaTrabajos;
import taskflow.modelo.SolicitudTrabajo;
import taskflow.modelo.Trabajo;

public class GestorFicheros {

	public static int cargarSolicitudesCsv(String rutaCsv, GestorColaTrabajos gestorCola) {
		var cargadas = 0;
		try (final var br = new BufferedReader(new FileReader(rutaCsv))) {
			String linea;
			var primera = true;
			while ((linea = br.readLine()) != null) {
				linea = linea.trim();
				if (linea.isEmpty() || linea.startsWith("#"))
					continue;

				if (primera && linea.toUpperCase().startsWith("ID;")) {
					primera = false;
					continue;
				}
				primera = false;

				final var sol = SolicitudTrabajo.desdeLineaCsv(linea);
				if (gestorCola.validarYRegistrar(sol) != null) {
					cargadas++;
				}
			}
		} catch (IOException e) {
			System.err.println("Error al cargar solicitudes: " + e.getMessage());
		}
		return cargadas;
	}

	public static void exportarResultados(String rutaSalidaCsv, Collection<Trabajo> trabajos) {
		try (final var bw = new BufferedWriter(new FileWriter(rutaSalidaCsv))) {
			bw.write("ID;OPERACION;RUTA_ARCHIVO;ESTADO;DURACION_MS;RESULTADO;ERROR");
			bw.newLine();
			for (final var t : trabajos) {
				bw.write(String.format("%s;%s;%s;%s;%d;%s;%s", t.getId(), t.getOperacion(), t.getRutaArchivo(),
						t.getEstado(), t.getDuracionMs(), t.getResultado().replace(";", ","),
						t.getDescripcionError() != null ? t.getDescripcionError().replace(";", ",") : ""));
				bw.newLine();
			}
			System.out.println("-> Archivo de resultados generado en: " + rutaSalidaCsv);
		} catch (IOException e) {
			System.err.println("Error exportando resultados: " + e.getMessage());
		}
	}

	public static void exportarIncidencias(String rutaIncidencias, List<String> incidencias) {
		try (final var bw = new BufferedWriter(new FileWriter(rutaIncidencias))) {
			bw.write("=== REGISTRO DE RECHAZOS E INCIDENCIAS DE TASKFLOW ===");
			bw.newLine();
			for (final var inc : incidencias) {
				bw.write(inc);
				bw.newLine();
			}
			System.out.println("-> Archivo de incidencias generado en: " + rutaIncidencias);
		} catch (IOException e) {
			System.err.println("Error exportando incidencias: " + e.getMessage());
		}
	}
}