package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista circular genérica propia del SGA.
 *
 * <p>Se usa para la rotación de turnos de los recepcionistas: el turno actual siempre
 * es el nodo "actual" y, al terminar el turno, se avanza al siguiente nodo de forma
 * circular.</p>
 *
 * <p>El último nodo enlaza de vuelta con el primero, por lo que el recorrido es
 * infinito y cíclico.</p>
 *
 * @param <T> tipo de los elementos de la lista circular
 */
public class ListaCircular<T> implements Iterable<T> {

    /** Nodo circular. */
    private static class NodoCircular<T> {
        private T valor;
        private NodoCircular<T> siguiente;

        NodoCircular(T valor) {
            this.valor = valor;
            this.siguiente = this;
        }
    }

    private NodoCircular<T> actual;
    private int tamano;

    public ListaCircular() {
        this.actual = null;
        this.tamano = 0;
    }

    /**
     * Agrega un elemento a la lista. Si es el primero, queda como nodo actual.
     */
    public void agregar(T valor) {
        NodoCircular<T> nuevo = new NodoCircular<>(valor);
        if (actual == null) {
            actual = nuevo;
            nuevo.siguiente = nuevo;
        } else {
            // Se inserta justo después del actual, manteniendo la circularidad.
            NodoCircular<T> ultimo = ultimoNodo();
            nuevo.siguiente = actual;
            ultimo.siguiente = nuevo;
        }
        tamano++;
    }

    /**
     * Elimina el elemento indicado. Si era el nodo actual, el actual pasa a ser el
     * siguiente.
     *
     * @return {@code true} si el elemento existía y fue eliminado
     */
    public boolean remover(T valor) {
        if (actual == null) {
            return false;
        }
        if (tamano == 1) {
            if (iguales(actual.valor, valor)) {
                actual = null;
                tamano = 0;
                return true;
            }
            return false;
        }
        // Busca el nodo a eliminar y su predecesor, sin salir del anillo.
        NodoCircular<T> previo = actual;
        NodoCircular<T> recorrido = actual.siguiente;
        while (recorrido != actual && !iguales(recorrido.valor, valor)) {
            previo = recorrido;
            recorrido = recorrido.siguiente;
        }
        if (recorrido == actual) {
            // El recorrido no encontró el valor: puede ser el nodo actual.
            if (!iguales(actual.valor, valor)) {
                return false;
            }
            // El predecesor del nodo actual es el último del anillo.
            previo = ultimoNodo();
        }
        previo.siguiente = recorrido.siguiente;
        if (recorrido == actual) {
            actual = recorrido.siguiente;
        }
        // Se desconecta el nodo eliminado para que no quede en el anillo.
        recorrido.siguiente = recorrido;
        tamano--;
        return true;
    }

    /** Devuelve el elemento del turno actual. */
    public T actual() {
        if (actual == null) {
            return null;
        }
        return actual.valor;
    }

    /** Devuelve el siguiente elemento en la rotación sin avanzar el turno. */
    public T siguiente() {
        if (actual == null) {
            return null;
        }
        return actual.siguiente.valor;
    }

    /**
     * Avanza el turno: el nodo actual pasa a ser el siguiente de la rotación.
     */
    public void rotar() {
        if (actual != null) {
            actual = actual.siguiente;
        }
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void limpiar() {
        actual = null;
        tamano = 0;
    }

    private NodoCircular<T> ultimoNodo() {
        NodoCircular<T> n = actual;
        while (n.siguiente != actual) {
            n = n.siguiente;
        }
        return n;
    }

    private static boolean iguales(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /** Copia el contenido circular en un arreglo propio, empezando por el actual. */
    public Object[] aArreglo() {
        Object[] datos = new Object[tamano];
        if (actual == null) {
            return datos;
        }
        NodoCircular<T> n = actual;
        for (int i = 0; i < tamano; i++) {
            datos[i] = n.valor;
            n = n.siguiente;
        }
        return datos;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorCircular();
    }

    /**
     * Iterador propio de la lista circular: recorre un giro completo a partir del
     * nodo actual.
     */
    public class IteradorCircular implements Iterator<T> {

        private NodoCircular<T> recorrido;
        private int restantes;

        public IteradorCircular() {
            this.recorrido = actual;
            this.restantes = tamano;
        }

        @Override
        public boolean hasNext() {
            return restantes > 0;
        }

        @Override
        public T next() {
            if (restantes == 0) {
                throw new NoSuchElementException("No hay más elementos en la lista circular.");
            }
            T valor = recorrido.valor;
            recorrido = recorrido.siguiente;
            restantes--;
            return valor;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ListaCircular[");
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
