package datastructures;

import exceptions.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola genérica propia del SGA (comportamiento FIFO).
 *
 * <p>Se usa para las solicitudes que llegan del canal externo (plataforma de
 * terceros), que deben procesarse una a una y en orden de llegada.</p>
 *
 * <p>Internamente implementa un buffer circular con arreglo propio, de modo que el
 * desencolado y el encolado son de tiempo constante.</p>
 *
 * @param <T> tipo de los elementos encolados
 */
public class Cola<T> implements Iterable<T> {

    private static final int CAPACIDAD_INICIAL = 8;

    private Object[] elementos;
    private int cantidad;
    private int frente;
    private int final_;

    public Cola() {
        this.elementos = new Object[CAPACIDAD_INICIAL];
        this.cantidad = 0;
        this.frente = 0;
        this.final_ = 0;
    }

    /** Encola un elemento al final de la cola. */
    public void encolar(T valor) {
        ensureCapacity(cantidad + 1);
        elementos[final_] = valor;
        final_ = (final_ + 1) % elementos.length;
        cantidad++;
    }

    /**
     * Desencola y devuelve el elemento que está al frente de la cola.
     *
     * @throws EstructuraVaciaException si la cola no tiene elementos
     */
    public T desencolar() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Cola", "desencolar");
        }
        T valor = (T) elementos[frente];
        elementos[frente] = null;
        frente = (frente + 1) % elementos.length;
        cantidad--;
        return valor;
    }

    /** Devuelve el elemento del frente sin retirarlo. */
    public T verFrente() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Cola", "consultar el frente");
        }
        return (T) elementos[frente];
    }

    public boolean estaVacia() {
        return cantidad == 0;
    }

    public int tamano() {
        return cantidad;
    }

    public void limpiar() {
        for (int i = 0; i < cantidad; i++) {
            elementos[(frente + i) % elementos.length] = null;
        }
        cantidad = 0;
        frente = 0;
        final_ = 0;
    }

    /** Copia el contenido de la cola en un arreglo propio (de frente a final). */
    public Object[] aArreglo() {
        Object[] datos = new Object[cantidad];
        for (int i = 0; i < cantidad; i++) {
            datos[i] = elementos[(frente + i) % elementos.length];
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
            nuevos[i] = elementos[(frente + i) % elementos.length];
        }
        elementos = nuevos;
        frente = 0;
        final_ = cantidad;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorCola();
    }

    /**
     * Iterador propio de la cola: recorre del frente hacia el final.
     */
    public class IteradorCola implements Iterator<T> {

        private int posicion;

        public IteradorCola() {
            this.posicion = 0;
        }

        @Override
        public boolean hasNext() {
            return posicion < cantidad;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No hay más elementos en la cola.");
            }
            T valor = (T) elementos[(frente + posicion) % elementos.length];
            posicion++;
            return valor;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Cola[");
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
