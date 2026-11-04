package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;

import exceptions.FechasInvalidasException;

/**
 * Estancia: el intervalo de tiempo que un grupo ocupa un apartamento.
 *
 * Regla central del alojamiento:la noche de salida no se cobra ni se ocupa.
 * Por eso la estancia del 10 al 12 de marzo son dos noches (10 y 11 de marzo) y el
 * apartamento queda libre el 12
 *
 * La estancia es iterable sobre las noches a cobrar, lo que permite
 * liquidarla con un simple for-each:
 *
 * for (LocalDate noche : estancia) {
 *     double valorNoche = tarifa * personasQuePaganEn(noche);
 *     // ...
 * }
 * }
 */
public class Estancia implements Iterable<LocalDate> {

    private final LocalDate fechaEntrada;
    private final LocalDate fechaSalida;

    /**
     * @throws FechasInvalidasException si la salida no es posterior a la entrada
     */
    public Estancia(LocalDate fechaEntrada, LocalDate fechaSalida) {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new FechasInvalidasException("Las fechas de entrada y salida son obligatorias.");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new FechasInvalidasException(fechaEntrada.toString(), fechaSalida.toString());
        }
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    /**
     * Número de noches a cobrar. La noche de salida no se cuenta.
     */
    public int numeroNoches() {
        return (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /**
     * {@code true} si la fecha indicada es una noche ocupada por la estancia
     * (la fecha de salida no está ocupada).
     */
    public boolean ocupaNoche(LocalDate fecha) {
        if (fecha == null) {
            return false;
        }
        return !fecha.isBefore(fechaEntrada) && fecha.isBefore(fechaSalida);
    }

    /**
     * {@code true} si ambas estancias comparten al menos una noche.
     *
     * <p>Dos estancias se solapan cuando la entrada de una es anterior a la salida
     * de la otra y viceversa. Como la noche de salida no se ocupa, dos estancias que
     * se tocan (una sale el día que la otra entra) <b>no</b> se solapan.</p>
     */
    public boolean seSolapa(Estancia otra) {
        if (otra == null) {
            return false;
        }
        return this.fechaEntrada.isBefore(otra.fechaSalida)
                && otra.fechaEntrada.isBefore(this.fechaSalida);
    }

    /** Desplaza toda la estancia un número de días (positivo o negativo). */
    public Estancia desplazada(int dias) {
        return new Estancia(fechaEntrada.plusDays(dias), fechaSalida.plusDays(dias));
    }

    /**
     * Devuelve las noches a cobrar como una lista propia del SGA (sin usar
     * colecciones de {@code java.util}).
     */
    public datastructures.ListaEnlazada<LocalDate> noches() {
        datastructures.ListaEnlazada<LocalDate> fechas = new datastructures.ListaEnlazada<>();
        for (LocalDate noche : this) {
            fechas.agregar(noche);
        }
        return fechas;
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return new IteradorNoches();
    }

    /**
     * Iterador propio de la estancia: recorre noche por noche las fechas a cobrar.
     */
    private class IteradorNoches implements Iterator<LocalDate> {

        private LocalDate actual;

        IteradorNoches() {
            this.actual = fechaEntrada;
        }

        @Override
        public boolean hasNext() {
            return actual != null && actual.isBefore(fechaSalida);
        }

        @Override
        public LocalDate next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No quedan noches en la estancia.");
            }
            LocalDate noche = actual;
            actual = actual.plusDays(1);
            return noche;
        }
    }

    @Override
    public String toString() {
        return fechaEntrada + " → " + fechaSalida + " (" + numeroNoches() + " noches)";
    }
}
