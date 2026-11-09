package registro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requerimiento 13: verifica los T-invariantes analizando con una expresión regular la
 * secuencia de transiciones disparadas.
 *
 * <p>Se usa UNA sola expresión con todos los caminos, porque los invariantes comparten
 * transiciones (T0 y T9). Cada coincidencia se reemplaza por la concatenación de sus grupos
 * (.*?): se borran las transiciones del invariante encontrado y se conserva lo que estaba
 * intercalado (disparos de otras transacciones). Se repite hasta que no haya más coincidencias.
 * Si todos los disparos forman invariantes completos, el residuo queda vacío.</p>
 */
public class AnalizadorInvariantes {

    private final Pattern patron;
    private final String[] grupos;    // grupo con nombre que identifica a cada invariante
    private final String[] nombres;   // nombre de cada invariante para mostrar

    public AnalizadorInvariantes(String regex, String[] grupos, String[] nombres) {
        if (grupos.length != nombres.length) {
            throw new IllegalArgumentException("Hace falta un nombre por grupo");
        }
        this.patron = Pattern.compile(regex);
        this.grupos = grupos.clone();
        this.nombres = nombres.clone();
    }

    public Resultado analizar(String secuencia) {
        int[] cuenta = new int[grupos.length];
        String resto = secuencia;
        int pasadas = 0;
        while (true) {
            Matcher m = patron.matcher(resto);
            StringBuffer reducido = new StringBuffer();
            int encontrados = 0;
            while (m.find()) {
                encontrados++;
                cuenta[identificar(m)]++;
                StringBuilder intercalado = new StringBuilder();
                for (int g = 1; g <= m.groupCount(); g++) {
                    if (m.group(g) != null) {
                        intercalado.append(m.group(g));
                    }
                }
                m.appendReplacement(reducido, Matcher.quoteReplacement(intercalado.toString()));
            }
            m.appendTail(reducido);
            if (encontrados == 0) {
                break;
            }
            resto = reducido.toString();
            pasadas++;
        }
        return new Resultado(nombres, cuenta, resto, pasadas);
    }

    private int identificar(Matcher m) {
        for (int i = 0; i < grupos.length; i++) {
            if (m.group(grupos[i]) != null) {
                return i;
            }
        }
        throw new IllegalStateException("Coincidencia sin invariante identificado: " + m.group());
    }

    /** Resultado del análisis: cantidad por invariante y residuo. */
    public static class Resultado {
        private final String[] nombres;
        private final int[] cantidades;
        private final String residuo;
        private final int pasadas;

        Resultado(String[] nombres, int[] cantidades, String residuo, int pasadas) {
            this.nombres = nombres.clone();
            this.cantidades = cantidades.clone();
            this.residuo = residuo;
            this.pasadas = pasadas;
        }

        public String[] getNombres() {
            return nombres.clone();
        }

        public int[] getCantidades() {
            return cantidades.clone();
        }

        public int getTotal() {
            int total = 0;
            for (int c : cantidades) {
                total += c;
            }
            return total;
        }

        public String getResiduo() {
            return residuo;
        }

        public int getPasadas() {
            return pasadas;
        }

        /** {@code true} si todos los disparos formaron invariantes completos. */
        public boolean residuoVacio() {
            return residuo.isEmpty();
        }
    }
}
