package gui;

import datastructures.Pila;

/**
 * Historial de navegación entre pantallas para el botón Atrás
 *
 * Cada vez que el usuario abre una pantalla nueva se apila la anterior; al
 * pulsar «Atrás» se desapila la última y se vuelve a ella.
 */
public class HistorialNavegacion {

    private final Pila<String> pantallas = new Pila<>();
    private String actual;

    /** Registra la pantalla que se acaba de abrir y apila la anterior. */
    public void visitar(String pantalla) {
        if (pantalla == null || pantalla.equals(actual)) {
            return;
        }
        if (actual != null) {
            pantallas.apilar(actual);
        }
        actual = pantalla;
    }

    /**
     * Retrocede a la pantalla anterior.
     *
     * @return el nombre de la pantalla a la que se vuelve o {@code null} si no hay
     */
    public String atras() {
        if (pantallas.estaVacia()) {
            return null;
        }
        actual = pantallas.desapilar();
        return actual;
    }

    public boolean puedeVolver() {
        return !pantallas.estaVacia();
    }

    public String getActual() {
        return actual;
    }

    /** Olvida todo el recorrido (por ejemplo, al cambiar de rol). */
    public void reiniciar() {
        pantallas.limpiar();
        actual = null;
    }
}
