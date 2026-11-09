package red;

import java.util.Arrays;

/**
 * Datos de la red de Petri del TP (Figura 1 del enunciado): sistema de procesamiento de
 * transacciones de pago. Es la ÚNICA clase que conoce transiciones y plazas puntuales.
 * Para ejecutar otra red alcanza con cambiar esta clase: el monitor no cambia.
 *
 * <p>Plazas: P0 cola de arribo (idle) · P1 transacción admitida · P2, P3 flujo tarjeta ·
 * P4 flujo alto riesgo · P5, P6 flujo transferencia · P7 sesión del gateway (recurso) ·
 * P8 slot antifraude (recurso) · P9 buffer de salida.</p>
 */
public final class ConfiguracionRed {

    private ConfiguracionRed() {
    }

    public static final int CANTIDAD_TRANSICIONES = 10;

    /** W-: arcos plaza -> transición. Filas = plazas P0..P9, columnas = transiciones T0..T9. */
    public static final int[][] W_MENOS = {
        //T0 T1 T2 T3 T4 T5 T6 T7 T8 T9
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},   // P0
        {0, 1, 0, 0, 1, 0, 1, 0, 0, 0},   // P1
        {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},   // P2
        {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},   // P3
        {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},   // P4
        {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},   // P5
        {0, 0, 0, 0, 0, 0, 0, 0, 1, 0},   // P6
        {0, 1, 0, 0, 1, 0, 0, 0, 0, 0},   // P7
        {0, 0, 0, 0, 1, 0, 1, 0, 0, 0},   // P8
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},   // P9
    };

    /** W+: arcos transición -> plaza. Filas = plazas P0..P9, columnas = transiciones T0..T9. */
    public static final int[][] W_MAS = {
        //T0 T1 T2 T3 T4 T5 T6 T7 T8 T9
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 1},   // P0
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0},   // P1
        {0, 1, 0, 0, 0, 0, 0, 0, 0, 0},   // P2
        {0, 0, 1, 0, 0, 0, 0, 0, 0, 0},   // P3
        {0, 0, 0, 0, 1, 0, 0, 0, 0, 0},   // P4
        {0, 0, 0, 0, 0, 0, 1, 0, 0, 0},   // P5
        {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},   // P6
        {0, 0, 0, 1, 0, 1, 0, 0, 0, 0},   // P7
        {0, 0, 0, 0, 0, 1, 0, 0, 1, 0},   // P8
        {0, 0, 0, 1, 0, 1, 0, 0, 1, 0},   // P9
    };

    /** M0: 3 transacciones en la cola de arribo, gateway libre y slot antifraude libre. */
    public static final int[] MARCADO_INICIAL = {3, 0, 0, 0, 0, 0, 0, 1, 1, 0};

    /** P-invariantes (vectores de pesos). Las constantes se calculan con M0. */
    public static final int[][] P_INVARIANTES = {
        {1, 1, 1, 1, 1, 1, 1, 0, 0, 1},   // PI1: transacciones en el sistema (= 3)
        {0, 0, 1, 1, 1, 0, 0, 1, 0, 0},   // PI2: sesión del gateway (= 1)
        {0, 0, 0, 0, 1, 1, 1, 0, 1, 0},   // PI3: slot antifraude (= 1)
    };

    /** T-invariantes, con sus transiciones en orden de disparo. */
    public static final int[][] T_INVARIANTES = {
        {0, 1, 2, 3, 9},                  // IT1: pago con tarjeta
        {0, 4, 5, 9},                     // IT2: pago de alto riesgo
        {0, 6, 7, 8, 9},                  // IT3: transferencia bancaria
    };

    public static final String[] NOMBRES_T_INVARIANTES = {"IT1 tarjeta", "IT2 alto riesgo", "IT3 transferencia"};

    /**
     * Expresión regular única con los tres T-invariantes (comparten T0 y T9). Los grupos (.*?)
     * guardan lo que se intercala; los grupos con nombre marcan qué invariante coincidió.
     */
    public static final String REGEX_T_INVARIANTES =
            "T0(.*?)(?:T1(?<it1>.*?)T2(.*?)T3(.*?)|T4(?<it2>.*?)T5(.*?)|T6(?<it3>.*?)T7(.*?)T8(.*?))T9";

    public static final String[] GRUPOS_T_INVARIANTES = {"it1", "it2", "it3"};

    /**
     * Política priorizada: peso de cada transición en el conflicto de ruteo {T1, T4, T6}.
     * Alto riesgo (T4) recibe 8 de cada 10 turnos; tarjeta (T1) y transferencia (T6), 1 cada una.
     */
    public static final int[] PESOS_PRIORIZADA = {0, 1, 0, 0, 8, 0, 1, 0, 0, 0};

    /**
     * Segmentos de hilos (sección "Implementación" del enunciado y artículo de referencia):
     * 3 + 1 + 1 + 1 + 3 = 9 hilos.
     */
    public static Segmento[] segmentos() {
        return new Segmento[] {
            new Segmento("SA", new int[] {0}, 3, Segmento.Rol.ENTRADA),     // antes del conflicto en P1
            new Segmento("SB", new int[] {1, 2, 3}, 1, Segmento.Rol.NORMAL), // tarjeta
            new Segmento("SC", new int[] {4, 5}, 1, Segmento.Rol.NORMAL),    // alto riesgo
            new Segmento("SD", new int[] {6, 7, 8}, 1, Segmento.Rol.NORMAL), // transferencia
            new Segmento("SE", new int[] {9}, 3, Segmento.Rol.SALIDA),      // después del join en P9
        };
    }

    /**
     * Perfiles de tiempos (alfa en ms de T2, T3, T5, T7, T8). "base" es el de entrega;
     * "sin_tiempo" ejecuta el monitor sin semántica temporal (primera etapa del TP).
     */
    public static final String[] PERFILES = {"base", "riesgo_lento", "tarjeta_lenta", "rapido", "sin_tiempo"};

    /**
     * Alfa (ms) de cada transición según el perfil. Las inmediatas (T0, T1, T4, T6, T9) valen 0.
     * base: T2 = 120 (respuesta del issuer), T3 = 80 (captura), T5 = 140 (scoring),
     * T7 = 70 (validación de cuenta), T8 = 130 (ejecución de la transferencia).
     */
    public static long[] alfaDelPerfil(String perfil) {
        long t2 = 120, t3 = 80, t5 = 140, t7 = 70, t8 = 130;
        if ("riesgo_lento".equals(perfil)) {
            t5 = 280;
        } else if ("tarjeta_lenta".equals(perfil)) {
            t2 = 240;
            t3 = 160;
        } else if ("sin_tiempo".equals(perfil)) {
            t2 = 0;
            t3 = 0;
            t5 = 0;
            t7 = 0;
            t8 = 0;
        } else if ("rapido".equals(perfil)) {
            t2 = 60;
            t3 = 40;
            t5 = 70;
            t7 = 35;
            t8 = 65;
        } else if (!"base".equals(perfil)) {
            throw new IllegalArgumentException("Perfil de tiempos desconocido: " + perfil
                    + ". Opciones: " + Arrays.toString(PERFILES));
        }
        //                T0 T1  T2  T3 T4  T5 T6  T7  T8 T9
        return new long[] {0, 0, t2, t3, 0, t5, 0, t7, t8, 0};
    }

    /** Beta (ms): sin límite para todas las transiciones. */
    public static long[] beta() {
        long[] beta = new long[CANTIDAD_TRANSICIONES];
        Arrays.fill(beta, ControlTemporal.SIN_LIMITE);
        return beta;
    }
}
