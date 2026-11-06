# GUÍA TÉCNICA DE TASKFLOW: COMPILACIÓN, EJECUCIÓN Y REPOSITORIO (GIT)

**Módulo:** Programación de Servicios y Procesos (PSP) — 2º DAM  
**Entorno:** Java 21 — Consola / Eclipse IDE

## 1. ¿QUÉ ES TASKFLOW?
Es una aplicación de consola en Java 21 para gestionar y ejecutar tareas con archivos de forma concurrente. El sistema usa un pool de 4 hilos trabajadores, una cola bloqueante (`BlockingQueue`) con capacidad para 20 tareas en espera, y lanza procesos hijos con `ProcessBuilder` para la operación pesada `ESTADISTICAS_CSV` (limitando a un máximo de 2 procesos simultáneos mediante un semáforo). Además, monitoriza el estado del sistema cada 2 segundos y asegura que al cerrar no se pierda ninguna tarea.

## 2. ÁRBOL DE DIRECTORIOS DEL PROYECTO
```text
TaskFlow_EntregaFinal/
├── bin/                                // Archivos .class compilados
├── src/
│   └── taskflow/
│       ├── app/
│       │   └── TaskFlowMain.java       // Clase principal (CLI y menú)
│       ├── concurrencia/
│       │   ├── GestorColaTrabajos.java // Manejo de la BlockingQueue y entrada de tareas
│       │   └── MotorEjecucion.java     // Gestión del pool de hilos y Semáforos
│       ├── io/
│       │   └── GestorFicheros.java     // Lectura de CSV de solicitudes y guardado de resultados
│       ├── modelo/
│       │   ├── EstadoTrabajo.java      // Enum de estados (PENDIENTE, EN_COLA, etc.)
│       │   ├── SolicitudTrabajo.java   // Validación de los parámetros de entrada
│       │   └── TipoOperacion.java      // Tipos de tareas (ANALIZAR_TEXTO, BUSCAR_TEXTO, ESTADISTICAS_CSV)
│       └── procesadores/
│           ├── EstadisticasCsvHijo.java// Programa Java independiente (proceso hijo)
│           └── Procesadores.java       // Lógica de conteo, búsqueda y llamadas a ProcessBuilder
├── docs/
│   ├── Memoria_Tecnica_TaskFlow.pdf    // Memoria del proyecto
│   └── coevaluacion_individual.md      // Nota individual del grupo
├── datos_numeros.csv                   // Archivo de prueba para el proceso hijo
├── prueba_texto.txt                    // Archivo de prueba para búsquedas
├── solicitudes_100.csv                 // El lote de 100 solicitudes obligatorias
├── resultados_finales.csv              // Salida generada automáticamente al cerrar
└── README.md                           // Este archivo
```

## 3. CÓMO COMPILAR Y EJECUTAR DESDE LA TERMINAL
Si prefieres no usar Eclipse, puedes lanzarlo desde la consola de comandos (CMD, PowerShell o Bash) siguiendo estos pasos desde la raíz del proyecto:

1. **Crear la carpeta para los binarios:**
   ```bash
   mkdir bin
   ```
2. **Compilar todo el código fuente en Java 21:**
   ```bash
   javac -encoding UTF-8 -d bin src/taskflow/modelo/*.java src/taskflow/concurrencia/*.java src/taskflow/procesadores/*.java src/taskflow/io/*.java src/taskflow/app/*.java
   ```
3. **Ejecutar el programa principal:**
   ```bash
   java -cp bin taskflow.app.TaskFlowMain
   ```

## 4. CÓMO EJECUTAR EN ECLIPSE IDE
1. Importa la carpeta `TaskFlow_EntregaFinal` en tu Workspace (**File > Import > Existing Projects into Workspace**).
2. Abre las propiedades del proyecto y asegúrate en **Java Build Path** y **Java Compiler** de que está seleccionado el JDK de **Java 21**.
3. **Importante:** Comprueba que en la carpeta `src` no haya ningún archivo `module-info.java`. Si existe, bórralo para que no dé problemas de permisos al lanzar el proceso hijo.
4. Haz clic derecho sobre `TaskFlowMain.java` y selecciona **Run As > Java Application**.

## 5. FUNCIONAMIENTO DEL MENÚ INTERACTIVO (CLI)
Al arrancar el programa, la pantalla mostrará el estado de los hilos cada 2 segundos y dejará usar las siguientes opciones:

* **Opción [1] Ver Estados:** Saca por pantalla una tabla con todas las tareas pasadas, su estado actual y el resultado que han devuelto.
* **Opción [2] Añadir Solicitud:** Permite meter una tarea a mano de forma directa escribiendo una línea con formato CSV (`ID;OPERACION;RUTA;PARAMETROS`).
* **Opción [3] Cancelar Trabajo:** Te pide el ID de una tarea. Si todavía está esperando en la cola, la cancela inmediatamente.
* **Opción [4] Salir y Cerrar:** El programa deja de admitir tareas nuevas, espera a que terminen los hilos que están trabajando en ese momento, vuelca todo a `resultados_finales.csv` y comprueba que se cumpla el balance obligatorio: `ACEPTADOS = COMPLETADOS + FALLIDOS + CANCELADOS`.

## 6. REPARTO DE TRABAJO Y COMMITS DE GIT
Para cumplir con el desarrollo en un repositorio común, nos hemos repartido las tareas del proyecto y subido los bloques de código de la siguiente manera:

### Integrante 1: Entrada de datos y validaciones
```bash
git add src/taskflow/modelo/TipoOperacion.java src/taskflow/modelo/SolicitudTrabajo.java src/taskflow/io/GestorFicheros.java
git commit -m "Estructura de datos, lectura de archivos CSV y validacion de solicitudes"
git push origin main
```

### Integrante 2: Motor de ejecución y estados
```bash
git add src/taskflow/modelo/EstadoTrabajo.java src/taskflow/modelo/Trabajo.java src/taskflow/concurrencia/GestorColaTrabajos.java
git commit -m "Control de la cola bloqueante, hilos trabajadores y maquina de estados"
git push origin main
```

### Integrante 3: Procesadores y Multiproceso
```bash
git add src/taskflow/procesadores/EstadisticasCsvHijo.java src/taskflow/procesadores/Procesadores.java src/taskflow/concurrencia/MotorEjecucion.java
git commit -m "Logica de las tareas de texto y lanzamiento del proceso hijo con ProcessBuilder"
git push origin main
```

### Integrante 4: Interfaz de consola, Monitor y Pruebas
```bash
git add src/taskflow/app/TaskFlowMain.java solicitudes_100.csv README.md docs/
git commit -m "Menu de consola, hilo de monitorizacion periodica y lote de 100 pruebas"
git push origin main
```
