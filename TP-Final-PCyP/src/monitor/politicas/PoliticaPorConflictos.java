package monitor.politicas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base de las dos políticas pedidas.
 *
 * <p>Para cada conjunto de transiciones en conflicto (lo calcula la red a partir de W-, sin
 * nombres de transiciones) la política mantiene un <b>turno</b>: la única transición del
 * conjunto que tiene permiso para dispararse. Cuando esa transición se dispara, la subclase
 * elige el próximo turno.</p>
 *
 * <p>En esta red el único conjunto es {T1, T4, T6} (comparten P1, y además P7 y P8). El turno
 * decide por qué flujo sale la próxima transacción admitida. Así el reparto entre flujos lo
 * fija la política y no el orden en que llegan los hilos al monitor.</p>
 */
public abstract class PoliticaPorConflictos implements Politica {

    private final List<int[]> conflictos;
    private final int[] conjuntoDe;   // transición -> índice de su conjunto en conflicto (-1 si no tiene)
    private final int[] turno;        // conjunto -> transición con el turno (-1 = todavía sin elegir)

    protected PoliticaPorConflictos(List<int[]> conflictos, int cantidadTransiciones) {
        this.conflictos = new ArrayList<int[]>(conflictos);
        this.conjuntoDe = new int[cantidadTransiciones];
        Arrays.fill(conjuntoDe, -1);
        for (int k = 0; k < this.conflictos.size(); k++) {
            for (int t : this.conflictos.get(k)) {
                conjuntoDe[t] = k;
            }
        }
        this.turno = new int[this.conflictos.size()];
        Arrays.fill(turno, -1);
    }

    @Override
    public boolean permite(int t) {
        int k = conjuntoDe[t];
        return k < 0 || turnoDe(k) == t;
    }

    @Override
    public void notificarDisparo(int t) {
        int k = conjuntoDe[t];
        if (k >= 0 && turnoDe(k) == t) {
            turno[k] = elegirTurno(conflictos.get(k));
        }
    }

    /** Transición que tiene el turno en el conjunto k (se elige la primera vez que se pide). */
    private int turnoDe(int k) {
        if (turno[k] < 0) {
            turno[k] = elegirTurno(conflictos.get(k));
        }
        return turno[k];
    }

    /** Elige qué transición del conjunto en conflicto tiene el próximo turno. */
    protected abstract int elegirTurno(int[] conjunto);

    /** Cantidad de candidatas en {@code true}. */
    protected static int contar(boolean[] candidatas) {
        int n = 0;
        for (boolean c : candidatas) {
            if (c) {
                n++;
            }
        }
        return n;
    }
}
