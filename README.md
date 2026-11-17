# resolucion-sistemas-lineales

Proyecto académico de la UNSA para resolver sistemas de ecuaciones lineales con métodos numéricos directos e iterativos, con una interfaz gráfica JavaFX y empaquetado autónomo (para Linux y Windows).

## Estado actual

La aplicación **compila, se ejecuta y resuelve sistemas** a través de su interfaz gráfica. El punto de entrada existe y la app arranca; no es un esqueleto.

| Componente | Estado |
|---|---|
| `Matriz`, `ValidadorMatriz`, `SistemaController` | Implementados y funcionales |
| Las 9 clases de algoritmos (directos e iterativos) | Implementados y con pruebas |
| Interfaz gráfica (`MainApp`, `SistemaFXController`, `Sistema.fxml`) | Implementada y funcional (se lanza y resuelve) |
| `Launcher` (punto de entrada empaquetado) | Implementado: reenvía `main` a `MainApp` |
| Traza de procedimiento y paneles de estado | Implementados |
| `Sustituciones` (utilidad) | Clase auxiliar aún sin contenido: las sustituciones hacia adelante/atrás se resuelven dentro de los resolutores directos |
| Tests | 12 clases, 82 pruebas (ver *Cómo probar*) |

Un detalle de honestidad: los resultados que muestra la app siguen el **contrato de honestidad de resultados** (`Estado`/`Resultado`). Un sistema que no converge o produce valores no finitos se reporta como tal, no como una solución inventada. Ver `openspec/specs/result-honesty/spec.md`.

## Cómo usarla (sin instalar Java ni Maven)

Descarga/descomprime `TrabajoIntegrador.zip` (se genera con `bash package.sh`; vea *Cómo empaquetar*). Dentro hay dos carpetas, una por sistema operativo. El entorno de ejecución (Java) ya va incluido dentro del ZIP: **la computadora que la usa no necesita tener Java ni Maven instalados.**

- **Linux** — entra en `TrabajoIntegrador-linux/` y haz doble clic en `bin/TrabajoIntegrador`.
- **Windows** — entra en `TrabajoIntegrador-windows/` y haz doble clic en `Iniciar.bat`. (En Windows se abre además una ventana de consola junto a la app: es normal y muestra mensajes por si algo falla; cárgala con `LEEME.txt`.)

### La app no abre en Windows: "Una directiva de Control de aplicaciones bloqueó este archivo"

Si al ejecutar aparece un error en consola que menciona `prism_d3d.dll`, `prism_sw.dll`, `no suitable pipeline found` o `No toolkit found`, **no es un defecto del programa ni del ZIP**: es Windows bloqueando las librerías nativas de JavaFX (sin firma digital) que se extraen en tu perfil. Se soluciona en la propia PC, en este orden:

1. **Desbloquear la descarga antes de extraer** (el bloqueo "Mark of the Web" afecta a todo lo descargado de internet): clic derecho sobre `TrabajoIntegrador.zip` → *Propiedades* → tildar **Desbloquear** → Aceptar → y volver a extraer. O en PowerShell:
   ```powershell
   Unblock-File .\TrabajoIntegrador.zip
   ```
2. **Excluir las carpetas en Seguridad de Windows** (necesita permisos de administrador):
   *Seguridad de Windows → Protección contra virus y amenazas → Administrar la seguridad → Configuraciones de exclusión → Agregar o quitar exclusiones* y agregar:
   - la carpeta extraída `TrabajoIntegrador-windows\`
   - `C:\Users\<tu usuario>\.openjfx\`
3. Si la PC es de un **dominio corporativo/universitario** con directivas (WDAC/AppLocker) que bloquean DLLs sin firma: ninguna solución local vale, hace falta el administrador de TI o usar una PC personal.

Opción definitiva (de pago, para productos): **firmar el ejecutable** con un certificado de código. Los artefactos de este proyecto, académicos, van sin firmar — por eso la documentación del bloqueo es parte de la entrega.

Tamaño del artefacto: el ZIP mide **~86 MB**, por encima del límite de 25 MB del correo. Entrega por USB, Google Drive o la plataforma del campus, no por email.

### Cómo resolver un sistema dentro de la app

1. Escribe (o carga desde un `.txt`) la matriz de coeficientes y el vector de resultados.
2. Elige el método en el selector.
3. Pulsa *Resolver*. El resultado, la traza del procedimiento y el estado aparecen en sus paneles.

En el repositorio hay casos listos para cargar en la carpeta `pruebas txt/` (p. ej. `prueba iterativo (converge).txt`).

## Cómo probar

```bash
sh ./mvnw -o clean test
```

Salida esperada: `Tests run: 82, Failures: 0, Errors: 0`. El modo `-o` (offline) usa la caché local de Maven; no requiere red.

## Cómo empaquetar (build desde el código)

En la **máquina de build** (no en la de uso) hacen falta:

- **JDK 17** con `jlink` y `jpackage` (el runtime se probó con Temurin; la versión Windows empaquetada está fijada a Temurin `17.0.21+9`).
- Maven Wrapper incluido en el repo (`./mvnw`); no hace falta Maven global.

Pasos:

```bash
# 1) Compilar y correr las pruebas (offline)
sh ./mvnw -o clean test

