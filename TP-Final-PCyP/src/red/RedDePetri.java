package red;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lógica de la red de Petri: marcado, sensibilizado y disparo con la ecuación de estado.
 *
 * <p>Se construye con matrices genéricas (W-, W+, marcado inicial y P-invariantes), así que sirve
 * para cualquier red ordinaria o generalizada. No es thread-safe por sí misma: el monitor la usa
 * siempre con el mutex tomado.</p>
 */
public class RedDePetri {

    private final int plazas;
    private final int transiciones;
    private final int[][] wMenos;          // W-: peso de los arcos plaza -> transición
    private final int[][] incidencia;      // I = W+ - W-
    private final int[] marcadoInicial;
    private final int[] marcado;
    private final boolean[] sensibilizadas;
    private final int[][] pInvariantes;
    private final int[] constantesPInv;    // x . M0 de cada P-invariante
    private final ControlTemporal temporal;
    private long disparos = 0;
    private long verificacionesPInv = 0;

    public RedDePetri(int[][] wMenos, int[][] wMas, int[] marcadoInicial,
                      int[][] pInvariantes, ControlTemporal temporal) {
        this.plazas = wMenos.length;
        this.transiciones = wMenos[0].length;
        if (wMas.length != plazas || marcadoInicial.length != plazas) {
            throw new IllegalArgumentException("Dimensiones de la red inconsistentes");
        }
        this.wMenos = copiar(wMenos);
        this.incidencia = new int[plazas][transiciones];
        for (int p = 0; p < plazas; p++) {
            for (int t = 0; t < transiciones; t++) {
                incidencia[p][t] = wMas[p][t] - wMenos[p][t];
            }
        }
        this.marcadoInicial = marcadoInicial.clone();
        this.marcado = marcadoInicial.clone();
        this.pInvariantes = copiar(pInvariantes);
        this.constantesPInv = new int[pInvariantes.length];
        for (int i = 0; i < pInvariantes.length; i++) {
            if (!esPInvariante(pInvariantes[i])) {
                throw new IllegalArgumentException("El vector " + Arrays.toString(pInvariantes[i])
                        + " no es un P-invariante de la red (x^T . I != 0)");
            }
            constantesPInv[i] = producto(pInvariantes[i], marcadoInicial);
        }
        this.temporal = temporal;
        this.sensibilizadas = new boolean[transiciones];
        recalcularSensibilizadas();
        temporal.inicializar(sensibilizadas, System.nanoTime());
    }

    /** Ecuación de estado: M' = M + I . sigma, con sigma el vector de disparo de t. */
    private int[] ecuacionDeEstado(int t) {
        int[] sigma = new int[transiciones];
        sigma[t] = 1;
        int[] siguiente = new int[plazas];
        for (int p = 0; p < plazas; p++) {
            int suma = 0;
            for (int j = 0; j < transiciones; j++) {
                suma += incidencia[p][j] * sigma[j];
            }
            siguiente[p] = marcado[p] + suma;
        }
        return siguiente;
    }

    /** t está sensibilizada si M >= W- . sigma (alcanzan las marcas de todos sus arcos de entrada). */
    private boolean calcularSensibilizada(int t) {
        for (int p = 0; p < plazas; p++) {
            if (marcado[p] < wMenos[p][t]) {
                return false;
            }
        }
        return true;
    }

    private void recalcularSensibilizadas() {
        for (int t = 0; t < transiciones; t++) {
            sensibilizadas[t] = calcularSensibilizada(t);
        }
    }

    /**
     * Dispara t, que debe estar sensibilizada: nuevo marcado con la ecuación de estado,
     * nuevo vector de sensibilizadas y actualización de los timestamps.
     */
    public void disparar(int t, long ahoraNs) {
        if (!sensibilizadas[t]) {
            throw new IllegalStateException("T" + t + " no esta sensibilizada");
        }
        int[] nuevo = ecuacionDeEstado(t);
        for (int p = 0; p < plazas; p++) {
            if (nuevo[p] < 0) {
                throw new IllegalStateException("Marcado negativo en P" + p + " al disparar T" + t);
            }
        }
        boolean[] antes = sensibilizadas.clone();
        System.arraycopy(nuevo, 0, marcado, 0, plazas);
        recalcularSensibilizadas();
        temporal.actualizar(antes, sensibilizadas, t, ahoraNs);
        disparos++;
    }

