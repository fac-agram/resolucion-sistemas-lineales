# resolucion-sistemas-lineales

Proyecto académico de la UNSA para resolver sistemas de ecuaciones lineales con métodos numéricos directos e iterativos.

## Estado actual

> **Importante:** el proyecto **compila y resuelve varios algoritmos**. Falta resolver varios casos específicos con varios algoritmos, pero el programa compila y devuelve resultados.

| Componente | Estado |
|---|---|
| `Matriz`, `ValidadorMatriz`, `SistemaController` | ✅ Implementados y funcionales |
| Las 9 clases de algoritmos (`Jacobi`, `Crout`, etc.) | ✅ Implementados y funcionales |
| `Main` / punto de entrada | ❌ No existe todavía |
| `ConsoleView`, `Sustituciones`, `Normas`, `AnalisisSensibilidad` | Falta implementar Sustituciones |
| Tests |  ✅ Implementado varios casos de prueba |

Un lector no debe asumir que la aplicación se puede ejecutar hoy: **compila, pero falta corregir ciertos casos a la hora de ejecutar los algoritmos**.
Para poder ejecutar el programa hay que ir a la clase "ConsoleView" y ejecutar el programa desde ahí

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
│   └── SistemaController.java    — Validación, despacho y Resultado para la vista
├── model/
│   ├── matrix/
│   │   ├── Matriz.java           — Matriz con acceso por índice, producto, transpuesta y determinante
│   │   ├── Normas.java           — (vacío) normas vectoriales/matriciales
│   │   └── AnalisisSensibilidad.java — (vacío) análisis de sensibilidad
│   ├── direct/
│   │   ├── ResolutorDirecto.java — (interfaz vacía)
│   │   ├── EliminacionGaussiana.java — stub: Gauss directo
│   │   ├── GaussJordan.java      — stub: Gauss-Jordan
│   │   ├── Crout.java            — stub: Crout
│   │   └── Cholesky.java         — stub: Cholesky
│   └── iterative/
│       ├── ResolutorIterativo.java — (interfaz vacía)
│       ├── Jacobi.java           — stub: Jacobi
│       ├── GaussSeidel.java      — stub: Gauss-Seidel
│       ├── SOR.java              — stub: SOR
│       ├── RelajacionSimple.java — stub: relajación simple
│       └── MejoramientoIterativo.java — stub: mejoramiento iterativo
├── utils/
│   ├── ValidadorMatriz.java      — Validaciones matemáticas (LU, simetría, dominancia…)
│   └── Sustituciones.java        — (vacío) sustitución hacia adelante/atrás
└── view/
    └── ConsoleView.java          — (vacío) consola interactiva
```

## Requisitos y cómo compilar

- **JDK 14 o superior** (el código usa `switch` con flecha `->`). Desarrollado y verificado con `javac 17.0.20.1`.
- Sin dependencias externas: solo `javac`.

```bash
# Desde la raíz del proyecto
javac -d out $(find src -name '*.java')
```

> No existe comando de ejecución todavía porque el proyecto no tiene clase `Main`.

## Checklist de lectura

- [ ] ¿Buscas qué hace hoy el proyecto? → sección **Estado actual**
- [ ] ¿Buscas el nombre de un método para la vista? → sección **Métodos soportados**
- [ ] ¿Buscas dónde vive una clase? → **Estructura del proyecto**
- [ ] ¿Quieres compilar? → **Requisitos y cómo compilar**
