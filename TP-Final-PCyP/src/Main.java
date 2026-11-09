import java.nio.file.Paths;
import java.util.Locale;

import simulacion.Resultado;
import simulacion.Simulacion;

/**
 * Punto de entrada del TP Final de Programación Concurrente y Paralela 2026.
 *
 * <p>Ejecuta UNA corrida de la red de Petri del sistema de procesamiento de pagos hasta completar
 * 200 invariantes, con la política indicada (cada política se ejecuta y analiza por separado).
 * Deja los logs en la carpeta {@code logs/}.</p>
 *
 * <pre>
 *   java -cp out Main                         política aleatoria, perfil de tiempos base
 *   java -cp out Main priorizada              política priorizada
 *   java -cp out Main aleatoria 02 base       política, etiqueta de la corrida y perfil de tiempos
 * </pre>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        long inicioPrograma = System.nanoTime();
        String politica = args.length > 0 ? args[0] : "aleatoria";
        String etiqueta = args.length > 1 ? args[1] : "01";
        String perfil = args.length > 2 ? args[2] : "base";

        System.out.println("TP Final PCyP 2026 - politica " + politica + ", perfil de tiempos " + perfil
                + ". Ejecutando hasta completar " + Simulacion.INVARIANTES_POR_CORRIDA + " invariantes...");
        Resultado resultado = Simulacion.ejecutar(politica, perfil, etiqueta, Paths.get("logs"));

        System.out.println();
        System.out.print(resultado.resumen());
        System.out.println(String.format(Locale.ROOT, "Tiempo total del programa: %.3f s",
                (System.nanoTime() - inicioPrograma) / 1e9));
        System.out.println("Logs en la carpeta logs/ (" + resultado.getPolitica() + "_" + perfil + "_" + etiqueta + "_*)");
        if (!resultado.esCorrecto()) {
            System.exit(1);
        }
    }
}
