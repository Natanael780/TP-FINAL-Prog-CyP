package simulacion;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Resultado de una corrida: lo que se escribe en el log de resumen y en la tabla de resultados. */
public class Resultado {

    String politica;
    String perfil;
    String etiqueta;
    long[] alfa;
    double duracionSegundos;
    boolean completo;
    int hilosCreados;
    int hilosVivos;
    int[] disparosPorTransicion;
    String[] nombresInvariantes;
    int[] cantidadesInvariantes;
    String residuo;
    int pasadasRegex;
    long verificacionesPInv;
    int[] marcadoFinal;
    boolean volvioAMarcadoInicial;
    final Map<String, Long> disparosPorHilo = new LinkedHashMap<String, Long>();

    /** Cumple todo lo pedido: 200 invariantes, residuo vacío, M0 al final y sin hilos vivos. */
    public boolean esCorrecto() {
        return completo && hilosVivos == 0 && residuo.isEmpty()
                && getTotalInvariantes() == Simulacion.INVARIANTES_POR_CORRIDA
                && volvioAMarcadoInicial;
    }

    public int getTotalInvariantes() {
        int total = 0;
        for (int c : cantidadesInvariantes) {
            total += c;
        }
        return total;
    }

    public double getDuracionSegundos() {
        return duracionSegundos;
    }

    public int[] getCantidadesInvariantes() {
        return cantidadesInvariantes.clone();
    }

    public String getPolitica() {
        return politica;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static String encabezadoCsv() {
        return "etiqueta;politica;perfil;duracion_s;IT1_tarjeta;IT2_alto_riesgo;IT3_transferencia;total;"
                + "residuo_vacio;verificaciones_pinv;volvio_a_M0;hilos_creados;hilos_vivos;correcto";
    }

    public String filaCsv() {
        return String.format(Locale.ROOT, "%s;%s;%s;%.3f;%d;%d;%d;%d;%s;%d;%s;%d;%d;%s",
                etiqueta, politica, perfil, duracionSegundos,
                cantidadesInvariantes[0], cantidadesInvariantes[1], cantidadesInvariantes[2],
                getTotalInvariantes(), residuo.isEmpty(), verificacionesPInv, volvioAMarcadoInicial,
                hilosCreados, hilosVivos, esCorrecto());
    }

    /** Resumen legible de la corrida. */
    public String resumen() {
        StringBuilder s = new StringBuilder();
        s.append("=== TP Final PCyP 2026 - resumen de la corrida ===\n");
        s.append("Etiqueta: ").append(etiqueta).append('\n');
        s.append("Politica: ").append(politica).append('\n');
        s.append("Perfil de tiempos: ").append(perfil)
         .append("  alfa(ms) T2=").append(alfa[2]).append(" T3=").append(alfa[3])
         .append(" T5=").append(alfa[5]).append(" T7=").append(alfa[7]).append(" T8=").append(alfa[8])
         .append("  beta = infinito\n");
        s.append(String.format(Locale.ROOT, "Duracion hasta el invariante %d: %.3f s%n",
                Simulacion.INVARIANTES_POR_CORRIDA, duracionSegundos));
        s.append("Se completaron los ").append(Simulacion.INVARIANTES_POR_CORRIDA).append(" invariantes: ")
         .append(completo ? "si" : "NO").append('\n');
        s.append('\n').append("Invariantes de transicion (expresion regular sobre el log):\n");
        int total = getTotalInvariantes();
        for (int i = 0; i < nombresInvariantes.length; i++) {
            double porcentaje = total == 0 ? 0 : 100.0 * cantidadesInvariantes[i] / total;
            s.append(String.format(Locale.ROOT, "  %-18s %4d  (%5.1f %%)%n",
                    nombresInvariantes[i], cantidadesInvariantes[i], porcentaje));
        }
        s.append(String.format(Locale.ROOT, "  %-18s %4d%n", "Total", total));
        s.append("  Residuo de la regex: ").append(residuo.isEmpty() ? "vacio" : "\"" + residuo + "\"")
         .append(" (").append(pasadasRegex).append(" pasadas)\n");
        s.append('\n').append("Disparos por transicion:\n  ");
        for (int t = 0; t < disparosPorTransicion.length; t++) {
            s.append('T').append(t).append('=').append(disparosPorTransicion[t]).append(t + 1 < disparosPorTransicion.length ? "  " : "\n");
        }
        s.append("Disparos por hilo:\n");
        for (Map.Entry<String, Long> e : disparosPorHilo.entrySet()) {
            s.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        s.append('\n');
        s.append("Verificaciones de P-invariantes (una por disparo): ").append(verificacionesPInv).append(", todas OK\n");
        s.append("Marcado final: ").append(Arrays.toString(marcadoFinal))
         .append(volvioAMarcadoInicial ? " = M0" : " (distinto de M0)").append('\n');
        s.append("Hilos creados: ").append(hilosCreados).append(" - hilos activos al terminar: ").append(hilosVivos).append('\n');
        s.append("Resultado: ").append(esCorrecto() ? "CORRECTO" : "CON ERRORES").append('\n');
        return s.toString();
    }
}
