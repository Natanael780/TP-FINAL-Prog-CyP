package red;

/**
 * Segmento de un T-invariante y los hilos que lo ejecutan (algoritmo del artículo de referencia).
 *
 * <p>Un segmento es una secuencia de transiciones que un mismo hilo dispara en orden, y la
 * cantidad de hilos es el máximo de marcas simultáneas en sus plazas de acción.</p>
 */
public class Segmento {

    /** Qué hace el segmento además de disparar su secuencia. */
    public enum Rol {
        /** Solo dispara su secuencia. */
        NORMAL,
        /** Su secuencia es la entrada de transacciones: se limita a la cantidad pedida por corrida. */
        ENTRADA,
        /** Su secuencia cierra un invariante: cada ciclo completo cuenta un invariante terminado. */
        SALIDA
    }

    private final String nombre;
    private final int[] secuencia;
    private final int hilos;
    private final Rol rol;

    public Segmento(String nombre, int[] secuencia, int hilos, Rol rol) {
        if (secuencia.length == 0 || hilos < 1) {
            throw new IllegalArgumentException("Segmento invalido: " + nombre);
        }
        this.nombre = nombre;
        this.secuencia = secuencia.clone();
        this.hilos = hilos;
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public int[] getSecuencia() {
        return secuencia.clone();
    }

    public int getHilos() {
        return hilos;
    }

    public Rol getRol() {
        return rol;
    }
}
