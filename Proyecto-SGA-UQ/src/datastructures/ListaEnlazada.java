package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista simplemente enlazada genérica propia del SGA.
 *
 * <p>Implementa {@link Iterable} con una clase {@link Iterator} propia, de modo que
 * puede recorrerse con el for-each sin usar ninguna colección de {@code java.util}.</p>
 *
 * <p>Nodo interno {@link Nodo} enlazado a su sucesor.</p>
 *
 * @param <T> tipo de los elementos almacenados
 */
public class ListaEnlazada<T> implements Iterable<T> {

    /** Nodo de la lista simplemente enlazada. */
    protected static class Nodo<T> {
        private T valor;
        private Nodo<T> siguiente;

        Nodo(T valor) {
            this.valor = valor;
            this.siguiente = null;
        }

        public T getValor() {
            return valor;
        }

        public Nodo<T> getSiguiente() {
            return siguiente;
        }

        void setSiguiente(Nodo<T> siguiente) {
            this.siguiente = siguiente;
        }
    }

    protected Nodo<T> cabeza;
    protected Nodo<T> cola;
    protected int tamano;

    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    /**
     * Constructor de copia: crea una lista nueva con los mismos elementos.
     * Se usa para poder modificar una lista mientras se recorre.
     */
    public ListaEnlazada(ListaEnlazada<T> origen) {
        this();
        if (origen != null) {
            for (T valor : origen) {
                agregar(valor);
            }
        }
    }

