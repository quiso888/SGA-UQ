package datastructures;

import exceptions.EstructuraVaciaException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola de prioridad genérica propia del SGA, implementada como montículo binario
 * (heap) sobre un arreglo propio.
 *
 * <p>Dos usos obligatorios en el sistema:</p>
 * <ul>
 *   <li><b>Cola de preparación inteligente:</b> los apartamentos que quedan
 *       <i>Pendiente de preparación</i> entran con prioridad 1 si tienen entrada
 *       programada para hoy (desempate por hora estimada de llegada) y con prioridad 2
 *       si no la tienen.</li>
 *   <li><b>Reservas pendientes por vencimiento:</b> las reservas en estado
 *       <i>Pendiente</i> se organizan por su hora de vencimiento (24 horas después de
 *       creadas) para poder cancelarlas automáticamente al vencer el plazo.</li>
 * </ul>
 *
 * <p>El orden de salida es: primero por {@code prioridad} ascendente (menor = más
 * urgente) y, en caso de empate, por el {@link Comparator} de desempate proporcionado
 * al constructor. Esto permite combinar comparadores con
 * {@code Comparator.thenComparing}.</p>
 *
 * @param <T> tipo de los elementos encolados
 */
public class ColaPrioridad<T> implements Iterable<T> {

    private static final int CAPACIDAD_INICIAL = 8;

    /** Elemento de la cola: guarda el dato, su prioridad y su posición en el heap. */
    private static class Elemento<T> {
        private final T valor;
        private final long prioridad;

        Elemento(T valor, long prioridad) {
            this.valor = valor;
            this.prioridad = prioridad;
        }
    }

    private Elemento<T>[] heap;
    private int cantidad;
    private final Comparator<T> desempate;

    @SuppressWarnings("unchecked")
    public ColaPrioridad() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public ColaPrioridad(Comparator<T> desempate) {
        this.heap = new Elemento[CAPACIDAD_INICIAL];
        this.cantidad = 0;
        this.desempate = desempate;
    }

    /**
     * Encola un elemento con la prioridad indicada. Menor valor = mayor prioridad.
     */
    public void encolar(T valor, long prioridad) {
        ensureCapacity(cantidad + 1);
        heap[cantidad] = new Elemento<>(valor, prioridad);
        cantidad++;
        // Sube en el montículo manteniendo la propiedad de heap.
        siftUp(cantidad - 1);
    }

    /** Encola un elemento con prioridad 1 (usada para la cola de preparación). */
    public void encolar(T valor) {
        encolar(valor, 1L);
    }

    /**
     * Elimina de la cola el elemento indicado (por identidad o por el comparador
     * de desempate).
     *
     * <p>La eliminación en un montículo se hace sustituyendo el elemento por el
     * último y reubicándolo hacia arriba o hacia abajo según corresponda.</p>
     *
     * @return {@code true} si el elemento estaba en la cola
     */
    public boolean remover(T valor) {
        if (valor == null) {
            return false;
        }
        for (int i = 0; i < cantidad; i++) {
            Elemento<T> elemento = heap[i];
            if (elemento.valor == valor || elemento.valor.equals(valor)
                    || comparar(elemento, new Elemento<>(valor, elemento.prioridad)) == 0) {
                eliminarEnIndice(i);
                return true;
            }
        }
        return false;
    }

    private void eliminarEnIndice(int indice) {
        cantidad--;
        if (indice == cantidad) {
            heap[cantidad] = null;
            return;
        }
        heap[indice] = heap[cantidad];
        heap[cantidad] = null;
        siftUp(indice);
        siftDown(indice);
    }

    /** Sube el elemento de la posición indicada hasta restaurar la propiedad de heap. */
    private void siftUp(int indice) {
        int hijo = indice;
        while (hijo > 0) {
            int padre = (hijo - 1) / 2;
            if (comparar(heap[hijo], heap[padre]) < 0) {
                Elemento<T> temp = heap[hijo];
                heap[hijo] = heap[padre];
                heap[padre] = temp;
                hijo = padre;
            } else {
                break;
            }
        }
    }

