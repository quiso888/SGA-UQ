package datastructures;

import exceptions.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Pila genérica propia del SGA (comportamiento LIFO).
 *
 * <p>Se usa para:</p>
 * <ul>
 *   <li>Deshacer la última operación del folio de un huésped: cada cargo o pago se
 *       apila y al deshacer se registra el movimiento contrario (nunca se borra).</li>
 *   <li>Navegación "atrás" entre pantallas de la interfaz gráfica.</li>
 * </ul>
 *
 * <p>Internamente usa un arreglo propio que duplica su capacidad cuando se llena,
 * sin recurrir a ninguna colección de {@code java.util}.</p>
 *
 * @param <T> tipo de los elementos apilados
 */
public class Pila<T> implements Iterable<T> {

    private static final int CAPACIDAD_INICIAL = 8;

    private Object[] elementos;
    private int cantidad;
    private int cima;

    public Pila() {
        this.elementos = new Object[CAPACIDAD_INICIAL];
        this.cantidad = 0;
        this.cima = -1;
    }

    /** Apila un elemento en la cima de la pila. */
    public void apilar(T valor) {
        ensureCapacity(cantidad + 1);
        cima++;
        elementos[cima] = valor;
        cantidad++;
    }

    /**
     * Desapila y devuelve el elemento de la cima.
     *
     * @throws EstructuraVaciaException si la pila no tiene elementos
     */
    public T desapilar() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Pila", "desapilar");
        }
        T valor = (T) elementos[cima];
        elementos[cima] = null;
        cima--;
        cantidad--;
        return valor;
    }

    /** Devuelve el elemento de la cima sin retirarlo. */
    public T verCima() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Pila", "consultar la cima");
        }
        return (T) elementos[cima];
    }

    public boolean estaVacia() {
        return cantidad == 0;
    }

    public int tamano() {
        return cantidad;
    }

    public void limpiar() {
        for (int i = 0; i < cantidad; i++) {
            elementos[i] = null;
        }
        cantidad = 0;
        cima = -1;
    }

    /** Copia el contenido de la pila en un arreglo propio (de cima a base). */
    public Object[] aArreglo() {
        Object[] datos = new Object[cantidad];
        for (int i = 0; i < cantidad; i++) {
            datos[i] = elementos[cima - i];
        }
        return datos;
    }

    private void ensureCapacity(int requerida) {
        if (requerida <= elementos.length) {
            return;
        }
        int nuevaCapacidad = Math.max(requerida, elementos.length * 2);
        Object[] nuevos = new Object[nuevaCapacidad];
        for (int i = 0; i < cantidad; i++) {
            nuevos[i] = elementos[i];
        }
        elementos = nuevos;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorPila();
    }

    /**
     * Iterador propio de la pila: recorre de la cima hacia la base.
     */
    public class IteradorPila implements Iterator<T> {

        private int posicion;

        public IteradorPila() {
            this.posicion = 0;
        }

        @Override
        public boolean hasNext() {
            return posicion < cantidad;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No hay más elementos en la pila.");
            }
            T valor = (T) elementos[cima - posicion];
            posicion++;
            return valor;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Pila[");
        boolean primero = true;
        for (T valor : this) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(valor);
            primero = false;
        }
        return sb.append(']').toString();
    }
}
