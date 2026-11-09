package red;

import java.util.Arrays;

/**
 * Semántica temporal de la red: tiempo de sensibilización con semántica débil.
 *
 * <p>Cada transición tiene un intervalo estático [alfa, beta] en milisegundos. Si quedó
 * sensibilizada en el instante w, solo puede dispararse entre w + alfa y w + beta, y el disparo
 * en sí es instantáneo. Las transiciones inmediatas tienen alfa = 0; beta = {@link #SIN_LIMITE}
 * representa infinito.</p>
 *
 * <p>Guarda el timestamp de sensibilización de cada transición y una bandera "esperando" que
 * indica que un hilo está durmiendo hasta que se abra la ventana. Todos los métodos se usan
 * con el mutex del monitor tomado.</p>
 */
public class ControlTemporal {

    public static final long SIN_LIMITE = Long.MAX_VALUE;
    private static final long NANOS_POR_MS = 1_000_000L;

    private final long[] alfa;          // ms
    private final long[] beta;          // ms (SIN_LIMITE = infinito)
    private final long[] timestamp;     // instante de sensibilización en ns (-1 = no sensibilizada)
    private final boolean[] esperando;  // un hilo duerme esperando la ventana de esa transición

    public ControlTemporal(long[] alfaMs, long[] betaMs) {
        if (alfaMs.length != betaMs.length) {
            throw new IllegalArgumentException("alfa y beta deben tener el mismo largo");
        }
        for (int t = 0; t < alfaMs.length; t++) {
            if (alfaMs[t] < 0 || betaMs[t] < alfaMs[t]) {
                throw new IllegalArgumentException("Intervalo invalido para T" + t);
            }
        }
        this.alfa = alfaMs.clone();
        this.beta = betaMs.clone();
        this.timestamp = new long[alfaMs.length];
        this.esperando = new boolean[alfaMs.length];
        Arrays.fill(timestamp, -1L);
    }

    /** Arranca el reloj de las transiciones sensibilizadas en el marcado inicial. */
    void inicializar(boolean[] sensibilizadas, long ahoraNs) {
        for (int t = 0; t < sensibilizadas.length; t++) {
            timestamp[t] = sensibilizadas[t] ? ahoraNs : -1L;
        }
    }

    /**
     * Actualiza los timestamps después de un disparo. Las transiciones recién sensibilizadas
     * (y la disparada, si sigue sensibilizada) arrancan su reloj ahora; las que dejaron de
     * estar sensibilizadas lo pierden.
     */
    void actualizar(boolean[] antes, boolean[] despues, int disparada, long ahoraNs) {
        for (int t = 0; t < despues.length; t++) {
            if (!despues[t]) {
                timestamp[t] = -1L;
            } else if (!antes[t] || t == disparada) {
                timestamp[t] = ahoraNs;
            }
        }
    }

    /** Milisegundos que faltan para que se abra la ventana de t (0 si ya está abierta). */
    public long milisHastaVentana(int t, long ahoraNs) {
        if (alfa[t] == 0 || timestamp[t] < 0) {
            return 0;
        }
        long faltan = timestamp[t] + alfa[t] * NANOS_POR_MS - ahoraNs;
        if (faltan <= 0) {
            return 0;
        }
        return (faltan + NANOS_POR_MS - 1) / NANOS_POR_MS;     // redondeo hacia arriba
    }

    /** {@code true} si ya pasó el final de la ventana de t (transcurrido > beta). */
    public boolean ventanaVencida(int t, long ahoraNs) {
        if (beta[t] == SIN_LIMITE || timestamp[t] < 0) {
            return false;
        }
        return ahoraNs - timestamp[t] > beta[t] * NANOS_POR_MS;
    }

    public boolean hayAlguienEsperando(int t) {
        return esperando[t];
    }

    public void setEsperando(int t, boolean valor) {
        esperando[t] = valor;
    }

    public boolean esTemporal(int t) {
        return alfa[t] > 0 || beta[t] != SIN_LIMITE;
    }

    public long getAlfa(int t) {
        return alfa[t];
    }

    public long getBeta(int t) {
        return beta[t];
    }
}
