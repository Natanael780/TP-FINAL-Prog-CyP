package hilos;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import monitor.MonitorInterface;

/**
 * Hilo de un segmento: repite en orden la secuencia de transiciones de su segmento.
 *
 * <p>Después de cada disparo ejecuta la acción asociada fuera del monitor. La duración de cada
 * etapa está modelada por las transiciones temporales (el monitor hace esperar al hilo), así que
 * la acción solo deja constancia del trabajo hecho.</p>
 *
 * <p>Termina cuando {@code fireTransition} devuelve {@code false} (fin de la ejecución) o, si
 * tiene cupo, cuando el cupo compartido de su segmento se agota.</p>
 */
public class Disparador implements Runnable {

    private final String nombre;
    private final MonitorInterface monitor;
    private final int[] secuencia;
    private final AtomicInteger cupo;              // ciclos que le quedan al segmento (null = sin límite)
    private final CountDownLatch ciclosCompletos;  // se descuenta en cada ciclo completo (null = no cuenta)
    private volatile long disparos = 0;
    private volatile long ciclos = 0;

    public Disparador(String nombre, MonitorInterface monitor, int[] secuencia,
                      AtomicInteger cupo, CountDownLatch ciclosCompletos) {
        this.nombre = nombre;
        this.monitor = monitor;
        this.secuencia = secuencia.clone();
        this.cupo = cupo;
        this.ciclosCompletos = ciclosCompletos;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(nombre);
        while (!Thread.currentThread().isInterrupted()) {
            if (cupo != null && cupo.getAndDecrement() <= 0) {
                return;                                    // el segmento ya reservó todos sus ciclos
            }
            for (int t : secuencia) {
                if (!monitor.fireTransition(t)) {
                    return;                                // fin de la ejecución
                }
                accion(t);
            }
            ciclos++;
            if (ciclosCompletos != null) {
                ciclosCompletos.countDown();
            }
        }
    }

    /** Acción de la etapa a la que llegó la transacción, fuera del monitor. */
    private void accion(int transicion) {
        disparos++;
    }

    public String getNombre() {
        return nombre;
    }

    public long getDisparos() {
        return disparos;
    }

    public long getCiclos() {
        return ciclos;
    }
}
