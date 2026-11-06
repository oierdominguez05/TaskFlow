package taskflow.concurrencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import taskflow.modelo.EstadoTrabajo;
import taskflow.modelo.SolicitudTrabajo;
import taskflow.modelo.Trabajo;

public class GestorColaTrabajos {

	public static final int CAPACIDAD_REFERENCIA = 20;

	private final int capacidadMaximaCola;
	private final BlockingQueue<Trabajo> colaEspera;
	private final ConcurrentLinkedQueue<Trabajo> bufferPendientes;
	private final Map<String, Trabajo> registroTrabajos;
	private final Set<String> idsAceptados;
	private final List<String> historialRechazosValidacion;
	private final AtomicBoolean admisionCerrada;
	private final AtomicInteger contadorSaturacionesCola;

	public GestorColaTrabajos(int capacidadMaximaCola) {
		this.capacidadMaximaCola = capacidadMaximaCola;
		this.colaEspera = new ArrayBlockingQueue<>(capacidadMaximaCola, true);
		this.bufferPendientes = new ConcurrentLinkedQueue<>();
		this.registroTrabajos = new ConcurrentHashMap<>();
		this.idsAceptados = ConcurrentHashMap.newKeySet();
		this.historialRechazosValidacion = Collections.synchronizedList(new ArrayList<>());
		this.admisionCerrada = new AtomicBoolean(false);
		this.contadorSaturacionesCola = new AtomicInteger(0);
	}

	public synchronized Trabajo validarYRegistrar(SolicitudTrabajo solicitud) {
		if (admisionCerrada.get() || solicitud == null) {
			return null;
		}

		final var error = solicitud.validar(idsAceptados);
		if (error != null) {
			historialRechazosValidacion.add("Solicitud ID='" + solicitud.getId() + "' RECHAZADA: " + error);
			return null;
		}

		final var nuevoTrabajo = new Trabajo(solicitud);
		idsAceptados.add(nuevoTrabajo.getId());
		registroTrabajos.put(nuevoTrabajo.getId(), nuevoTrabajo);

		promoverPendientesACola();

		if (bufferPendientes.isEmpty() && colaEspera.offer(nuevoTrabajo)) {
			nuevoTrabajo.intentarPasarAEnCola();
		} else {
			contadorSaturacionesCola.incrementAndGet();
			bufferPendientes.offer(nuevoTrabajo);
		}

		return nuevoTrabajo;
	}

	public synchronized void promoverPendientesACola() {
		while (colaEspera.remainingCapacity() > 0 && !bufferPendientes.isEmpty()) {
			final var siguiente = bufferPendientes.peek();
			if (siguiente == null)
				break;

			if (siguiente.getEstado() == EstadoTrabajo.CANCELADO) {
				bufferPendientes.poll();
				continue;
			}

			if (colaEspera.offer(siguiente)) {
				bufferPendientes.poll();
				siguiente.intentarPasarAEnCola();
			} else {
				break;
			}
		}
	}

	public Trabajo tomarSiguienteTrabajo(long timeout, TimeUnit unidad) throws InterruptedException {
		promoverPendientesACola();
		final var trabajo = colaEspera.poll(timeout, unidad);
		if (trabajo != null) {
			promoverPendientesACola();
		}
		return trabajo;
	}

	public synchronized boolean cancelarTrabajo(String idTrabajo, String motivo) {
		final var trabajo = registroTrabajos.get(idTrabajo);
		if (trabajo == null) {
			return false;
		}
		final var cancelado = trabajo.intentarCancelar(motivo);
		if (cancelado) {
			colaEspera.remove(trabajo);
			bufferPendientes.remove(trabajo);
			promoverPendientesACola();
		}
		return cancelado;
	}

	public int contarPorEstado(EstadoTrabajo estado) {
		var cuenta = 0;
		for (final var t : registroTrabajos.values()) {
			if (t.getEstado() == estado) {
				cuenta++;
			}
		}
		return cuenta;
	}

	public int getTamanoActualBlockingQueue() {
		return colaEspera.size();
	}

	public int getTamanoActualPendientesBuffer() {
		return bufferPendientes.size();
	}

	public int getTotalTrabajosAceptados() {
		return registroTrabajos.size();
	}

	public Collection<Trabajo> obtenerTodosLosTrabajos() {
		return Collections.unmodifiableCollection(registroTrabajos.values());
	}

	public List<String> getHistorialRechazosValidacion() {
		synchronized (historialRechazosValidacion) {
			return new ArrayList<>(historialRechazosValidacion);
		}
	}

	public void cerrarAdmision() {
		admisionCerrada.set(true);
	}
}