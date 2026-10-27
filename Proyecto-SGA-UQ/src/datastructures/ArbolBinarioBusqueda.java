package datastructures;

import exceptions.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Árbol binario de búsqueda genérico propio del SGA.
 *
 * <p>Estructura obligatoria para:</p>
 * <ul>
 *   <li>Catálogo de apartamentos por <b>código</b> (orden natural de
 *       {@code Apartamento}).</li>
 *   <li>Catálogo de huéspedes por <b>documento</b> (orden natural de {@code Huesped}).</li>
 *   <li>Reservas de cada apartamento ordenadas por <b>fecha de entrada</b> (orden
 *       natural de {@code Reserva}), lo que permite detectar solapamientos recorriendo
 *       el árbol de forma recursiva.</li>
 * </ul>
 *
 * <p>Todas las operaciones (inserción, búsqueda, eliminación, altura y recorridos)
 * se implementan de manera <b>recursiva</b>. El recorrido por defecto es in-orden,
 * que devuelve los elementos ya ordenados por su clave natural.</p>
 *
 * @param <T> tipo de los elementos, que deben implementar {@link Comparable}
 */
public class ArbolBinarioBusqueda<T extends Comparable<T>> implements Iterable<T> {

    /** Nodo del árbol binario de búsqueda. */
    public static class Nodo<T> {
        private T clave;
        private Nodo<T> izquierdo;
        private Nodo<T> derecho;

        Nodo(T clave) {
            this.clave = clave;
            this.izquierdo = null;
            this.derecho = null;
        }

        public T getClave() {
            return clave;
        }

        public Nodo<T> getIzquierdo() {
            return izquierdo;
        }

        public Nodo<T> getDerecho() {
            return derecho;
        }
    }

    private Nodo<T> raiz;
    private int tamano;

    public ArbolBinarioBusqueda() {
        this.raiz = null;
        this.tamano = 0;
    }

