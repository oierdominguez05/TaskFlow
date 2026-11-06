package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class EstadisticasCsvHijo {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Error: Falta la ruta del archivo CSV.");
			System.exit(1);
		}

		final var rutaArchivo = args[0];
		long cantidadValidos = 0;
		long descartados = 0;
		double suma = 0.0;
		double minimo = Double.MAX_VALUE;
		double maximo = -Double.MAX_VALUE;

		try (final var br = new BufferedReader(new FileReader(rutaArchivo))) {
			String linea;
			while ((linea = br.readLine()) != null) {
				linea = linea.trim();
				if (linea.isEmpty() || linea.startsWith("#")) {
					continue;
				}

				final var tokens = linea.split("[,;]");
				for (final var token : tokens) {
					final var limpio = token.trim();
					if (limpio.isEmpty())
						continue;
					try {

						final var valor = Double.parseDouble(limpio);
						suma += valor;
						cantidadValidos++;
						if (valor < minimo)
							minimo = valor;
						if (valor > maximo)
							maximo = valor;
					} catch (NumberFormatException e) {
						descartados++;
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error al leer el archivo CSV: " + e.getMessage());
			System.exit(2);
		}

		if (cantidadValidos == 0) {
			System.out.println("Validos=0; Descartados=" + descartados + "; Sin datos numericos");
			System.exit(0);
		}

		final var media = suma / cantidadValidos;
		System.out.printf(Locale.US, "Validos=%d; Descartados=%d; Suma=%.2f; Media=%.2f; Min=%.2f; Max=%.2f%n",
				cantidadValidos, descartados, suma, media, minimo, maximo);
		System.exit(0);
	}
}