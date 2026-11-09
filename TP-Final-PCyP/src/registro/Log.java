package registro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Registro de la ejecución.
 *
 * <p>El monitor llama a {@link #registrar} con el mutex tomado, así que el orden del log es el
 * orden real de los disparos. Se guardan dos cosas: la secuencia de disparos en una sola línea
 * ("T0T1T0T4...", la que analiza la expresión regular) y una tabla de eventos con el instante,
 * el hilo, la transición y el marcado resultante (para el análisis de tiempos).</p>
 */
public class Log {

    private final long inicioNs;
    private final StringBuilder secuencia = new StringBuilder();
    private final StringBuilder eventos = new StringBuilder("ms;hilo;transicion;marcado\n");
    private final int[] disparosPorTransicion;

    public Log(int transiciones, long inicioNs) {
        this.inicioNs = inicioNs;
        this.disparosPorTransicion = new int[transiciones];
    }

    /** Registra un disparo. Se llama desde el monitor, con el mutex tomado. */
    public void registrar(int transicion, long ahoraNs, int[] marcado) {
        secuencia.append('T').append(transicion);
        disparosPorTransicion[transicion]++;
        eventos.append(String.format(Locale.ROOT, "%.3f", (ahoraNs - inicioNs) / 1e6))
               .append(';').append(Thread.currentThread().getName())
               .append(";T").append(transicion)
               .append(';');
        for (int p = 0; p < marcado.length; p++) {
            if (p > 0) {
                eventos.append(' ');
            }
            eventos.append(marcado[p]);
        }
        eventos.append('\n');
    }

    public String getSecuencia() {
        return secuencia.toString();
    }

    public String getEventos() {
        return eventos.toString();
    }

    public int[] getDisparosPorTransicion() {
        return disparosPorTransicion.clone();
    }

    /** Escribe un texto en un archivo (UTF-8), creando la carpeta si no existe. */
    public static void escribir(Path archivo, String contenido) throws IOException {
        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        Files.write(archivo, contenido.getBytes(StandardCharsets.UTF_8));
    }
}
