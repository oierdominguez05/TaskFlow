package src;

public enum EstadoTrabajo {

	PENDIENTE(false), EN_COLA(false), EJECUTANDO(false), COMPLETADO(true), FALLIDO(true), CANCELADO(true);

	private final boolean estadoFinal;

	EstadoTrabajo(boolean estadoFinal) {
		this.estadoFinal = estadoFinal;
	}

	public boolean isEstadoFinal() {
		return estadoFinal;
	}

	public boolean puedeTransitarA(EstadoTrabajo nuevoEstado) {
		if (nuevoEstado == null || this.estadoFinal) {
			return false;
		}
		return switch (this) {
		case PENDIENTE -> nuevoEstado == EN_COLA || nuevoEstado == CANCELADO || nuevoEstado == FALLIDO;
		case EN_COLA -> nuevoEstado == EJECUTANDO || nuevoEstado == CANCELADO || nuevoEstado == FALLIDO;
		case EJECUTANDO -> nuevoEstado == COMPLETADO || nuevoEstado == FALLIDO;
		case COMPLETADO, FALLIDO, CANCELADO -> false;
		};
	}
}