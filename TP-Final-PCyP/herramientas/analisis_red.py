"""
Verificación independiente de las propiedades de la red del TP (complementa el análisis con PIPE).

Calcula, sin bibliotecas externas:
  - matriz de incidencia I = W+ - W-
  - P-invariantes y T-invariantes mínimos (vectores 0/1)
  - grafo de alcanzabilidad: cantidad de marcados, deadlocks, cota de cada plaza,
    reversibilidad y vivacidad
  - conflictos estructurales
  - hilos por segmento (máximo de marcas simultáneas en las plazas de acción)

Uso:  python3 herramientas/analisis_red.py
"""
from collections import deque
from itertools import product

P = [f"P{i}" for i in range(10)]
T = [f"T{i}" for i in range(10)]

# Arcos de la Figura 1: (entradas, salidas) de cada transición
ARCOS = {
    0: ([0], [1]),          # T0: P0 -> P1
    1: ([1, 7], [2]),       # T1: P1, P7 -> P2
    2: ([2], [3]),          # T2: P2 -> P3
    3: ([3], [7, 9]),       # T3: P3 -> P7, P9
    4: ([1, 7, 8], [4]),    # T4: P1, P7, P8 -> P4
    5: ([4], [7, 8, 9]),    # T5: P4 -> P7, P8, P9
    6: ([1, 8], [5]),       # T6: P1, P8 -> P5
    7: ([5], [6]),          # T7: P5 -> P6
    8: ([6], [8, 9]),       # T8: P6 -> P8, P9
    9: ([9], [0]),          # T9: P9 -> P0
}
M0 = (3, 0, 0, 0, 0, 0, 0, 1, 1, 0)

pre = [[0] * 10 for _ in range(10)]
post = [[0] * 10 for _ in range(10)]
for t, (entradas, salidas) in ARCOS.items():
    for p in entradas:
        pre[p][t] += 1
    for p in salidas:
        post[p][t] += 1
I = [[post[p][t] - pre[p][t] for t in range(10)] for p in range(10)]


def minimales(es_invariante):
    sols = [v for v in product([0, 1], repeat=10) if any(v) and es_invariante(v)]
    return [v for v in sols if not any(w != v and all(a <= b for a, b in zip(w, v)) for w in sols)]


def es_t_inv(x):
    return all(sum(I[p][t] * x[t] for t in range(10)) == 0 for p in range(10))


def es_p_inv(y):
    return all(sum(y[p] * I[p][t] for p in range(10)) == 0 for t in range(10))


def sensibilizada(m, t):
    return all(m[p] >= pre[p][t] for p in range(10))


def disparar(m, t):
    return tuple(m[p] + I[p][t] for p in range(10))


print("Matriz de incidencia I (filas P0..P9, columnas T0..T9)")
for p in range(10):
    print(f"  {P[p]}: " + " ".join(f"{v:>3}" for v in I[p]))

print("\nT-invariantes mínimos:")
for v in minimales(es_t_inv):
    print("  " + " ".join(T[i] for i in range(10) if v[i]))
print("P-invariantes mínimos:")
for v in minimales(es_p_inv):
    constante = sum(v[p] * M0[p] for p in range(10))
    print("  " + " + ".join(f"M({P[p]})" for p in range(10) if v[p]) + f" = {constante}")

# Grafo de alcanzabilidad
vistos = {M0}
arcos = []
cola = deque([M0])
while cola:
    m = cola.popleft()
    for t in range(10):
        if sensibilizada(m, t):
            m2 = disparar(m, t)
            arcos.append((m, t, m2))
            if m2 not in vistos:
                vistos.add(m2)
                cola.append(m2)

print(f"\nMarcados alcanzables: {len(vistos)} · arcos: {len(arcos)}")
deadlocks = [m for m in vistos if not any(sensibilizada(m, t) for t in range(10))]
print("Deadlocks:", "ninguno" if not deadlocks else deadlocks)
cotas = [max(m[p] for m in vistos) for p in range(10)]
print("Cota de cada plaza:", ", ".join(f"{P[p]}={cotas[p]}" for p in range(10)))
print("Acotada:", f"sí ({max(cotas)}-acotada)", "· segura:", "sí" if max(cotas) <= 1 else "no")

ady = {m: [] for m in vistos}
inv = {m: [] for m in vistos}
for a, _, b in arcos:
    ady[a].append(b)
    inv[b].append(a)


def alcanzables(grafo, inicio):
    vis, pila = {inicio}, [inicio]
    while pila:
        for y in grafo[pila.pop()]:
            if y not in vis:
                vis.add(y)
                pila.append(y)
    return vis


reversible = alcanzables(ady, M0) == alcanzables(inv, M0) == vistos
todas = sorted({t for _, t, _ in arcos}) == list(range(10))
print("Reversible (desde todo marcado se vuelve a M0):", "sí" if reversible else "no")
print("Viva (reversible y todas las transiciones disparables):", "sí" if reversible and todas else "no")

print("\nConflictos estructurales:")
for p in range(10):
    salidas = [T[t] for t in range(10) if pre[p][t] > 0]
    if len(salidas) > 1:
        print(f"  {P[p]} -> {{{', '.join(salidas)}}}")

SEGMENTOS = {"SA (T0)": [1], "SB (T1 T2 T3)": [2, 3], "SC (T4 T5)": [4], "SD (T6 T7 T8)": [5, 6], "SE (T9)": [9]}
print("\nHilos por segmento (máximo de marcas simultáneas en sus plazas de acción):")
total = 0
for nombre, plazas in SEGMENTOS.items():
    maximo = max(sum(m[p] for p in plazas) for m in vistos)
    total += maximo
    print(f"  {nombre:15s} plazas {', '.join(P[p] for p in plazas):8s} -> {maximo}")
print(f"  Total: {total} hilos")