    /**
     * Inserta un elemento. Si la clave ya existe, se conserva el valor existente
     * y el árbol no cambia (no se admiten duplicados).
     *
     * @return {@code true} si el elemento fue insertado
     */
    public boolean insertar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("No se puede insertar un elemento nulo en el árbol.");
        }
        if (raiz == null) {
            raiz = new Nodo<>(elemento);
            tamano++;
            return true;
        }
        boolean[] insertado = new boolean[1];
        raiz = insertarRecursivo(raiz, elemento, insertado);
        if (insertado[0]) {
            tamano++;
        }
        return insertado[0];
    }

    private Nodo<T> insertarRecursivo(Nodo<T> nodo, T elemento, boolean[] insertado) {
        if (nodo == null) {
            insertado[0] = true;
            return new Nodo<>(elemento);
        }
        int comparacion = elemento.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = insertarRecursivo(nodo.izquierdo, elemento, insertado);
        } else if (comparacion > 0) {
            nodo.derecho = insertarRecursivo(nodo.derecho, elemento, insertado);
        }
        return nodo;
    }

    /**
     * Busca un elemento por su clave.
     *
     * @return el elemento encontrado o {@code null} si no existe
     */
    public T buscar(T clave) {
        return buscarRecursivo(raiz, clave);
    }

    private T buscarRecursivo(Nodo<T> nodo, T clave) {
        if (nodo == null) {
            return null;
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion == 0) {
            return nodo.clave;
        }
        if (comparacion < 0) {
            return buscarRecursivo(nodo.izquierdo, clave);
        }
        return buscarRecursivo(nodo.derecho, clave);
    }

    /** {@code true} si el árbol contiene el elemento. */
    public boolean contiene(T clave) {
        return buscar(clave) != null;
    }

    /**
     * Elimina un elemento del árbol.
     *
     * @return {@code true} si el elemento existía y fue eliminado
     */
    public boolean eliminar(T clave) {
        if (raiz == null) {
            return false;
        }
        boolean[] eliminado = new boolean[1];
        raiz = eliminarRecursivo(raiz, clave, eliminado);
        if (eliminado[0]) {
            tamano--;
        }
        return eliminado[0];
    }

    private Nodo<T> eliminarRecursivo(Nodo<T> nodo, T clave, boolean[] eliminado) {
        if (nodo == null) {
            return null;
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = eliminarRecursivo(nodo.izquierdo, clave, eliminado);
        } else if (comparacion > 0) {
            nodo.derecho = eliminarRecursivo(nodo.derecho, clave, eliminado);
        } else {
            eliminado[0] = true;
            if (nodo.izquierdo == null) {
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                return nodo.izquierdo;
            }
            // El nodo tiene dos hijos: se sustituye por su sucesor in-orden.
            Nodo<T> sucesor = minimo(nodo.derecho);
            nodo.clave = sucesor.clave;
            boolean[] auxiliar = new boolean[1];
            nodo.derecho = eliminarRecursivo(nodo.derecho, sucesor.clave, auxiliar);
        }
        return nodo;
    }

    /**
     * Devuelve el elemento más pequeño del árbol.
     *
     * @throws EstructuraVaciaException si el árbol está vacío
     */
    public T minimo() {
        if (raiz == null) {
            throw new EstructuraVaciaException("Árbol binario de búsqueda", "obtener el mínimo");
        }
        return minimo(raiz).clave;
    }

    private Nodo<T> minimo(Nodo<T> nodo) {
        if (nodo.izquierdo == null) {
            return nodo;
        }
        return minimo(nodo.izquierdo);
    }

    /**
     * Devuelve el elemento más grande del árbol.
     *
     * @throws EstructuraVaciaException si el árbol está vacío
     */
    public T maximo() {
        if (raiz == null) {
            throw new EstructuraVaciaException("Árbol binario de búsqueda", "obtener el máximo");
        }
        Nodo<T> nodo = raiz;
        while (nodo.derecho != null) {
            nodo = nodo.derecho;
        }
        return nodo.clave;
    }

    /**
     * Altura del árbol (número de niveles). Se calcula de forma recursiva.
     */
    public int altura() {
        return alturaRecursiva(raiz);
    }

    private int alturaRecursiva(Nodo<T> nodo) {
        if (nodo == null) {
            return 0;
        }
        int izq = alturaRecursiva(nodo.izquierdo);
        int der = alturaRecursiva(nodo.derecho);
        return 1 + Math.max(izq, der);
    }

    /** Número de elementos almacenados. */
    public int tamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }

    public void limpiar() {
        raiz = null;
        tamano = 0;
    }

    /** Acceso a la raíz para recorridos externos (por ejemplo, detección de solapamientos). */
    public Nodo<T> getRaiz() {
        return raiz;
    }

    /** Recorrido in-orden: devuelve los elementos ordenados de forma ascendente. */
    public ListaEnlazada<T> recorridoInOrden() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        inOrden(raiz, resultado);
        return resultado;
    }

    private void inOrden(Nodo<T> nodo, ListaEnlazada<T> resultado) {
        if (nodo == null) {
            return;
        }
        inOrden(nodo.izquierdo, resultado);
        resultado.agregar(nodo.clave);
        inOrden(nodo.derecho, resultado);
    }

    /** Recorrido pre-orden: raíz, subárbol izquierdo, subárbol derecho. */
    public ListaEnlazada<T> recorridoPreOrden() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        preOrden(raiz, resultado);
        return resultado;
    }

    private void preOrden(Nodo<T> nodo, ListaEnlazada<T> resultado) {
        if (nodo == null) {
            return;
        }
        resultado.agregar(nodo.clave);
        preOrden(nodo.izquierdo, resultado);
        preOrden(nodo.derecho, resultado);
    }

    /** Recorrido post-orden: subárbol izquierdo, subárbol derecho, raíz. */
    public ListaEnlazada<T> recorridoPostOrden() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        postOrden(raiz, resultado);
        return resultado;
    }

    private void postOrden(Nodo<T> nodo, ListaEnlazada<T> resultado) {
        if (nodo == null) {
            return;
        }
        postOrden(nodo.izquierdo, resultado);
        postOrden(nodo.derecho, resultado);
        resultado.agregar(nodo.clave);
    }

    /** Recorrido por niveles (amplitud), usando la cola propia del SGA. */
    public ListaEnlazada<T> recorridoPorNivel() {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        if (raiz == null) {
            return resultado;
        }
        Cola<Nodo<T>> pendientes = new Cola<>();
        pendientes.encolar(raiz);
        while (!pendientes.estaVacia()) {
            Nodo<T> nodo = pendientes.desencolar();
            resultado.agregar(nodo.clave);
            if (nodo.izquierdo != null) {
                pendientes.encolar(nodo.izquierdo);
            }
            if (nodo.derecho != null) {
                pendientes.encolar(nodo.derecho);
            }
        }
        return resultado;
    }

    /**
     * Busca de forma recursiva un elemento que cumple una condición, poda las ramas
     * cuyo rango de claves no puede contener coincidencias.
     *
     * @param desde     límite inferior inclusivo del rango de búsqueda
     * @param hasta     límite superior inclusivo del rango de búsqueda
     * @param condicion condición que debe cumplir el elemento buscado
     * @return el primer elemento que cumple la condición o {@code null}
     */
    public T buscarEnRango(T desde, T hasta, Condicion<T> condicion) {
        return buscarEnRango(raiz, desde, hasta, condicion);
    }

    private T buscarEnRango(Nodo<T> nodo, T desde, T hasta, Condicion<T> condicion) {
        if (nodo == null) {
            return null;
        }
        if (nodo.clave.compareTo(desde) < 0) {
            return buscarEnRango(nodo.derecho, desde, hasta, condicion);
        }
        if (nodo.clave.compareTo(hasta) > 0) {
            return buscarEnRango(nodo.izquierdo, desde, hasta, condicion);
        }
        if (condicion.cumple(nodo.clave)) {
            return nodo.clave;
        }
        T encontrado = buscarEnRango(nodo.izquierdo, desde, hasta, condicion);
        if (encontrado != null) {
            return encontrado;
        }
        return buscarEnRango(nodo.derecho, desde, hasta, condicion);
    }

    /**
     * Condición booleana evaluada sobre un elemento del árbol.
     *
     * @param <T> tipo del elemento
     */
    public interface Condicion<T> {
        boolean cumple(T elemento);
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorArbol();
    }

    /**
     * Iterador propio del árbol: recorre en orden in-orden (ascendente).
     */
    public class IteradorArbol implements Iterator<T> {

        private final ListaEnlazada<T> elementos;
        private int posicion;

        public IteradorArbol() {
            this.elementos = recorridoInOrden();
            this.posicion = 0;
        }

        @Override
        public boolean hasNext() {
            return posicion < elementos.tamano();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No hay más elementos en el árbol.");
            }
            return elementos.obtener(posicion++);
        }
    }

    @Override
    public String toString() {
        return "ArbolBinarioBusqueda" + recorridoInOrden();
    }
}
