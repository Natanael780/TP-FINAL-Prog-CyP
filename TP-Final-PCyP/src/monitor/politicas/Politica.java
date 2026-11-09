package monitor.politicas;

/**
 * Política del sistema: resuelve los conflictos de la red.
 *
 * El monitor la consulta siempre con el mutex tomado:
 * <ul>
 *   <li>{@link #permite}: si una transición en conflicto tiene permiso para dispararse ahora;</li>
 *   <li>{@link #cual}: a cuál de los hilos despertables despertar;</li>
 *   <li>{@link #notificarDisparo}: aviso de cada disparo, para actualizar su estado.</li>
 * </ul>
 */
public interface Politica {

    /** {@code true} si la transición puede dispararse ahora según la política. */
    boolean permite(int transicion);

    /**
     * Elige a cuál despertar entre las transiciones candidatas.
     *
     * @return el índice elegido, o -1 si no hay candidatas
     */
    int cual(boolean[] candidatas);

    /** Aviso de que la transición se disparó. */
    void notificarDisparo(int transicion);

    /** Nombre corto de la política (para los logs). */
    String getNombre();
}
