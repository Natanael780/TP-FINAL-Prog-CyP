import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import simulacion.Resultado;
import simulacion.Simulacion;

/**
 * Corre el plan de experimentos del informe, una corrida detrás de otra, y deja cada resultado
 * en {@code logs/resultados.csv} (además de los logs de cada corrida).
 *
 * <pre>
 *   java -cp out Experimentos            E1 a E8: 10 corridas con tiempos base y 5 por cada variación
 *   java -cp out Experimentos 3          3 corridas en E1 y E2 (y 2 en las variaciones)
 *   java -cp out Experimentos 10 E1,E2   solo los experimentos indicados
 * </pre>
 */
public class Experimentos {

    private static final String[][] PLAN = {
        // id,  política,     perfil de tiempos
        {"E1", "aleatoria", "base"},
        {"E2", "priorizada", "base"},
        {"E3", "aleatoria", "riesgo_lento"},
        {"E4", "priorizada", "riesgo_lento"},
        {"E5", "aleatoria", "tarjeta_lenta"},
        {"E6", "priorizada", "tarjeta_lenta"},
        {"E7", "aleatoria", "rapido"},
        {"E8", "priorizada", "rapido"},
    };

    public static void main(String[] args) throws Exception {
        int corridas = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        Set<String> elegidos = new HashSet<String>();
        if (args.length > 1) {
            elegidos.addAll(Arrays.asList(args[1].split(",")));
        }
        Path logs = Paths.get("logs");
        int errores = 0;
        for (String[] e : PLAN) {
            if (!elegidos.isEmpty() && !elegidos.contains(e[0])) {
                continue;
            }
            int n = "base".equals(e[2]) ? corridas : Math.max(1, corridas / 2);
            for (int i = 1; i <= n; i++) {
                String etiqueta = String.format(Locale.ROOT, "%s-%02d", e[0], i);
                Resultado r = Simulacion.ejecutar(e[1], e[2], etiqueta, logs);
                int[] c = r.getCantidadesInvariantes();
                System.out.println(String.format(Locale.ROOT, "%s %-10s %-13s %6.2f s  IT1=%3d IT2=%3d IT3=%3d  %s",
                        etiqueta, e[1], e[2], r.getDuracionSegundos(), c[0], c[1], c[2],
                        r.esCorrecto() ? "OK" : "ERROR"));
                if (!r.esCorrecto()) {
                    errores++;
                }
            }
        }
        System.out.println(errores == 0 ? "Todas las corridas fueron correctas." : errores + " corridas con errores.");
    }
}
