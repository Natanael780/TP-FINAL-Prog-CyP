package monitor;

/**
 * Interfaz pedida por el enunciado. La implementa la clase {@link Monitor}.
 */
public interface MonitorInterface {

    /**
     * Intenta disparar una transición de la red. Si la transición no se puede disparar
     * todavía, el hilo que llama queda bloqueado hasta que pueda hacerlo.
     *
     * @param transition índice de la transición (0 = T0, 1 = T1, ...)
     * @return {@code true} si la transición se disparó; {@code false} si la ejecución
     *         terminó (el hilo fue interrumpido) o si se venció su ventana temporal
     */
    boolean fireTransition(int transition);
}
