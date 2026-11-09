import java.util.*; import java.util.concurrent.*; import java.util.concurrent.atomic.*;
import hilos.*; import monitor.*; import monitor.politicas.*; import red.*; import registro.*;
/**
 * Versión de comparación (no es parte de la entrega principal): la política SOLO elige a quién
 * despertar, como en el diagrama básico del monitor, y no resuelve el conflicto de ruteo.
 * Sirve para mostrar por qué nuestras políticas deciden el turno del conflicto (informe, sección 8.2).
 *
 * Uso (desde la carpeta del proyecto, después de compilar en out/):
 *   javac -encoding UTF-8 -cp out -d out herramientas/comparacion/SoloDespertar.java
 *   java -cp out SoloDespertar aleatoria 3
 *   java -cp out SoloDespertar priorizada 3
 */
public class SoloDespertar {
    public static void main(String[] a) throws Exception {
        final boolean priorizar = a[0].equals("priorizada"); int corridas = Integer.parseInt(a[1]);
        for (int c = 0; c < corridas; c++) {
            ControlTemporal temporal = new ControlTemporal(ConfiguracionRed.alfaDelPerfil("base"), ConfiguracionRed.beta());
            RedDePetri red = new RedDePetri(ConfiguracionRed.W_MENOS, ConfiguracionRed.W_MAS, ConfiguracionRed.MARCADO_INICIAL, ConfiguracionRed.P_INVARIANTES, temporal);
            final Random azar = new Random();
            Politica p = new Politica() {
                public boolean permite(int t) { return true; }
                public int cual(boolean[] m) {
                    if (priorizar && m[4]) return 4;
                    List<Integer> l = new ArrayList<>(); for (int t = 0; t < m.length; t++) if (m[t]) l.add(t);
                    return l.isEmpty() ? -1 : l.get(azar.nextInt(l.size()));
                }
                public void notificarDisparo(int t) {}
                public String getNombre() { return "solo-despertar"; }
            };
            FabricaHilos fab = new FabricaHilos(); ExecutorService ej = Executors.newFixedThreadPool(9, fab);
            AtomicInteger cupo = new AtomicInteger(200); CountDownLatch fin = new CountDownLatch(200);
            long ini = System.nanoTime(); Log log = new Log(10, ini); Monitor mon = new Monitor(red, p, log);
            for (Segmento s : ConfiguracionRed.segmentos()) for (int i = 1; i <= s.getHilos(); i++)
                ej.execute(new Disparador(s.getNombre()+"-"+i, mon, s.getSecuencia(), s.getRol()==Segmento.Rol.ENTRADA?cupo:null, s.getRol()==Segmento.Rol.SALIDA?fin:null));
            fin.await(); double dur = (System.nanoTime()-ini)/1e9; ej.shutdownNow(); ej.awaitTermination(5, TimeUnit.SECONDS);
            int[] d = log.getDisparosPorTransicion();
            System.out.printf(Locale.ROOT, "%s corrida %d: %.2f s  tarjeta=%d alto_riesgo=%d transferencia=%d%n", a[0], c+1, dur, d[3], d[5], d[8]);
        }
    }
}
