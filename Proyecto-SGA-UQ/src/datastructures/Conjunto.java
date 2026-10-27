package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Conjunto genérico propio del SGA: colección sin duplicados.
 *
 * <p>Se usa para los apartamentos favoritos de cada huésped (sin repetidos) y para el
 * catálogo de códigos de apartamento registrados.</p>
 *
 * <p>Implementación por Separate Chaining sobre un arreglo propio de cubos: cada
 * cubo es una lista simplemente enlazada de {@link NodoConjunto}. No utiliza ninguna
 * colección de {@code java.util}.</p>
 *
 * @param <T> tipo de los elementos del conjunto
 */
public class Conjunto<T> implements Iterable<T> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA = 0.75;

    /** Nodo de una cadena de cubos. */
    private static class NodoConjunto<T> {
        private final T valor;
        private final int hash;
        private NodoConjunto<T> siguiente;

        NodoConjunto(T valor, int hash) {
            this.valor = valor;
            this.hash = hash;
            this.siguiente = null;
        }
    }

    private NodoConjunto<T>[] cubos;
    private int capacidad;
    private int cantidad;

    @SuppressWarnings("unchecked")
    public Conjunto() {
        this.cubos = new NodoConjunto[CAPACIDAD_INICIAL];
        this.capacidad = CAPACIDAD_INICIAL;
        this.cantidad = 0;
    }

    /**
     * Agrega un elemento al conjunto.
     *
     * @return {@code true} si el elemento no estaba y fue agregado
     */
    public boolean agregar(T valor) {
        if (cantidad + 1 > (int) (capacidad * FACTOR_CARGA)) {
            redimensionar();
        }
        int hash = dispersar(valor);
        int indice = indiceBucket(hash);
        NodoConjunto<T> actual = cubos[indice];
        while (actual != null) {
            if (actual.hash == hash && iguales(actual.valor, valor)) {
                return false;
            }
            actual = actual.siguiente;
        }
        NodoConjunto<T> nuevo = new NodoConjunto<>(valor, hash);
        nuevo.siguiente = cubos[indice];
        cubos[indice] = nuevo;
        cantidad++;
        return true;
    }

    /**
     * Elimina un elemento del conjunto.
     *
     * @return {@code true} si el elemento estaba y fue eliminado
     */
    public boolean remover(T valor) {
        int hash = dispersar(valor);
        int indice = indiceBucket(hash);
        NodoConjunto<T> actual = cubos[indice];
        NodoConjunto<T> previo = null;
        while (actual != null) {
            if (actual.hash == hash && iguales(actual.valor, valor)) {
                if (previo == null) {
                    cubos[indice] = actual.siguiente;
                } else {
                    previo.siguiente = actual.siguiente;
                }
                cantidad--;
                return true;
            }
            previo = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    /** {@code true} si el elemento pertenece al conjunto. */
    public boolean contiene(T valor) {
        int hash = dispersar(valor);
        NodoConjunto<T> actual = cubos[indiceBucket(hash)];
        while (actual != null) {
            if (actual.hash == hash && iguales(actual.valor, valor)) {
                return true;
            }
            actual = actual.siguiente;
        }
        return false;
    }

    public int tamano() {
        return cantidad;
    }

    public boolean estaVacia() {
        return cantidad == 0;
    }

    public void limpiar() {
        for (int i = 0; i < capacidad; i++) {
            cubos[i] = null;
        }
        cantidad = 0;
    }

    /** Copia el contenido del conjunto en un arreglo propio. */
    public Object[] aArreglo() {
        Object[] datos = new Object[cantidad];
        int i = 0;
        for (int c = 0; c < capacidad; c++) {
            NodoConjunto<T> actual = cubos[c];
            while (actual != null) {
                datos[i++] = actual.valor;
                actual = actual.siguiente;
            }
        }
        return datos;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        NodoConjunto<T>[] anteriores = cubos;
        capacidad = capacidad * 2;
        cubos = new NodoConjunto[capacidad];
        cantidad = 0;
        for (NodoConjunto<T> cubo : anteriores) {
            NodoConjunto<T> actual = cubo;
            while (actual != null) {
                NodoConjunto<T> siguiente = actual.siguiente;
                int indice = indiceBucket(actual.hash);
                actual.siguiente = cubos[indice];
                cubos[indice] = actual;
                cantidad++;
                actual = siguiente;
            }
        }
    }

    private int indiceBucket(int hash) {
        // Dispersión: convierte la(hash) en un índice no negativo.
        return (hash & 0x7fffffff) % capacidad;
    }

    private static int dispersar(Object valor) {
        if (valor == null) {
            return 0;
        }
        return valor.hashCode();
    }

    private static boolean iguales(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorConjunto();
    }

    /**
     * Iterador propio del conjunto.
     */
    public class IteradorConjunto implements Iterator<T> {

        private int cuboActual;
        private NodoConjunto<T> nodoActual;

        public IteradorConjunto() {
            this.cuboActual = 0;
            this.nodoActual = cubos.length > 0 ? cubos[0] : null;
            while (nodoActual == null && cuboActual < cubos.length - 1) {
                cuboActual++;
                nodoActual = cubos[cuboActual];
            }
        }

        @Override
        public boolean hasNext() {
            return nodoActual != null;
        }

        @Override
        public T next() {
            if (nodoActual == null) {
                throw new NoSuchElementException("No hay más elementos en el conjunto.");
            }
            T valor = nodoActual.valor;
            nodoActual = nodoActual.siguiente;
            while (nodoActual == null && cuboActual < cubos.length - 1) {
                cuboActual++;
                nodoActual = cubos[cuboActual];
            }
            return valor;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Conjunto{");
        boolean primero = true;
        for (T valor : this) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(valor);
            primero = false;
        }
        return sb.append('}').toString();
    }
}