    /** Requerimiento 12: verifica x . M = constante para cada P-invariante. */
    public void verificarInvariantesPlaza() {
        verificacionesPInv++;
        for (int i = 0; i < pInvariantes.length; i++) {
            int valor = producto(pInvariantes[i], marcado);
            if (valor != constantesPInv[i]) {
                throw new IllegalStateException("No se cumple el P-invariante " + (i + 1) + ": "
                        + valor + " != " + constantesPInv[i] + " con M = " + Arrays.toString(marcado));
            }
        }
    }

    /**
     * Conjuntos de transiciones en conflicto estructural: las que comparten alguna plaza de
     * entrada, agrupadas en forma transitiva. Sale de W-, sin conocer la red en particular.
     */
    public List<int[]> conflictosEstructurales() {
        int[] grupo = new int[transiciones];
        for (int t = 0; t < transiciones; t++) {
            grupo[t] = t;
        }
        for (int p = 0; p < plazas; p++) {
            int primera = -1;
            for (int t = 0; t < transiciones; t++) {
                if (wMenos[p][t] > 0) {
                    if (primera < 0) {
                        primera = t;
                    } else {
                        unir(grupo, primera, t);
                    }
                }
            }
        }
        List<int[]> conjuntos = new ArrayList<int[]>();
        for (int raiz = 0; raiz < transiciones; raiz++) {
            List<Integer> miembros = new ArrayList<Integer>();
            for (int t = 0; t < transiciones; t++) {
                if (buscar(grupo, t) == raiz) {
                    miembros.add(t);
                }
            }
            if (miembros.size() > 1) {
                int[] conjunto = new int[miembros.size()];
                for (int i = 0; i < conjunto.length; i++) {
                    conjunto[i] = miembros.get(i);
                }
                conjuntos.add(conjunto);
            }
        }
        return conjuntos;
    }

    private static int buscar(int[] grupo, int t) {
        while (grupo[t] != t) {
            t = grupo[t];
        }
        return t;
    }

    private static void unir(int[] grupo, int a, int b) {
        int ra = buscar(grupo, a);
        int rb = buscar(grupo, b);
        if (ra != rb) {
            grupo[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }

    private boolean esPInvariante(int[] x) {
        if (x.length != plazas) {
            return false;
        }
        for (int t = 0; t < transiciones; t++) {
            int suma = 0;
            for (int p = 0; p < plazas; p++) {
                suma += x[p] * incidencia[p][t];
            }
            if (suma != 0) {
                return false;
            }
        }
        return true;
    }

    private static int producto(int[] a, int[] b) {
        int suma = 0;
        for (int i = 0; i < a.length; i++) {
            suma += a[i] * b[i];
        }
        return suma;
    }

    private static int[][] copiar(int[][] m) {
        int[][] copia = new int[m.length][];
        for (int i = 0; i < m.length; i++) {
            copia[i] = m[i].clone();
        }
        return copia;
    }

    public boolean estaSensibilizada(int t) {
        return sensibilizadas[t];
    }

    public boolean[] getSensibilizadas() {
        return sensibilizadas.clone();
    }

    public int[] getMarcado() {
        return marcado.clone();
    }

    public boolean volvioAlMarcadoInicial() {
        return Arrays.equals(marcado, marcadoInicial);
    }

    public int getCantidadPlazas() {
        return plazas;
    }

    public int getCantidadTransiciones() {
        return transiciones;
    }

    public ControlTemporal getControlTemporal() {
        return temporal;
    }

    public long getDisparos() {
        return disparos;
    }

    public long getVerificacionesPInv() {
        return verificacionesPInv;
    }

    public int[] getConstantesPInv() {
        return constantesPInv.clone();
    }
}