    /** Agrega un elemento al final de la lista. */
    public void agregar(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    /** Agrega un elemento al inicio de la lista. */
    public void agregarAlInicio(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        nuevo.siguiente = cabeza;
        cabeza = nuevo;
        if (cola == null) {
            cola = nuevo;
        }
        tamano++;
    }

    /**
     * Inserta un elemento en la posición indicada.
     *
     * @throws IndexOutOfBoundsException si el índice no está en el rango [0, tamano]
     */
    public void agregar(T valor, int indice) {
        if (indice < 0 || indice > tamano) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + tamano);
        }
        if (indice == tamano) {
            agregar(valor);
            return;
        }
        if (indice == 0) {
            agregarAlInicio(valor);
            return;
        }
        Nodo<T> anterior = nodoEn(indice - 1);
        Nodo<T> nuevo = new Nodo<>(valor);
        nuevo.siguiente = anterior.siguiente;
        anterior.siguiente = nuevo;
        tamano++;
    }

    /** Devuelve el elemento que ocupa la posición indicada. */
    public T obtener(int indice) {
        return nodoEn(indice).valor;
    }

    /** Devuelve el primer elemento o {@code null} si la lista está vacía. */
    public T primero() {
        return cabeza == null ? null : cabeza.valor;
    }

    /** Devuelve el último elemento o {@code null} si la lista está vacía. */
    public T ultimo() {
        return cola == null ? null : cola.valor;
    }

    /** Elimina y devuelve el elemento que ocupa la posición indicada. */
    public T remover(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + tamano);
        }
        T valor;
        if (indice == 0) {
            valor = cabeza.valor;
            cabeza = cabeza.siguiente;
            if (cabeza == null) {
                cola = null;
            }
        } else {
            Nodo<T> anterior = nodoEn(indice - 1);
            Nodo<T> aEliminar = anterior.siguiente;
            valor = aEliminar.valor;
            anterior.siguiente = aEliminar.siguiente;
            if (aEliminar == cola) {
                cola = anterior;
            }
        }
        tamano--;
        return valor;
    }

    /** Elimina la primera aparición del valor indicado. Devuelve {@code true} si lo eliminó. */
    public boolean removerValor(T valor) {
        if (cabeza == null) {
            return false;
        }
        if (iguales(cabeza.valor, valor)) {
            cabeza = cabeza.siguiente;
            if (cabeza == null) {
                cola = null;
            }
            tamano--;
            return true;
        }
        Nodo<T> anterior = cabeza;
        while (anterior.siguiente != null) {
            if (iguales(anterior.siguiente.valor, valor)) {
                anterior.siguiente = anterior.siguiente.siguiente;
                if (anterior.siguiente == null) {
                    cola = anterior;
                }
                tamano--;
                return true;
            }
            anterior = anterior.siguiente;
        }
        return false;
    }

    /** {@code true} si la lista contiene el valor indicado. */
    public boolean contiene(T valor) {
        return indiceDe(valor) >= 0;
    }

    /** Devuelve el índice de la primera aparición del valor o {@code -1} si no existe. */
    public int indiceDe(T valor) {
        int i = 0;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (iguales(actual.valor, valor)) {
                return i;
            }
            actual = actual.siguiente;
            i++;
        }
        return -1;
    }

    /** Elimina todos los elementos. */
    public void limpiar() {
        cabeza = null;
        cola = null;
        tamano = 0;
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    /** Invierte la lista enlazando los nodos en sentido contrario (algoritmo recursivo). */
    public void invertir() {
        Nodo<T> anterior = null;
        Nodo<T> actual = cabeza;
        cola = cabeza;
        invertirRecursivo(actual, anterior);
    }

    private void invertirRecursivo(Nodo<T> actual, Nodo<T> anterior) {
        if (actual == null) {
            cabeza = anterior;
            return;
        }
        Nodo<T> siguiente = actual.siguiente;
        actual.siguiente = anterior;
        invertirRecursivo(siguiente, actual);
    }

    /**
     * Ordena la lista con el comparador indicado (ordenamiento por inserción,
     * sin usar ninguna colección de {@code java.util}).
     *
     * @param comparador comparador que define el orden
     */
    public void ordenar(java.util.Comparator<T> comparador) {
        if (comparador == null || tamano < 2) {
            return;
        }
        Object[] datos = aArreglo();
        for (int i = 1; i < datos.length; i++) {
            Object actual = datos[i];
            int j = i - 1;
            while (j >= 0 && comparador.compare(cast(datos[j]), cast(actual)) > 0) {
                datos[j + 1] = datos[j];
                j--;
            }
            datos[j + 1] = actual;
        }
        limpiar();
        for (Object dato : datos) {
            agregar(cast(dato));
        }
    }

    @SuppressWarnings("unchecked")
    private T cast(Object valor) {
        return (T) valor;
    }

    protected Nodo<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + tamano);
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual;
    }

    /** Compara dos valores admitiendo {@code null}. */
    protected static boolean iguales(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /** Devuelve los elementos en un arreglo propio (sin usar java.util). */
    public Object[] aArreglo() {
        Object[] datos = new Object[tamano];
        int i = 0;
        for (Nodo<T> n = cabeza; n != null; n = n.siguiente) {
            datos[i++] = n.valor;
        }
        return datos;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorLista();
    }

    /**
     * Iterador propio de la lista simplemente enlazada.
     */
    public class IteradorLista implements Iterator<T> {

        private Nodo<T> actual;
        private Nodo<T> previo;

        public IteradorLista() {
            this.actual = cabeza;
            this.previo = null;
        }

        @Override
        public boolean hasNext() {
            return actual != null;
        }

        @Override
        public T next() {
            if (actual == null) {
                throw new NoSuchElementException("No hay más elementos en la lista.");
            }
            T valor = actual.valor;
            previo = actual;
            actual = actual.siguiente;
            return valor;
        }

        @Override
        public void remove() {
            if (previo == null) {
                throw new IllegalStateException("No se puede remover: el iterador no está sobre un elemento.");
            }
            if (previo == cabeza) {
                cabeza = actual;
                if (cabeza == null) {
                    cola = null;
                }
            } else {
                Nodo<T> aux = cabeza;
                while (aux.siguiente != previo) {
                    aux = aux.siguiente;
                }
                aux.siguiente = actual;
                if (previo == cola) {
                    cola = aux;
                }
            }
            tamano--;
            previo = null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
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