# 2) Generar el entregable (armado + verificación + zip)
bash package.sh
```

`package.sh` produce `dist/TrabajoIntegrador.zip` con ambas carpetas y auto-verifica su contenido. Por defecto es **offline** y reutiliza la caché `~/.cache/gentle-deliverable`; solo descarga de red si pasas `--fetch` (descarga única de los insumos de Windows, con URLs y huellas fijas en `package/pins.txt`). Para probar únicamente la ruta Linux: `bash package.sh --linux-only`. La carpeta `dist/` está en `.gitignore` y **nunca** se sube al repositorio.

## Métodos soportados por la interfaz

`SistemaController` expone nueve identificadores (constantes públicas):

### Directos

| Identificador | Descripción |
|---|---|
| `GAUSS` | Eliminación Gaussiana con pivoteo parcial. |
| `GAUSS_JORDAN` | Gauss-Jordan: reduce a forma escalonada reducida. |
| `CROUT` | Factorización LU de Crout (sin pivoteo). |
| `CHOLESKY` | Factorización A = LLᵀ para simétricas definidas positivas. |

### Iterativos

| Identificador | Descripción |
|---|---|
| `JACOBI` | Iteración por desplazamiento de coordenadas. |
| `GAUSS_SEIDEL` | Iteración que reutiliza los valores ya actualizados de cada fila. |
| `SOR` | Sobrerelajación sucesiva (relajación con factor de aceleración). |
| `RELAJACION` | Relajación simple. |
| `MEJORAMIENTO` | Corrección iterativa sobre una aproximación previa. |

## Estructura del proyecto

```
src/unsa/sistemas/
├── controller/
│   └── SistemaController.java    — Validación, despacho y Resultado (Estado) para la vista
├── model/
│   ├── Resolutor.java            — Contrato común de los resolutores
│   ├── matrix/
│   │   ├── Matriz.java           — Acceso por índice, producto, transpuesta y determinante
│   │   ├── Normas.java           — Normas vectoriales y matriciales
│   │   └── AnalisisSensibilidad.java — Análisis de sensibilidad (con pruebas)
│   ├── direct/
│   │   ├── ResolutorDirecto.java — Interfaz
│   │   ├── EliminacionGaussiana.java — Gauss con pivoteo parcial
│   │   ├── GaussJordan.java      — Gauss-Jordan
│   │   ├── Crout.java            — Factorización LU de Crout
│   │   └── Cholesky.java         — Factorización de Cholesky
│   ├── iterative/
│   │   ├── ResolutorIterativo.java — Interfaz
│   │   ├── Jacobi.java           — Jacobi
│   │   ├── GaussSeidel.java      — Gauss-Seidel
│   │   ├── SOR.java              — Sobrerelajación sucesiva
│   │   ├── RelajacionSimple.java — Relajación simple
│   │   └── MejoramientoIterativo.java — Mejoramiento iterativo
│   └── trace/
│       ├── Paso.java             — Un paso de la traza
│       └── Traza.java            — Registro del procedimiento
├── utils/
│   ├── ValidadorMatriz.java      — Validaciones matemáticas (LU, simetría, dominancia…)
│   └── Sustituciones.java        — (placeholder; las sustituciones están en los resolutores directos)
└── view/
    ├── MainApp.java              — Aplicación JavaFX (carga Sistema.fxml)
    ├── Launcher.java             — Punto de entrada del jar/app-image; reenvía main a MainApp
    ├── SistemaFXController.java  — Controlador de la vista
    ├── Sistema.fxml              — Definición de la interfaz
    └── estilos.css               — Estilos
```

## Estado por sistema operativo del entregable

| Plataforma | Se construye aquí | Arranque verificado | Estado |
|---|---|---|---|
| **Linux x86-64** | Sí (`jlink` + `jpackage`, offline) | Sí: smoke-test de la GUI (ventana viva ≥12 s + captura) | **VERIFICADO** (evidencia en `openspec/changes/deliverable-packaging/evidence/`) |
| **Windows x86-64** | Sí, armado estático con el runtime Temurin fijado por hash | **No en esta máquina** (aquí no corre Windows) | **VERIFICADO SOLO ESTÁTICAMENTE** — runtime *sin verificar* hasta que se devuelva el `CHECKLIST-WINDOWS.txt` ejecutado en una PC con Windows |

La carpeta de Windows se arma de componentes fijados por su huella SHA-256 (`package/pins.txt`), y se validan de forma estática (`java.exe`, DLLs del JVM, los cuatro `.jar` de JavaFX `-win`, comillas de rutas en `Iniciar.bat`, tamaño dentro de rango). Su ejecución real queda pendiente de la atestación descrita en `CHECKLIST-WINDOWS.txt`: sin ese checklist devuelto, el estado de Windows no pasa de *sin verificar*.

## Referencias del proyecto (SDD)

- Especificaciones vivas: `openspec/specs/` (`test-execution-baseline`, `result-honesty`, `mejoramiento-iterativo`, `procedure-trace`).
- Cambios en curso/archivados: `openspec/changes/`.
- El empaquetado es el cambio `deliverable-packaging`: `openspec/changes/deliverable-packaging/`.

## Checklist de lectura

- [ ] ¿Buscas qué hace hoy el proyecto? → sección **Estado actual**
- [ ] ¿Solo quieres usar la app? → **Cómo usarla** (descomprime el ZIP; sin Java)
- [ ] ¿Buscas el nombre de un método para la vista? → **Métodos soportados**
- [ ] ¿Buscas dónde vive una clase? → **Estructura del proyecto**
- [ ] ¿Quieres compilar y probar? → **Cómo probar** y **Cómo empaquetar**
