package monitor;

import java.util.concurrent.Semaphore;

import monitor.politicas.Politica;
import red.ControlTemporal;
import red.RedDePetri;
import registro.Log;

/**
 * Monitor de concurrencia que guía la ejecución de la red de Petri.
 *
 * <p>Es agnóstico a la red: no tiene ninguna referencia a transiciones puntuales. Lo que sabe
 * de la red se lo pregunta a {@link RedDePetri} (sensibilizado, disparo, invariantes) y a
 * {@link ControlTemporal} (ventanas de tiempo). Los conflictos los resuelve la {@link Politica}.
 * Si se cambia la red, esta clase no cambia.</p>
 *
 * <p>Algoritmo de {@link #fireTransition} (diagramas de secuencia de la cátedra, con y sin tiempo):</p>
 * <ol>
 *   <li>El hilo toma el mutex.</li>
 *   <li>Si su transición no puede intentarse (no está sensibilizada, la política no la permite
 *       u otro hilo ya espera su ventana), libera el mutex y se bloquea en la cola de esa transición.</li>
 *   <li>Si está sensibilizada pero su ventana temporal todavía no se abrió, libera el mutex,
 *       duerme fuera del monitor lo que falta y vuelve a intentar.</li>
 *   <li>Si puede, dispara: actualiza el marcado, verifica los P-invariantes y registra el disparo.</li>
 *   <li>Calcula los hilos despertables (sensibilizadas, permitidas y con hilos esperando). Si hay,
 *       la política elige uno y el mutex pasa a ese hilo sin liberarse; si no hay, libera el mutex.</li>
 * </ol>
 *
 * <p>El único método público es {@code fireTransition} (requisito del enunciado).</p>
 */
public class Monitor implements MonitorInterface {

    private final Semaphore mutex = new Semaphore(1, true);
    private final RedDePetri red;
    private final ControlTemporal tiempo;
    private final Colas colas;
    private final Politica politica;
    private final Log log;
    private final int transiciones;

    public Monitor(RedDePetri red, Politica politica, Log log) {
        this.red = red;
        this.tiempo = red.getControlTemporal();
        this.politica = politica;
        this.log = log;
        this.transiciones = red.getCantidadTransiciones();
        this.colas = new Colas(transiciones);
    }

    @Override
    public boolean fireTransition(int transition) {
        if (transition < 0 || transition >= transiciones) {
            throw new IllegalArgumentException("No existe la transicion " + transition);
        }
        if (!tomarMutex()) {
            return false;                                   // interrumpido antes de entrar
        }
        while (true) {
            // El hilo tiene el mutex en cada vuelta del ciclo.
            if (!puedeIntentar(transition)) {
                if (!esperarEnCola(transition)) {
                    return false;                           // interrumpido mientras esperaba
                }
                continue;                                   // lo despertaron: ya tiene el mutex
            }
            long ahora = System.nanoTime();
            long esperaMs = tiempo.milisHastaVentana(transition, ahora);
            if (esperaMs > 0) {                             // antes de la ventana
                if (!dormirFueraDelMonitor(transition, esperaMs)) {
                    return false;                           // interrumpido mientras dormía
                }
                continue;                                   // volvió a tomar el mutex: reintenta
            }
            if (tiempo.ventanaVencida(transition, ahora)) { // después de la ventana
                mutex.release();
                return false;
            }
            disparar(transition, ahora);
            despertarOSalir();
            return true;
        }
    }

    /** La transición está sensibilizada, la política la permite y nadie espera ya su ventana. */
    private boolean puedeIntentar(int t) {
        return red.estaSensibilizada(t)
                && politica.permite(t)
                && !tiempo.hayAlguienEsperando(t);
    }

    /** Disparo con el mutex tomado: ecuación de estado, verificación y registro. */
    private void disparar(int t, long ahora) {
        red.disparar(t, ahora);
        red.verificarInvariantesPlaza();                    // requerimiento 12
        politica.notificarDisparo(t);
        log.registrar(t, ahora, red.getMarcado());
    }

    /**
     * Despertables = sensibilizadas AND esperando AND permitidas por la política.
     * Si hay alguno, la política elige a cuál despertar y el mutex pasa a ese hilo;
     * si no hay ninguno, se libera el mutex.
     */
    private void despertarOSalir() {
        boolean[] sensibilizadas = red.getSensibilizadas();
        boolean[] esperando = colas.quienesEstan();
        boolean[] despertables = new boolean[transiciones];
        for (int t = 0; t < transiciones; t++) {
            despertables[t] = sensibilizadas[t]
                    && esperando[t]
                    && politica.permite(t)
                    && !tiempo.hayAlguienEsperando(t);
        }
        int elegida = politica.cual(despertables);
        if (elegida >= 0) {
            if (!despertables[elegida]) {
                throw new IllegalStateException("La politica eligio T" + elegida + ", que no es despertable");
            }
            colas.despertar(elegida);                       // el mutex pasa al despertado
        } else {
            mutex.release();
        }
    }

    /** Libera el mutex y bloquea al hilo en la cola de t. Al volver, el hilo tiene el mutex. */
    private boolean esperarEnCola(int t) {
        colas.anotar(t);                                    // todavía con el mutex tomado
        mutex.release();
        try {
            colas.esperar(t);
            return true;                                    // quien lo despertó le pasó el mutex
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Antes de la ventana: duerme sin el mutex lo que falta y lo vuelve a tomar. */
    private boolean dormirFueraDelMonitor(int t, long esperaMs) {
        tiempo.setEsperando(t, true);
        mutex.release();
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (!tomarMutex()) {
            return false;
        }
        tiempo.setEsperando(t, false);
        return true;
    }

    private boolean tomarMutex() {
        try {
            mutex.acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
