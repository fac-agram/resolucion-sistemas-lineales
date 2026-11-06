package unsa.sistemas.controller;

import unsa.sistemas.model.direct.Cholesky;
import unsa.sistemas.model.direct.GaussJordan;
import unsa.sistemas.model.matrix.Matriz;
import unsa.sistemas.utils.ValidadorMatriz;

public class SistemaController {

    /**
     * Resuelve un sistema de ecuaciones utilizando el Método de Cholesky.
     * Valida previamente si la matriz es simétrica y definida positiva.
     */
    public double[] ejecutarCholesky(double[][] datosA, double[] datosB) {
        try {
            Matriz matrizA = new Matriz(datosA);

            // 1. Validaciones previas según la teoría
            if (!ValidadorMatriz.esSimetrica(matrizA)) {
                throw new IllegalArgumentException("La matriz A no es simétrica, condición obligatoria para Cholesky.");
            }
            if (!ValidadorMatriz.esDefinidaPositiva(matrizA)) {
                throw new IllegalArgumentException("La matriz A no es definida positiva.");
            }

            // 2. Ejecución del modelo
            Cholesky solver = new Cholesky();
            return solver.resolver(datosA, datosB);

        } catch (Exception e) {
            // Manejo de errores controlado para que la interfaz no colapse
            System.err.println("[Error en Cholesky]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve un sistema de ecuaciones utilizando el Método de Gauss-Jordan.
     */
    public double[] ejecutarGaussJordan(double[][] datosA, double[] datosB) {
        try {
            // Gauss-Jordan transforma la matriz en diagonal o diagonal unitaria
            GaussJordan solver = new GaussJordan();
            return solver.resolver(datosA, datosB);

        } catch (Exception e) {
            System.err.println("[Error en Gauss-Jordan]: " + e.getMessage());
            return null;
        }
    }
}