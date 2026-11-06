package src;

import java.io.File;
import java.util.Set;

public class SolicitudTrabajo {

	public static final String SEPARADOR_CSV = ";";
	public static final String SEPARADOR_MULTIRUTA = ",";

	private final String id;
	private final String operacionTexto;
	private final String rutaArchivo;
	private final String parametros;

	public SolicitudTrabajo(String id, String operacionTexto, String rutaArchivo, String parametros) {
		this.id = id != null ? id.trim() : "";
		this.operacionTexto = operacionTexto != null ? operacionTexto.trim() : "";
		this.rutaArchivo = rutaArchivo != null ? rutaArchivo.trim() : "";
		this.parametros = parametros != null ? parametros.trim() : "";
	}

	public static SolicitudTrabajo desdeLineaCsv(String lineaCruda) {
		if (lineaCruda == null || lineaCruda.isBlank()) {
			return new SolicitudTrabajo("", "", "", "");
		}
		final var partes = lineaCruda.split(SEPARADOR_CSV, -1);
		final var id = partes.length > 0 ? partes[0] : "";
		final var op = partes.length > 1 ? partes[1] : "";
		final var ruta = partes.length > 2 ? partes[2] : "";
		final var params = partes.length > 3 ? partes[3] : "";
		return new SolicitudTrabajo(id, op, ruta, params);
	}

	public String validar(Set<String> idsRegistrados) {
		if (id.isEmpty())
			return "ID vacío.";
		if (idsRegistrados.contains(id))
			return "ID duplicado.";
		final var tipoOp = TipoOperacion.desdeTexto(operacionTexto);
		if (tipoOp == null)
			return "Operación desconocida.";
		if (rutaArchivo.isEmpty())
			return "Ruta vacía.";
		if (tipoOp == TipoOperacion.BUSCAR_TEXTO && parametros.isEmpty()) {
			return "BUSCAR_TEXTO requiere parámetros.";
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public TipoOperacion getTipoOperacion() {
		return TipoOperacion.desdeTexto(operacionTexto);
	}

	public String getRutaArchivo() {
		return rutaArchivo;
	}

	public String getParametros() {
		return parametros;
	}
}