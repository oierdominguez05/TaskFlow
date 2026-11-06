package taskflow.modelo;

import java.time.Instant;
import java.util.concurrent.Future;

public class Trabajo {

	private final String id;
	private final TipoOperacion operacion;
	private final String rutaArchivo;
	private final String parametros;
	private final long instanteCreacionMs;

	private EstadoTrabajo estado;
	private long instanteInicioMs;
	private long instanteFinMs;
	private String resultado;
	private String descripcionError;
	private Future<?> futuroAsociado;

	public Trabajo(SolicitudTrabajo solicitud) {
		this.id = solicitud.getId();
		this.operacion = solicitud.getTipoOperacion();
		this.rutaArchivo = solicitud.getRutaArchivo();
		this.parametros = solicitud.getParametros();
		this.instanteCreacionMs = Instant.now().toEpochMilli();
		this.estado = EstadoTrabajo.PENDIENTE;
		this.resultado = "";
		this.descripcionError = "";
	}

	public synchronized boolean intentarPasarAEnCola() {
		if (this.estado.puedeTransitarA(EstadoTrabajo.EN_COLA)) {
			this.estado = EstadoTrabajo.EN_COLA;
			return true;
		}
		return false;
	}

	public synchronized boolean intentarIniciarEjecucion() {
		if (this.estado.puedeTransitarA(EstadoTrabajo.EJECUTANDO)) {
			this.estado = EstadoTrabajo.EJECUTANDO;
			this.instanteInicioMs = Instant.now().toEpochMilli();
			return true;
		}
		return false;
	}

	public synchronized boolean marcarCompletado(String detalleResultado) {
		if (this.estado.puedeTransitarA(EstadoTrabajo.COMPLETADO)) {
			this.estado = EstadoTrabajo.COMPLETADO;
			this.instanteFinMs = Instant.now().toEpochMilli();
			this.resultado = detalleResultado != null ? detalleResultado : "OK";
			return true;
		}
		return false;
	}

	public synchronized boolean marcarFallido(String motivoError) {
		if (this.estado.puedeTransitarA(EstadoTrabajo.FALLIDO)) {
			this.estado = EstadoTrabajo.FALLIDO;
			this.instanteFinMs = Instant.now().toEpochMilli();
			if (this.instanteInicioMs == 0L) {
				this.instanteInicioMs = this.instanteFinMs;
			}
			this.descripcionError = motivoError != null ? motivoError : "Error no especificado";
			return true;
		}
		return false;
	}

	public synchronized boolean intentarCancelar(String motivo) {
		if (this.estado.puedeTransitarA(EstadoTrabajo.CANCELADO)) {
			this.estado = EstadoTrabajo.CANCELADO;
			this.instanteFinMs = Instant.now().toEpochMilli();
			this.descripcionError = motivo != null ? motivo : "Cancelado antes de iniciar";
			if (this.futuroAsociado != null) {
				this.futuroAsociado.cancel(false);
			}
			return true;
		}
		return false;
	}

	public synchronized void setFuturoAsociado(Future<?> futuroAsociado) {
		this.futuroAsociado = futuroAsociado;
	}

	public synchronized EstadoTrabajo getEstado() {
		return estado;
	}

	public synchronized String getResultado() {
		return resultado;
	}

	public synchronized String getDescripcionError() {
		return descripcionError;
	}

	public synchronized long getDuracionMs() {
		if (instanteInicioMs > 0L && instanteFinMs >= instanteInicioMs) {
			return instanteFinMs - instanteInicioMs;
		}
		return 0L;
	}

	public String getId() {
		return id;
	}

	public TipoOperacion getOperacion() {
		return operacion;
	}

	public String getRutaArchivo() {
		return rutaArchivo;
	}

	public String getParametros() {
		return parametros;
	}

	public long getInstanteCreacionMs() {
		return instanteCreacionMs;
	}
}