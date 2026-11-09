#!/bin/sh
# Compila y ejecuta el TP. Uso: ./correr.sh [aleatoria|priorizada] [etiqueta] [perfil]
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -d out -sourcepath src src/Main.java src/Experimentos.java || exit 1
java -cp out Main "$@"
