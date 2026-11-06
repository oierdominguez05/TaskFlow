package src;

public enum TipoOperacion {

	ANALIZAR_TEXTO("Cuenta líneas, palabras y caracteres de un archivo de texto", false),
	BUSCAR_TEXTO("Busca un término o expresión literal en uno o varios archivos", false),
	ESTADISTICAS_CSV("Analiza datos numéricos de un CSV en un proceso hijo externo", true);

	private final String descripcion;
	private final boolean requiereProcesoHijo;

	TipoOperacion(String descripcion, boolean requiereProcesoHijo) {
		this.descripcion = descripcion;
		this.requiereProcesoHijo = requiereProcesoHijo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public boolean isRequiereProcesoHijo() {
		return requiereProcesoHijo;
	}

	public static TipoOperacion desdeTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		final var normalizado = texto.trim().toUpperCase().replace(" ", "_");
		for (final var op : values()) {
			if (op.name().equals(normalizado)) {
				return op;
			}
		}
		return null;
	}
}