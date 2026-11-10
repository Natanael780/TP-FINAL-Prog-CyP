package simulacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import hilos.Disparador;
import hilos.FabricaHilos;
import monitor.Monitor;
import monitor.politicas.Politica;
import monitor.politicas.PoliticaAleatoria;
import monitor.politicas.PoliticaPriorizada;
import red.ConfiguracionRed;
import red.ControlTemporal;
import red.RedDePetri;
import red.Segmento;
import registro.AnalizadorInvariantes;
import registro.Log;

/**
 * bobo el que lo lee
 * Arma y ejecuta una corrida completa: red, política, monitor, hilos, corte en 200
 * invariantes, finalización sin hilos vivos, verificación con regex y archivos de log.
 */
public final class Simulacion {

    public static final int INVARIANTES_POR_CORRIDA = 200;
    private static final long TIEMPO_MAXIMO_SEGUNDOS = 300;

    private Simulacion() {
    }

    public static Resultado ejecutar(String nombrePolitica, String perfil, String etiqueta, Path carpetaLogs)
            throws InterruptedException, IOException {

        // 1. Red de Petri con su semántica temporal
        long[] alfa = ConfiguracionRed.alfaDelPerfil(perfil);
        ControlTemporal temporal = new ControlTemporal(alfa, ConfiguracionRed.beta());
        RedDePetri red = new RedDePetri(ConfiguracionRed.W_MENOS, ConfiguracionRed.W_MAS,
                ConfiguracionRed.MARCADO_INICIAL, ConfiguracionRed.P_INVARIANTES, temporal);

        // 2. Política (los conflictos los calcula la red a partir de W-)
        Politica politica = crearPolitica(nombrePolitica, red);

        // 3. Hilos: un Disparador por hilo de cada segmento
        Segmento[] segmentos = ConfiguracionRed.segmentos();
        int totalHilos = 0;
        for (Segmento s : segmentos) {
            totalHilos += s.getHilos();
        }
        FabricaHilos fabrica = new FabricaHilos();
        ExecutorService ejecutor = Executors.newFixedThreadPool(totalHilos, fabrica);
        AtomicInteger cupoEntrada = new AtomicInteger(INVARIANTES_POR_CORRIDA);
        CountDownLatch invariantesCompletos = new CountDownLatch(INVARIANTES_POR_CORRIDA);

        long inicio = System.nanoTime();
        Log log = new Log(red.getCantidadTransiciones(), inicio);
        Monitor monitor = new Monitor(red, politica, log);

        List<Disparador> disparadores = new ArrayList<Disparador>();
        for (Segmento s : segmentos) {
            for (int i = 1; i <= s.getHilos(); i++) {
                AtomicInteger cupo = s.getRol() == Segmento.Rol.ENTRADA ? cupoEntrada : null;
                CountDownLatch cuenta = s.getRol() == Segmento.Rol.SALIDA ? invariantesCompletos : null;
                disparadores.add(new Disparador(s.getNombre() + "-" + i, monitor, s.getSecuencia(), cupo, cuenta));
            }
        }
        for (Disparador d : disparadores) {
            ejecutor.execute(d);
        }

        // 4. Esperar los 200 invariantes y terminar todos los hilos
        boolean completo = invariantesCompletos.await(TIEMPO_MAXIMO_SEGUNDOS, TimeUnit.SECONDS);
        long fin = System.nanoTime();
        ejecutor.shutdownNow();                       // interrumpe a los hilos bloqueados en el monitor
        ejecutor.awaitTermination(10, TimeUnit.SECONDS);
        fabrica.esperarQueTerminen(10_000);

        // 5. Verificación de T-invariantes con la expresión regular (requerimiento 13)
        AnalizadorInvariantes analizador = new AnalizadorInvariantes(ConfiguracionRed.REGEX_T_INVARIANTES,
                ConfiguracionRed.GRUPOS_T_INVARIANTES, ConfiguracionRed.NOMBRES_T_INVARIANTES);
        String secuencia = log.getSecuencia();
        AnalizadorInvariantes.Resultado analisis = analizador.analizar(secuencia);

        // 6. Resultado y archivos de log
        Resultado r = new Resultado();
        r.politica = politica.getNombre();
        r.perfil = perfil;
        r.etiqueta = etiqueta;
        r.alfa = alfa;
        r.duracionSegundos = (fin - inicio) / 1e9;
        r.completo = completo;
        r.hilosCreados = fabrica.getCantidadCreados();
        r.hilosVivos = fabrica.contarVivos();
        r.disparosPorTransicion = log.getDisparosPorTransicion();
        r.nombresInvariantes = analisis.getNombres();
        r.cantidadesInvariantes = analisis.getCantidades();
        r.residuo = analisis.getResiduo();
        r.pasadasRegex = analisis.getPasadas();
        r.verificacionesPInv = red.getVerificacionesPInv();
        r.marcadoFinal = red.getMarcado();
        r.volvioAMarcadoInicial = red.volvioAlMarcadoInicial();
        for (Disparador d : disparadores) {
            r.disparosPorHilo.put(d.getNombre(), d.getDisparos());
        }

        String base = r.politica + "_" + perfil + "_" + etiqueta;
        Log.escribir(carpetaLogs.resolve(base + "_disparos.txt"), secuencia + "\n");
        Log.escribir(carpetaLogs.resolve(base + "_eventos.csv"), log.getEventos());
        Log.escribir(carpetaLogs.resolve(base + "_resumen.txt"), r.resumen());
        agregarFila(carpetaLogs.resolve("resultados.csv"), r);
        return r;
    }

    private static Politica crearPolitica(String nombre, RedDePetri red) {
        List<int[]> conflictos = red.conflictosEstructurales();
        int n = red.getCantidadTransiciones();
        if ("aleatoria".equalsIgnoreCase(nombre)) {
            return new PoliticaAleatoria(conflictos, n, new Random());
        }
        if ("priorizada".equalsIgnoreCase(nombre)) {
            return new PoliticaPriorizada(conflictos, n, ConfiguracionRed.PESOS_PRIORIZADA);
        }
        throw new IllegalArgumentException("Politica desconocida: " + nombre + " (aleatoria | priorizada)");
    }

    /** Agrega una fila a la tabla de resultados de todas las corridas. */
    private static synchronized void agregarFila(Path archivo, Resultado r) throws IOException {
        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        StringBuilder texto = new StringBuilder();
        if (!Files.exists(archivo)) {
            texto.append(Resultado.encabezadoCsv()).append('\n');
        }
        texto.append(r.filaCsv()).append('\n');
        Files.write(archivo, texto.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
