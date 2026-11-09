package hilos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Fábrica de hilos (patrón Factory, como en la clase de ThreadFactory y Executors).
 *
 * <p>Crea todos los hilos de la misma manera y guarda una referencia a cada uno, para poder
 * verificar al final de la ejecución que no quedó ningún hilo vivo (requerimiento 3).</p>
 */
public class FabricaHilos implements ThreadFactory {

    private final List<Thread> creados = new ArrayList<Thread>();

    @Override
    public synchronized Thread newThread(Runnable tarea) {
        Thread hilo = new Thread(tarea, "hilo-" + (creados.size() + 1));
        hilo.setDaemon(false);
        creados.add(hilo);
        return hilo;
    }

    public synchronized int getCantidadCreados() {
        return creados.size();
    }

    /** Espera (como máximo {@code milis} en total) a que terminen todos los hilos creados. */
    public void esperarQueTerminen(long milis) throws InterruptedException {
        List<Thread> copia;
        synchronized (this) {
            copia = new ArrayList<Thread>(creados);
        }
        long limite = System.currentTimeMillis() + milis;
        for (Thread hilo : copia) {
            long resta = limite - System.currentTimeMillis();
            if (resta <= 0) {
                return;
            }
            hilo.join(resta);
        }
    }

    /** Cantidad de hilos creados por esta fábrica que siguen vivos. */
    public synchronized int contarVivos() {
        int vivos = 0;
        for (Thread hilo : creados) {
            if (hilo.isAlive()) {
                vivos++;
            }
        }
        return vivos;
    }
}
