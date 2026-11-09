@echo off
rem Compila y ejecuta el TP. Uso: correr.bat [aleatoria o priorizada] [etiqueta] [perfil]
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src src\Main.java src\Experimentos.java
if errorlevel 1 exit /b 1
java -cp out Main %*