    /**
     * Desencola y devuelve el elemento de mayor prioridad.
     *
     * @throws EstructuraVaciaException si la cola está vacía
     */
    public T desencolar() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Cola de prioridad", "desencolar");
        }
        T resultado = heap[0].valor;
        cantidad--;
        heap[0] = heap[cantidad];
        heap[cantidad] = null;
        siftDown(0);
        return resultado;
    }

    /** Devuelve el elemento de mayor prioridad sin retirarlo. */
    public T verFrente() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Cola de prioridad", "consultar el frente");
        }
        return heap[0].valor;
    }

    /** Devuelve la prioridad del elemento de mayor prioridad. */
    public long prioridadFrente() {
        if (cantidad == 0) {
            throw new EstructuraVaciaException("Cola de prioridad", "consultar el frente");
        }
        return heap[0].prioridad;
    }

    public boolean estaVacia() {
        return cantidad == 0;
    }

    public int tamano() {
        return cantidad;
    }

    public void limpiar() {
        for (int i = 0; i < cantidad; i++) {
            heap[i] = null;
        }
        cantidad = 0;
    }

    /** Devuelve el contenido ordenado por prioridad (sin modificar la cola). */
    public Object[] aArregloOrdenado() {
        Elemento<T>[] copia = new Elemento[cantidad];
        for (int i = 0; i < cantidad; i++) {
            copia[i] = heap[i];
        }
        // Ordenamiento por inserción sobre el arreglo (estable y sin java.util).
        for (int i = 1; i < copia.length; i++) {
            Elemento<T> actual = copia[i];
            int j = i - 1;
            while (j >= 0 && comparar(actual, copia[j]) < 0) {
                copia[j + 1] = copia[j];
                j--;
            }
            copia[j + 1] = actual;
        }
        Object[] datos = new Object[cantidad];
        for (int i = 0; i < cantidad; i++) {
            datos[i] = copia[i].valor;
        }
        return datos;
    }

    /**
     * Compara dos elementos: primero por prioridad y luego por el comparador de desempate.
     */
    private int comparar(Elemento<T> a, Elemento<T> b) {
        if (a.prioridad != b.prioridad) {
            return a.prioridad < b.prioridad ? -1 : 1;
        }
        if (desempate == null) {
            return 0;
        }
        return desempate.compare(a.valor, b.valor);
    }

    /** Baja en el montículo un elemento que viola la propiedad de heap. */
    private void siftDown(int indice) {
        int padre = indice;
        while (true) {
            int hijoIzq = 2 * padre + 1;
            int hijoDer = 2 * padre + 2;
            int menor = padre;
            if (hijoIzq < cantidad && comparar(heap[hijoIzq], heap[menor]) < 0) {
                menor = hijoIzq;
            }
            if (hijoDer < cantidad && comparar(heap[hijoDer], heap[menor]) < 0) {
                menor = hijoDer;
            }
            if (menor == padre) {
                break;
            }
            Elemento<T> temp = heap[padre];
            heap[padre] = heap[menor];
            heap[menor] = temp;
            padre = menor;
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity(int requerida) {
        if (requerida <= heap.length) {
            return;
        }
        Elemento<T>[] nuevos = new Elemento[Math.max(requerida, heap.length * 2)];
        for (int i = 0; i < cantidad; i++) {
            nuevos[i] = heap[i];
        }
        heap = nuevos;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteradorPrioridad();
    }

    /**
     * Iterador propio de la cola de prioridad: recorre en el orden de salida
     * (mayor prioridad primero) sin alterar la cola.
     */
    public class IteradorPrioridad implements Iterator<T> {

        private final Object[] orden;
        private int posicion;

        public IteradorPrioridad() {
            this.orden = aArregloOrdenado();
            this.posicion = 0;
        }

        @Override
        public boolean hasNext() {
            return posicion < orden.length;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No hay más elementos en la cola de prioridad.");
            }
            return (T) orden[posicion++];
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ColaPrioridad[");
        Object[] datos = aArregloOrdenado();
        for (int i = 0; i < datos.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(datos[i]);
        }
        return sb.append(']').toString();
    }
}
