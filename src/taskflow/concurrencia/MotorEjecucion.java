package taskflow.concurrencia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import taskflow.modelo.TipoOperacion;
import taskflow.modelo.Trabajo;
import taskflow.procesadores.Procesadores;

public class MotorEjecucion {

	private final ExecutorService executor;
	private final GestorColaTrabajos gestorCola;
	private final Semaphore semaforoHijos;
	private final AtomicInteger trabajadoresOcupados;

	public MotorEjecucion(GestorColaTrabajos gestorCola) {
		this.gestorCola = gestorCola;
		this.executor = Executors.newFixedThreadPool(4);
		this.semaforoHijos = new Semaphore(2, true); // Maximo 2 procesos hijos simultaneos
		this.trabajadoresOcupados = new AtomicInteger(0);
	}

	public void iniciarTrabajadores() {
		for (int i = 0; i < 4; i++) {
			executor.submit(() -> {
				try {
					while (!Thread.currentThread().isInterrupted()) {
						final var trabajo = gestorCola.tomarSiguienteTrabajo(1, TimeUnit.SECONDS);
						if (trabajo != null) {
							trabajadoresOcupados.incrementAndGet();
							try {
								procesarTrabajo(trabajo);
							} finally {
								trabajadoresOcupados.decrementAndGet();
							}
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}
	}

	private void procesarTrabajo(Trabajo trabajo) {
		if (!trabajo.intentarIniciarEjecucion()) {
			return;
		}

		try {
			String res;
			if (trabajo.getOperacion() == TipoOperacion.ANALIZAR_TEXTO) {
				res = Procesadores.analizarTexto(trabajo.getRutaArchivo());
			} else if (trabajo.getOperacion() == TipoOperacion.BUSCAR_TEXTO) {
				res = Procesadores.buscarTexto(trabajo.getRutaArchivo(), trabajo.getParametros());
			} else if (trabajo.getOperacion() == TipoOperacion.ESTADISTICAS_CSV) {
				semaforoHijos.acquire();
				try {
					res = Procesadores.ejecutarProcesoHijo(trabajo.getRutaArchivo());
				} finally {
					semaforoHijos.release();
				}
			} else {
				throw new Exception("Operacion no soportada");
			}
			trabajo.marcarCompletado(res);
		} catch (Exception e) {
			trabajo.marcarFallido(e.getMessage());
		}
	}

	public int getTrabajadoresOcupados() {
		return trabajadoresOcupados.get();
	}

	public void cerrar() throws InterruptedException {
		gestorCola.cerrarAdmision();
		executor.shutdown();
		if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
			executor.shutdownNow();
		}
	}
}