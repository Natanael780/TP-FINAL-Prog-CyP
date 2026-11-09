package monitor.politicas;

import java.util.List;
import java.util.Random;

/**
 * Política aleatoria: cada transacción se procesa por cualquiera de los tres flujos, al azar.
 *
 * <p>El turno de cada conflicto se sortea con probabilidad uniforme entre las transiciones del
 * conjunto (1/3 para T1, T4 y T6). Para despertar hilos también elige al azar entre los
 * candidatos.</p>
 */
public class PoliticaAleatoria extends PoliticaPorConflictos {

    private final Random azar;

    public PoliticaAleatoria(List<int[]> conflictos, int cantidadTransiciones, Random azar) {
        super(conflictos, cantidadTransiciones);
        this.azar = azar;
    }

    @Override
    protected int elegirTurno(int[] conjunto) {
        return conjunto[azar.nextInt(conjunto.length)];
    }

    @Override
    public int cual(boolean[] candidatas) {
        int n = contar(candidatas);
        if (n == 0) {
            return -1;
        }
        int elegida = azar.nextInt(n);
        for (int t = 0; t < candidatas.length; t++) {
            if (candidatas[t]) {
                if (elegida == 0) {
                    return t;
                }
                elegida--;
            }
        }
        return -1;
    }

    @Override
    public String getNombre() {
        return "aleatoria";
    }
}
