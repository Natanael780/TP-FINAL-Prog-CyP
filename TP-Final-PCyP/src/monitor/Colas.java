package monitor;

import java.util.concurrent.Semaphore;

/**
 * Colas de condición del monitor: una por transición (relación biunívoca transición - cola).
 *
 * Cada cola es un semáforo que arranca en 0 más un contador de hilos esperando. El contador
 * lo modifica siempre un hilo que tiene el mutex del monitor: se incrementa antes de soltar
 * el mutex ({@link #anotar}) y se decrementa al despertar ({@link #despertar}). Por eso no se
 * usa {@code Semaphore.getQueueLength()}, que es solo una estimación.
 */
public class Colas {

    private final Semaphore[] colas;
    private final int[] esperando;

    public Colas(int transiciones) {
        colas = new Semaphore[transiciones];
        esperando = new int[transiciones];
        for (int t = 0; t < transiciones; t++) {
            colas[t] = new Semaphore(0, true);   // FIFO entre los hilos de una misma transición
        }
    }

    /** Anota que un hilo va a esperar en la cola de t. Se llama con el mutex tomado. */
    public void anotar(int t) {
        esperando[t]++;
    }

    /** Bloquea al hilo en la cola de t hasta que otro hilo lo despierte. Se llama sin el mutex. */
    public void esperar(int t) throws InterruptedException {
        colas[t].acquire();
    }

    /** Despierta a un hilo de la cola de t. Se llama con el mutex tomado, que pasa al despertado. */
    public void despertar(int t) {
        if (esperando[t] <= 0) {
            throw new IllegalStateException("No hay hilos esperando en la cola de T" + t);
        }
        esperando[t]--;
        colas[t].release();
    }

    /** Vector de colas con al menos un hilo esperando. */
    public boolean[] quienesEstan() {
        boolean[] hay = new boolean[esperando.length];
        for (int t = 0; t < esperando.length; t++) {
            hay[t] = esperando[t] > 0;
        }
        return hay;
    }
}
