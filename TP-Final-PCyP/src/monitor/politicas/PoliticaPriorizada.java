package monitor.politicas;

import java.util.List;

/**
 * Política priorizada: favorece el flujo de alto riesgo (el que usa los dos recursos a la vez).
 *
 * <p>El turno de cada conflicto se reparte con un round-robin ponderado suave: una transición con
 * peso w recibe w turnos de cada (suma de pesos), intercalados en forma pareja. Con los pesos de
 * la configuración (T1 = 1, T4 = 8, T6 = 1), de cada 10 transacciones 8 van a alto riesgo y 1 a
 * cada uno de los otros flujos.</p>
 *
 * <p>Con peso 0 en tarjeta y transferencia sería prioridad estricta: esos flujos no se
 * ejecutarían nunca (inanición), porque alto riesgo siempre tiene transacciones esperando.</p>
 *
 * <p>Para despertar hilos elige la candidata de mayor peso (y, a igual peso, la de menor índice).</p>
 */
public class PoliticaPriorizada extends PoliticaPorConflictos {

    private final int[] pesos;
    private final int[] acumulado;    // estado del round-robin ponderado

    public PoliticaPriorizada(List<int[]> conflictos, int cantidadTransiciones, int[] pesos) {
        super(conflictos, cantidadTransiciones);
        if (pesos.length != cantidadTransiciones) {
            throw new IllegalArgumentException("Hace falta un peso por transicion");
        }
        for (int[] conjunto : conflictos) {
            int suma = 0;
            for (int t : conjunto) {
                if (pesos[t] < 0) {
                    throw new IllegalArgumentException("Peso negativo para T" + t);
                }
                suma += pesos[t];
            }
            if (suma == 0) {
                throw new IllegalArgumentException("Un conjunto en conflicto no tiene pesos");
            }
        }
        this.pesos = pesos.clone();
        this.acumulado = new int[cantidadTransiciones];
    }

    @Override
    protected int elegirTurno(int[] conjunto) {
        int total = 0;
        int elegida = -1;
        for (int t : conjunto) {
            acumulado[t] += pesos[t];
            total += pesos[t];
            if (elegida < 0 || acumulado[t] > acumulado[elegida]) {
                elegida = t;
            }
        }
        acumulado[elegida] -= total;
        return elegida;
    }

    @Override
    public int cual(boolean[] candidatas) {
        int elegida = -1;
        for (int t = 0; t < candidatas.length; t++) {
            if (candidatas[t] && (elegida < 0 || pesos[t] > pesos[elegida])) {
                elegida = t;
            }
        }
        return elegida;
    }

    @Override
    public String getNombre() {
        return "priorizada";
    }
}
