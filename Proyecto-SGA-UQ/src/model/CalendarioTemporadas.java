package model;

import java.time.LocalDate;

import datastructures.ListaEnlazada;

/**
 * Calendario de temporadas del alojamiento.
 *
 * El administrador define los rangos de fechas de cada temporada (Baja, Media y
 * Alta). El calendario es el que responde a la pregunta "¿en qué temporada cae esta
 * noche?" durante la liquidación.
 */
public class CalendarioTemporadas {

    /** Rango de fechas de una temporada. */
    public static class Periodo {
        private final Temporada temporada;
        private final LocalDate inicio;
        private final LocalDate fin;

        public Periodo(Temporada temporada, LocalDate inicio, LocalDate fin) {
            if (temporada == null || inicio == null || fin == null) {
                throw new IllegalArgumentException("Los datos del periodo de temporada son obligatorios.");
            }
            if (fin.isBefore(inicio)) {
                throw new IllegalArgumentException(
                        "El periodo de la temporada " + temporada + " está invertido.");
            }
            this.temporada = temporada;
            this.inicio = inicio;
            this.fin = fin;
        }

        public Temporada getTemporada() {
            return temporada;
        }

        public LocalDate getInicio() {
            return inicio;
        }

        public LocalDate getFin() {
            return fin;
        }

        public boolean contiene(LocalDate fecha) {
            return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
        }

        @Override
        public String toString() {
            return temporada.getDescripcion() + " (" + inicio + " a " + fin + ")";
        }
    }

    private final ListaEnlazada<Periodo> periodos = new ListaEnlazada<>();
    private Temporada temporadaPorDefecto = Temporada.BAJA;

    /**
     * Registra un periodo de temporada.
     */
    public void registrar(Temporada temporada, LocalDate inicio, LocalDate fin) {
        periodos.agregar(new Periodo(temporada, inicio, fin));
    }

    /**
     * Devuelve la temporada a la que pertenece una fecha. Si ningún periodo la
     * contiene, devuelve la temporada por defecto (temporada base).
     */
    public Temporada temporadaDe(LocalDate fecha) {
        if (fecha == null) {
            return temporadaPorDefecto;
        }
        for (Periodo periodo : periodos) {
            if (periodo.contiene(fecha)) {
                return periodo.getTemporada();
            }
        }
        return temporadaPorDefecto;
    }

    /** Elimina un periodo de temporada. */
    public boolean eliminar(Periodo periodo) {
        return periodos.removerValor(periodo);
    }

    /** Temporada usada cuando ninguna regla cubre la fecha consultada. */
    public Temporada getTemporadaPorDefecto() {
        return temporadaPorDefecto;
    }

    public void setTemporadaPorDefecto(Temporada temporadaPorDefecto) {
        this.temporadaPorDefecto = temporadaPorDefecto;
    }

    public ListaEnlazada<Periodo> getPeriodos() {
        return periodos;
    }

    public void limpiar() {
        periodos.limpiar();
    }
}
