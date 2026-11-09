# TP Final — Programación Concurrente y Paralela 2026

Red de Petri del **sistema de procesamiento de transacciones de pago** ejecutada por un monitor de
concurrencia en Java. Grupo: _(completar)_ · Integrantes: _(completar)_

## Requisitos

- JDK 8 o superior (probado con OpenJDK 21 compilando con `--release 8`).
- No usa bibliotecas externas: solo la biblioteca estándar de Java.

## Compilar y ejecutar

Desde esta carpeta:

```
mkdir out
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/Experimentos.java
java -cp out Main
```

Los mismos comandos funcionan en Linux, macOS y Windows: con `-sourcepath`, javac compila
todas las clases que usan `Main` y `Experimentos` (con JDK 8 la carpeta `out` tiene que existir antes).

O con los scripts: `./correr.sh` (Linux/macOS) o `correr.bat` (Windows).
En un IDE (IntelliJ, Eclipse, NetBeans, VS Code): abrir la carpeta, marcar `src/` como carpeta de
fuentes y ejecutar la clase `Main`.

### Argumentos de Main

```
java -cp out Main [politica] [etiqueta] [perfil]
```

| Argumento | Valores | Por defecto |
| --- | --- | --- |
| politica | `aleatoria`, `priorizada` | `aleatoria` |
| etiqueta | cualquier texto (nombra los logs) | `01` |
| perfil | `base`, `riesgo_lento`, `tarjeta_lenta`, `rapido`, `sin_tiempo` | `base` |

Con el perfil `base`, cada corrida completa 200 invariantes en unos 30 segundos y termina sola,
sin hilos activos.

### Plan de experimentos

```
java -cp out Experimentos          # E1 a E8: 10 corridas con tiempos base, 5 por cada variación
java -cp out Experimentos 3 E1,E2  # solo E1 y E2, 3 corridas cada uno
```

## Salidas (carpeta `logs/`)

| Archivo | Contenido |
| --- | --- |
| `<politica>_<perfil>_<etiqueta>_disparos.txt` | Secuencia de transiciones disparadas en una línea (`T0T1T0...`), la que analiza la expresión regular |
| `<politica>_<perfil>_<etiqueta>_eventos.csv` | Cada disparo: instante (ms), hilo, transición y marcado resultante |
| `<politica>_<perfil>_<etiqueta>_resumen.txt` | Invariantes por tipo, residuo de la regex, verificaciones de P-invariantes, hilos vivos, duración |
| `resultados.csv` | Una fila por corrida (todas las corridas) |

## Estructura

```
src/
  Main.java, Experimentos.java
  simulacion/   Simulacion, Resultado
  monitor/      MonitorInterface, Monitor, Colas
  monitor/politicas/  Politica, PoliticaPorConflictos, PoliticaAleatoria, PoliticaPriorizada
  red/          RedDePetri, ControlTemporal, ConfiguracionRed, Segmento
  hilos/        Disparador, FabricaHilos
  registro/     Log, AnalizadorInvariantes
docs/           diagrama_clases.png, diagrama_secuencia.png (+ fuentes .mmd), figuras de la red
herramientas/   analisis_red.py (verificación independiente de propiedades), comparacion/SoloDespertar.java
logs/           logs de las 50 corridas del informe, resultados.csv y comparacion_solo_despertar.txt
```

El informe (con el análisis de la red, los invariantes, la cantidad de hilos, las políticas,
los resultados y el análisis de tiempos) se entrega como documento aparte.

La única clase que conoce esta red en particular es `red/ConfiguracionRed.java`. Para ejecutar otra
red alcanza con cambiar esa clase: el monitor no cambia.
