package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista doblemente enlazada genérica propia del SGA.
 *
 * <p>Es la estructura obligatoria para el historial de estancias de cada huésped, la
 * lista de ocupantes de una reserva y el desglose de noches liquidadas.</p>
 *
 * <p>Cada nodo guarda la referencia al anterior y al siguiente, lo que permite
 * recorridos en ambos sentidos y eliminación de un nodo en tiempo constante cuando
 * se conoce su referencia.</p>
 *
 * @param <T> tipo de los elementos almacenados
 */
public class ListaDobleEnlazada<T> implements Iterable<T> {

    /** Nodo de la lista doblemente enlazada. */
    public static class NodoDoble<T> {
        private T valor;
        private NodoDoble<T> anterior;
        private NodoDoble<T> siguiente;

        NodoDoble(T valor) {
            this.valor = valor;
            this.anterior = null;
            this.siguiente = null;
        }

        public T getValor() {
            return valor;
        }

        public NodoDoble<T> getAnterior() {
            return anterior;
        }

        public NodoDoble<T> getSiguiente() {
            return siguiente;
        }
    }

    private NodoDoble<T> cabeza;
    private NodoDoble<T> cola;
    private int tamano;

    public ListaDobleEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    /** Agrega un elemento al final de la lista. */
    public void agregar(T valor) {
        NodoDoble<T> nuevo = new NodoDoble<>(valor);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            nuevo.anterior = cola;
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    /** Agrega un elemento al inicio de la lista. */
    public void agregarAlInicio(T valor) {
        NodoDoble<T> nuevo = new NodoDoble<>(valor);
        nuevo.siguiente = cabeza;
        if (cabeza != null) {
            cabeza.anterior = nuevo;
        }
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
        } else if (indice == 0) {
            agregarAlInicio(valor);
        } else {
            NodoDoble<T> anterior = nodoEn(indice - 1);
            NodoDoble<T> nuevo = new NodoDoble<>(valor);
            nuevo.anterior = anterior;
            nuevo.siguiente = anterior.siguiente;
            anterior.siguiente.anterior = nuevo;
            anterior.siguiente = nuevo;
            tamano++;
        }
    }

    /** Devuelve el elemento que ocupa la posición indicada. */
    public T obtener(int indice) {
        return nodoEn(indice).valor;
    }

    public T primero() {
        return cabeza == null ? null : cabeza.valor;
    }

    public T ultimo() {
        return cola == null ? null : cola.valor;
    }

    /** Elimina y devuelve el elemento que ocupa la posición indicada. */
    public T remover(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + tamano);
        }
        if (indice == 0) {
            T valor = cabeza.valor;
            cabeza = cabeza.siguiente;
            if (cabeza != null) {
                cabeza.anterior = null;
            } else {
                cola = null;
            }
            tamano--;
            return valor;
        }
        if (indice == tamano - 1) {
            T valor = cola.valor;
            cola = cola.anterior;
            cola.siguiente = null;
            tamano--;
            return valor;
        }
        NodoDoble<T> objetivo = nodoEn(indice);
        objetivo.anterior.siguiente = objetivo.siguiente;
        objetivo.siguiente.anterior = objetivo.anterior;
        T valor = objetivo.valor;
        tamano--;
        return valor;
    }

    /** Elimina la primera aparición del valor indicado. */
    public boolean removerValor(T valor) {
        NodoDoble<T> actual = cabeza;
        while (actual != null) {
            if (actual.valor == null ? valor == null : actual.valor.equals(valor)) {
                if (actual == cabeza) {
                    cabeza = actual.siguiente;
                    if (cabeza != null) {
                        cabeza.anterior = null;
                    } else {
                        cola = null;
                    }
                } else if (actual == cola) {
                    cola = actual.anterior;
                    cola.siguiente = null;
                } else {
                    actual.anterior.siguiente = actual.siguiente;
                    actual.siguiente.anterior = actual.anterior;
                }
                tamano--;
                return true;
            }
            actual = actual.siguiente;
        }
        return false;
    }

    public boolean contiene(T valor) {
        return indiceDe(valor) >= 0;
    }

    public int indiceDe(T valor) {
        int i = 0;
        for (NodoDoble<T> n = cabeza; n != null; n = n.siguiente) {
            if (n.valor == null ? valor == null : n.valor.equals(valor)) {
                return i;
            }
            i++;
        }
        return -1;
    }

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

    /** Invierte la lista usando recursión sobre los nodos. */
    public void invertir() {
        NodoDoble<T> anterior = null;
        NodoDoble<T> actual = cabeza;
        invertirRecursivo(actual, anterior);
        cabeza = cola;
        cola = actual;
    }

    private void invertirRecursivo(NodoDoble<T> actual, NodoDoble<T> anterior) {
        if (actual == null) {
            return;
        }
        NodoDoble<T> siguiente = actual.siguiente;
        actual.siguiente = anterior;
        actual.anterior = siguiente;
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

    private NodoDoble<T> nodoEn(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException(
                    "Índice " + indice + " fuera de rango para una lista de tamaño " + tamano);
        }
        // Se recorre desde el extremo más cercano para aprovechar el doble enlace.
        if (indice < tamano / 2) {
            NodoDoble<T> actual = cabeza;
            for (int i = 0; i < indice; i++) {
                actual = actual.siguiente;
            }
            return actual;
        }
        NodoDoble<T> actual = cola;
        for (int i = tamano - 1; i > indice; i--) {
            actual = actual.anterior;
        }
        return actual;
    }

    /** Copia el contenido de la lista en un arreglo propio. */
    public Object[] aArreglo() {
        Object[] datos = new Object[tamano];
        int i = 0;
        for (NodoDoble<T> n = cabeza; n != null; n = n.siguiente) {
            datos[i++] = n.valor;
        }
        return datos;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorDoble();
    }

    /** Iterador en sentido inverso (del último al primero). */
    public Iterator<T> iteradorInverso() {
        return new IteradorInverso();
    }

    /**
     * Iterador propio de la lista doblemente enlazada (sentido natural).
     */
    public class IteradorDoble implements Iterator<T> {

        private NodoDoble<T> actual;

        public IteradorDoble() {
            this.actual = cabeza;
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
            actual = actual.siguiente;
            return valor;
        }
    }

    /**
     * Iterador propio de la lista doblemente enlazada (sentido inverso).
     */
    public class IteradorInverso implements Iterator<T> {

        private NodoDoble<T> actual;

        public IteradorInverso() {
            this.actual = cola;
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
            actual = actual.anterior;
            return valor;
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
