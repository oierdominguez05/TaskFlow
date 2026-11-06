package src;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import taskflow.concurrencia.GestorColaTrabajos;
import taskflow.concurrencia.MotorEjecucion;
import taskflow.io.GestorFicheros;
import taskflow.modelo.EstadoTrabajo;
import taskflow.modelo.SolicitudTrabajo;

public class TaskFlowMain {

	public static void main(String[] args) throws InterruptedException {
		System.out.println("==================================================");
		System.out.println("       SISTEMA CONCURRENTE TASKFLOW (JAVA 21)     ");
		System.out.println("==================================================");

		// 1. Generar archivos de prueba iniciales si no existen
		prepararArchivosDePrueba();

		// 2. Inicializar componentes centrales
		final var gestorCola = new GestorColaTrabajos(20);
		final var motor = new MotorEjecucion(gestorCola);
		motor.iniciarTrabajadores();

		// 3. Monitor de telemetria en tiempo real (ScheduledExecutorService)
		final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
		monitor.scheduleAtFixedRate(() -> {
		    final var ocupados = motor.getTrabajadoresOcupados();
		    final var enCola = gestorCola.getTamanoActualBlockingQueue();
		    final var pendientes = gestorCola.getTamanoActualPendientesBuffer();
		    final var comp = gestorCola.contarPorEstado(EstadoTrabajo.COMPLETADO);
		    final var fall = gestorCola.contarPorEstado(EstadoTrabajo.FALLIDO);
		    final var canc = gestorCola.contarPorEstado(EstadoTrabajo.CANCELADO);


		    System.out.printf(
		            "\r[Estado] Trabajadores: %d/4 ocupados | En Cola: %d/20 | Buffer: %d | Comp: %d | Fall: %d | Canc: %d%nTaskFlow> ",
		            ocupados, enCola, pendientes, comp, fall, canc);
		}, 1, 5, TimeUnit.SECONDS); 

		// 4. Cargar solicitudes iniciales desde CSV
		System.out.println("\nCargando lote de solicitudes iniciales...");
		final var aceptadas = GestorFicheros.cargarSolicitudesCsv("src/solicitudes_100.csv", gestorCola);
		System.out.println("Solicitudes aceptadas en el arranque: " + aceptadas);

		// 5. Interfaz de consola interactiva
		final var scanner = new Scanner(System.in);
		var salir = false;

		System.out.println(
				"\nComandos disponibles: [1] Ver Estados | [2] Anadir Solicitud | [3] Cancelar Trabajo | [4] Salir y Cerrar");

		while (!salir) {
			System.out.print("\nTaskFlow> ");
			if (!scanner.hasNextLine())
				break;
			final var cmd = scanner.nextLine().trim();

			switch (cmd) {
			case "1" -> {
				System.out.println("\n--- ESTADO ACTUAL DE TRABAJOS ---");
				for (final var t : gestorCola.obtenerTodosLosTrabajos()) {
					System.out.printf("ID: %-5s | Op: %-16s | Estado: %-10s | Res: %s%n", t.getId(), t.getOperacion(),
							t.getEstado(),
							t.getEstado() == EstadoTrabajo.COMPLETADO ? t.getResultado() : t.getDescripcionError());
				}
			}
			case "2" -> {
				System.out.println("Introduce en formato CSV: ID;OPERACION;RUTA_ARCHIVO;PARAMETROS");
				System.out.print("Nueva solicitud> ");
				final var linea = scanner.nextLine();
				final var sol = SolicitudTrabajo.desdeLineaCsv(linea);
				final var t = gestorCola.validarYRegistrar(sol);
				if (t != null) {
					System.out.println("-> Solicitud admitida con exito: " + t.getId());
				} else {
					System.out.println("-> Solicitud rechazada por validacion.");
				}
			}
			case "3" -> {
				System.out.print("ID del trabajo a cancelar: ");
				final var idCanc = scanner.nextLine().trim();
				if (gestorCola.cancelarTrabajo(idCanc, "Cancelado manualmente por el usuario")) {
					System.out.println("-> Trabajo cancelado con exito.");
				} else {
					System.out.println("-> No se pudo cancelar (ya inicio ejecucion o no existe).");
				}
			}
			case "4" -> {
				System.out.println("\nIniciando secuencia de apagado ordenado...");
				salir = true;
			}
			default -> System.out.println("Comando desconocido. Opciones: 1, 2, 3 o 4.");
			}
		}

		// 6. Cierre controlado y parada de hilos
		monitor.shutdown();
		motor.cerrar();

		// 7. Exportacion de ficheros y balance final
		System.out.println("\nGenerando informes finales...");
		GestorFicheros.exportarResultados("resultados_finales.csv", gestorCola.obtenerTodosLosTrabajos());
		GestorFicheros.exportarIncidencias("incidencias_rechazos.txt", gestorCola.getHistorialRechazosValidacion());

		System.out.println("\n=== COMPROBACION DE BALANCE OBLIGATORIO ===");
		final var total = gestorCola.getTotalTrabajosAceptados();
		final var comp = gestorCola.contarPorEstado(EstadoTrabajo.COMPLETADO);
		final var fall = gestorCola.contarPorEstado(EstadoTrabajo.FALLIDO);
		final var canc = gestorCola.contarPorEstado(EstadoTrabajo.CANCELADO);

		System.out.printf("Aceptados: %d | Completados: %d | Fallidos: %d | Cancelados: %d%n", total, comp, fall, canc);
		System.out.println("¿Cumple el balance (Aceptados = Comp + Fall + Canc)?: " + (total == (comp + fall + canc)));
		System.out.println("Sistema finalizado con exito.");
	}

	private static void prepararArchivosDePrueba() {
		try {
			// Archivo de texto de prueba
			try (final var fw = new FileWriter("prueba_texto.txt")) {
				fw.write(
						"Hola mundo en Java 21\nProgramacion concurrente y multiproceso en DAM\nSistemas de servicios y procesos");
			}
			// Archivo CSV de prueba para el subproceso
			try (final var fw = new FileWriter("datos_numeros.csv")) {
				fw.write("10;20;30;40;50\n100;ERROR_DATO;200;300\n5;15;25");
			}
			// Archivo CSV con lote de solicitudes
			try (final var fw = new FileWriter("solicitudes_iniciales.csv")) {
				fw.write("ID;OPERACION;RUTA_ARCHIVO;PARAMETROS\n");
				fw.write("T01;ANALIZAR_TEXTO;prueba_texto.txt;\n");
				fw.write("T02;BUSCAR_TEXTO;prueba_texto.txt;DAM\n");
				fw.write("T03;ESTADISTICAS_CSV;datos_numeros.csv;\n");
				fw.write("T04;ANALIZAR_TEXTO;archivo_fantasma.txt;\n"); 
				fw.write("T05;ESTADISTICAS_CSV;datos_numeros.csv;\n");
			}
		} catch (IOException e) {
			System.err.println("Error creando ficheros de prueba: " + e.getMessage());
		}
	}
}