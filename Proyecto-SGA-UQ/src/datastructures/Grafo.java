package datastructures;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Grafo genérico propio del SGA que representa el mapa del alojamiento.
 *
 * Modelo del edificio:
 * Nodos:apartamentos, recepción, escaleras y cuartos de aseo.
 * Aristas:pasillos con un peso (metros o segundos que tarda el
 *       recorrido) que permite calcular rutas.
 *
 *
 * Algoritmos implementados:
 *
 *   Dijkstra (búsqueda del camino más corto desde cualquier nodo, con
 *       montículo propio { ColaPrioridad} como cola de prioridad).
 *   BFS(recorrido en anchura) para alcance y agrupación por bloque.
 *   DFS (recorrido en profundidad, recursivo) para analizar la
 *       conectividad y detectar ciclos.
 *
 *
 * @param <T> tipo de la etiqueta que identifica cada nodo del grafo
 */
public class Grafo<T> implements Iterable<Grafo.Nodo<T>> {

    /** Nodo del grafo. */
    public static class Nodo<T> {
        private final T etiqueta;
        private double x;
        private double y;
        private final int indice;
        private final ListaEnlazada<Arista<T>> adyacentes = new ListaEnlazada<>();

        Nodo(T etiqueta, int indice, double x, double y) {
            this.etiqueta = etiqueta;
            this.indice = indice;
            this.x = x;
            this.y = y;
        }

        public T getEtiqueta() {
            return etiqueta;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void setPosicion(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public int getIndice() {
            return indice;
        }

        public ListaEnlazada<Arista<T>> getAristas() {
            return adyacentes;
        }

        @Override
        public String toString() {
            return String.valueOf(etiqueta);
        }
    }

    /** Arista dirigida con peso. */
    public static class Arista<T> {
        private final Nodo<T> origen;
        private final Nodo<T> destino;
        private final int peso;
        private boolean activa;

        Arista(Nodo<T> origen, Nodo<T> destino, int peso) {
            this.origen = origen;
            this.destino = destino;
            this.peso = peso;
            this.activa = true;
        }

        public Nodo<T> getOrigen() {
            return origen;
        }

        public Nodo<T> getDestino() {
            return destino;
        }

        public int getPeso() {
            return peso;
        }

        public boolean isActiva() {
            return activa;
        }

        public void setActiva(boolean activa) {
            this.activa = activa;
        }

        @Override
        public String toString() {
            return origen + " -> " + destino + " (" + peso + ")";
        }
    }

    /** Resultado de una búsqueda de camino. */
    public static class Ruta<T> {
        private final boolean alcanzable;
        private final int distancia;
        private final ListaEnlazada<Nodo<T>> camino;

        Ruta(boolean alcanzable, int distancia, ListaEnlazada<Nodo<T>> camino) {
            this.alcanzable = alcanzable;
            this.distancia = distancia;
            this.camino = camino;
        }

        public boolean esAlcanzable() {
            return alcanzable;
        }

        public int getDistancia() {
            return distancia;
        }

        public ListaEnlazada<Nodo<T>> getCamino() {
            return camino;
        }

        /** Devuelve las etiquetas de los nodos del camino. */
        public ListaEnlazada<T> getEtiquetas() {
            ListaEnlazada<T> etiquetas = new ListaEnlazada<>();
            for (Nodo<T> nodo : camino) {
                etiquetas.agregar(nodo.getEtiqueta());
            }
            return etiquetas;
        }
    }

    /** Par (nodo, distancia) usado por la cola de prioridad de Dijkstra. */
    private static class Par implements Comparable<Par> {
        private final int indice;
        private final int distancia;

        Par(int indice, int distancia) {
            this.indice = indice;
            this.distancia = distancia;
        }

        @Override
        public int compareTo(Par otro) {
            if (this.distancia != otro.distancia) {
                return Integer.compare(this.distancia, otro.distancia);
            }
            return Integer.compare(this.indice, otro.indice);
        }
    }

    /** Marcador de nodos visitados, respaldado por un arreglo propio. */
    private static class Marcados {
        private final boolean[] visitados;

        Marcados(int cantidad) {
            this.visitados = new boolean[Math.max(1, cantidad)];
        }

        void marcar(int indice) {
            if (indice >= 0 && indice < visitados.length) {
                visitados[indice] = true;
            }
        }

        void desmarcar(int indice) {
            if (indice >= 0 && indice < visitados.length) {
                visitados[indice] = false;
            }
        }

        boolean estaMarcado(int indice) {
            return indice >= 0 && indice < visitados.length && visitados[indice];
        }
    }

    private static final int INFINITO = Integer.MAX_VALUE;

    private final ListaEnlazada<Nodo<T>> nodos = new ListaEnlazada<>();
    private final ListaEnlazada<Arista<T>> aristas = new ListaEnlazada<>();

    /**
     * Agrega un nodo al grafo.
     *
     * @return el nodo creado
     */
    public Nodo<T> agregarNodo(T etiqueta, double x, double y) {
        Nodo<T> nodo = new Nodo<>(etiqueta, nodos.tamano(), x, y);
        nodos.agregar(nodo);
        return nodo;
    }

    /** Agrega un nodo al grafo en la posición (0, 0). */
    public Nodo<T> agregarNodo(T etiqueta) {
        return agregarNodo(etiqueta, 0, 0);
    }

    /**
     * Agrega una arista no dirigida entre dos nodos ya existentes.
     *
     * @return la arista creada
     */
    public Arista<T> agregarArista(T origen, T destino, int peso) {
        Nodo<T> nodoOrigen = buscarNodo(origen);
        Nodo<T> nodoDestino = buscarNodo(destino);
        if (nodoOrigen == null || nodoDestino == null) {
            throw new IllegalArgumentException(
                    "No se puede crear la arista: alguno de los nodos no existe en el grafo.");
        }
        Arista<T> arista = new Arista<>(nodoOrigen, nodoDestino, peso);
        aristas.agregar(arista);
        nodoOrigen.getAristas().agregar(arista);
        nodoDestino.getAristas().agregar(arista);
        return arista;
    }

    /**
     * Agrega una arista dirigida (el pasillo se puede recorrer en un solo sentido).
     */
    public Arista<T> agregarAristaDirigida(T origen, T destino, int peso) {
        Nodo<T> nodoOrigen = buscarNodo(origen);
        Nodo<T> nodoDestino = buscarNodo(destino);
        if (nodoOrigen == null || nodoDestino == null) {
            throw new IllegalArgumentException(
                    "No se puede crear la arista: alguno de los nodos no existe en el grafo.");
        }
        Arista<T> arista = new Arista<>(nodoOrigen, nodoDestino, peso);
        aristas.agregar(arista);
        nodoOrigen.getAristas().agregar(arista);
        return arista;
    }

    /** Busca un nodo por su etiqueta. */
    public Nodo<T> buscarNodo(T etiqueta) {
        for (Nodo<T> nodo : nodos) {
            if (nodo.getEtiqueta() == null ? etiqueta == null : nodo.getEtiqueta().equals(etiqueta)) {
                return nodo;
            }
        }
        return null;
    }

    /** Busca la arista que une dos nodos, sin importar el sentido. */
    public Arista<T> buscarArista(T origen, T destino) {
        Nodo<T> nodoOrigen = buscarNodo(origen);
        Nodo<T> nodoDestino = buscarNodo(destino);
        if (nodoOrigen == null || nodoDestino == null) {
            return null;
        }
        for (Arista<T> arista : nodoOrigen.getAristas()) {
            if (arista.getDestino() == nodoDestino || arista.getOrigen() == nodoDestino) {
                return arista;
            }
        }
        return null;
    }

    /**
     * Cierra un pasillo: la arista deja de usarse en los recorridos, pero se
     * conserva en el grafo para poder reabrirla.
     */
    public boolean cerrarPasillo(T origen, T destino) {
        Arista<T> arista = buscarArista(origen, destino);
        if (arista == null) {
            return false;
        }
        arista.setActiva(false);
        return true;
    }

    /** Reabre un pasillo previamente cerrado. */
    public boolean abrirPasillo(T origen, T destino) {
        Arista<T> arista = buscarArista(origen, destino);
        if (arista == null) {
            return false;
        }
        arista.setActiva(true);
        return true;
    }

    public boolean eliminarNodo(T etiqueta) {
        Nodo<T> nodo = buscarNodo(etiqueta);
        if (nodo == null) {
            return false;
        }
        for (Arista<T> arista : new ListaEnlazada<>(aristas)) {
            if (arista.getOrigen() == nodo || arista.getDestino() == nodo) {
                eliminarArista(arista);
            }
        }
        return nodos.removerValor(nodo);
    }

    private void eliminarArista(Arista<T> arista) {
        aristas.removerValor(arista);
        arista.getOrigen().getAristas().removerValor(arista);
        if (arista.getOrigen() != arista.getDestino()) {
            arista.getDestino().getAristas().removerValor(arista);
        }
    }

    public int numeroNodos() {
        return nodos.tamano();
    }

    public int numeroAristas() {
        return aristas.tamano();
    }

    public boolean estaVacio() {
        return nodos.estaVacia();
    }

    public ListaEnlazada<Nodo<T>> getNodos() {
        return nodos;
    }

    public ListaEnlazada<Arista<T>> getAristas() {
        return aristas;
    }

    // ------------------------------------------------------------------
    // Dijkstra: camino más corto
    // ------------------------------------------------------------------

    /**
     * Calcula el camino más corto entre dos nodos usando el algoritmo de Dijkstra.
     *
     * @return la ruta con su distancia total; si no hay camino, la ruta llega con
     *         {@link Ruta#esAlcanzable()} en {@code false}
     */
    public Ruta<T> caminoMasCorto(T origen, T destino) {
        Nodo<T> nodoOrigen = buscarNodo(origen);
        Nodo<T> nodoDestino = buscarNodo(destino);
        if (nodoOrigen == null || nodoDestino == null) {
            return new Ruta<>(false, INFINITO, new ListaEnlazada<Nodo<T>>());
        }
        if (nodoOrigen == nodoDestino) {
            ListaEnlazada<Nodo<T>> camino = new ListaEnlazada<>();
            camino.agregar(nodoOrigen);
            return new Ruta<>(true, 0, camino);
        }

        // Distancias y predecesores indexados por el índice del nodo (arreglos propios).
        int[] dist = new int[nodos.tamano()];
        int[] prev = new int[nodos.tamano()];
        boolean[] visitados = new boolean[nodos.tamano()];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = INFINITO;
            prev[i] = -1;
        }
        dist[nodoOrigen.getIndice()] = 0;

        ColaPrioridad<Par> frontera = new ColaPrioridad<>(new Comparator<Par>() {
            @Override
            public int compare(Par a, Par b) {
                return a.compareTo(b);
            }
        });
        frontera.encolar(new Par(nodoOrigen.getIndice(), 0), 0);

        while (!frontera.estaVacia()) {
            Par actual = frontera.desencolar();
            int u = actual.indice;
            if (visitados[u]) {
                continue;
            }
            visitados[u] = true;
            Nodo<T> nodoU = nodoPorIndice(u);
            for (Arista<T> arista : nodoU.getAristas()) {
                if (!arista.isActiva()) {
                    continue;
                }
                Nodo<T> vecino = arista.getDestino() == nodoU ? arista.getOrigen() : arista.getDestino();
                int v = vecino.getIndice();
                int peso = arista.getPeso();
                if (dist[u] + peso < dist[v]) {
                    dist[v] = dist[u] + peso;
                    prev[v] = u;
                    frontera.encolar(new Par(v, dist[v]), dist[v]);
                }
            }
        }

        if (dist[nodoDestino.getIndice()] == INFINITO) {
            return new Ruta<>(false, INFINITO, new ListaEnlazada<Nodo<T>>());
        }

        // Reconstrucción del camino (recursiva).
        ListaEnlazada<Nodo<T>> camino = new ListaEnlazada<>();
        reconstruirCamino(nodoDestino.getIndice(), prev, camino);
        camino.invertir();
        return new Ruta<>(true, dist[nodoDestino.getIndice()], camino);
    }

    private void reconstruirCamino(int indice, int[] prev, ListaEnlazada<Nodo<T>> camino) {
        if (indice < 0) {
            return;
        }
        camino.agregar(nodoPorIndice(indice));
        reconstruirCamino(prev[indice], prev, camino);
    }

    private Nodo<T> nodoPorIndice(int indice) {
        int i = 0;
        for (Nodo<T> nodo : nodos) {
            if (i == indice) {
                return nodo;
            }
            i++;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // BFS y DFS
    // ------------------------------------------------------------------

    /**
     * Recorrido en anchura (BFS) desde un nodo, devolviendo los nodos alcanzables
     * en el orden en que se visiting.
     */
    public ListaEnlazada<Nodo<T>> bfs(T origen) {
        ListaEnlazada<Nodo<T>> alcanzables = new ListaEnlazada<>();
        Nodo<T> inicio = buscarNodo(origen);
        if (inicio == null) {
            return alcanzables;
        }
        Marcados marcados = new Marcados(nodos.tamano());
        Cola<Nodo<T>> pendientes = new Cola<>();
        pendientes.encolar(inicio);
        marcados.marcar(inicio.getIndice());
        while (!pendientes.estaVacia()) {
            Nodo<T> actual = pendientes.desencolar();
            alcanzables.agregar(actual);
            for (Arista<T> arista : actual.getAristas()) {
                if (!arista.isActiva()) {
                    continue;
                }
                Nodo<T> vecino = arista.getDestino() == actual ? arista.getOrigen() : arista.getDestino();
                if (!marcados.estaMarcado(vecino.getIndice())) {
                    marcados.marcar(vecino.getIndice());
                    pendientes.encolar(vecino);
                }
            }
        }
        return alcanzables;
    }

    /**
     * Recorrido en profundidad (DFS) recursivo desde un nodo.
     */
    public ListaEnlazada<Nodo<T>> dfs(T origen) {
        ListaEnlazada<Nodo<T>> visitados = new ListaEnlazada<>();
        Nodo<T> inicio = buscarNodo(origen);
        if (inicio == null) {
            return visitados;
        }
        Marcados marcados = new Marcados(nodos.tamano());
        dfsRecursivo(inicio, marcados, visitados);
        return visitados;
    }

    private void dfsRecursivo(Nodo<T> nodo, Marcados marcados, ListaEnlazada<Nodo<T>> visitados) {
        marcados.marcar(nodo.getIndice());
        visitados.agregar(nodo);
        for (Arista<T> arista : nodo.getAristas()) {
            if (!arista.isActiva()) {
                continue;
            }
            Nodo<T> vecino = arista.getDestino() == nodo ? arista.getOrigen() : arista.getDestino();
            if (!marcados.estaMarcado(vecino.getIndice())) {
                dfsRecursivo(vecino, marcados, visitados);
            }
        }
    }

    /**
     * Devuelve los nodos que <b>no</b> son alcanzables desde el nodo origen, por
     * ejemplo cuando se cierra un pasillo y un apartamento queda aislado.
     */
    public ListaEnlazada<Nodo<T>> inalcanzables(T origen) {
        Marcados alcanzados = new Marcados(nodos.tamano());
        for (Nodo<T> nodo : bfs(origen)) {
            alcanzados.marcar(nodo.getIndice());
        }
        ListaEnlazada<Nodo<T>> resultado = new ListaEnlazada<>();
        for (Nodo<T> nodo : nodos) {
            if (!alcanzados.estaMarcado(nodo.getIndice())) {
                resultado.agregar(nodo);
            }
        }
        return resultado;
    }

    /**
     * Agrupa los nodos en componentes conexas usando BFS. Se usa para agrupar los
     * apartamentos por bloque y detectar zonas aisladas.
     */
    public ListaEnlazada<ListaEnlazada<Nodo<T>>> componentesConexas() {
        Marcados visitados = new Marcados(nodos.tamano());
        ListaEnlazada<ListaEnlazada<Nodo<T>>> grupos = new ListaEnlazada<>();
        for (Nodo<T> nodo : nodos) {
            if (visitados.estaMarcado(nodo.getIndice())) {
                continue;
            }
            Marcados locales = new Marcados(nodos.tamano());
            ListaEnlazada<Nodo<T>> componente = new ListaEnlazada<>();
            bfsComponente(nodo, locales, componente);
            for (Nodo<T> n : componente) {
                visitados.marcar(n.getIndice());
            }
            grupos.agregar(componente);
        }
        return grupos;
    }

    private void bfsComponente(Nodo<T> inicio, Marcados marcados, ListaEnlazada<Nodo<T>> componente) {
        if (marcados.estaMarcado(inicio.getIndice())) {
            return;
        }
        marcados.marcar(inicio.getIndice());
        componente.agregar(inicio);
        for (Arista<T> arista : inicio.getAristas()) {
            if (!arista.isActiva()) {
                continue;
            }
            Nodo<T> vecino = arista.getDestino() == inicio ? arista.getOrigen() : arista.getDestino();
            bfsComponente(vecino, marcados, componente);
        }
    }

    /**
     * Comprueba si el grafo contiene algún ciclo, usando DFS recursivo.
     *
     * <p>Como las aristas del mapa son no dirigidas y se guardan en la lista de
     * adyacencia de ambos extremos, al recorrer se ignora la arista por la que se
     * llegó al nodo: de lo contrario, el simple regreso al padre se confundiría con
     * un ciclo.</p>
     */
    public boolean tieneCiclo() {
        Marcados visitados = new Marcados(nodos.tamano());
        for (Nodo<T> nodo : nodos) {
            if (!visitados.estaMarcado(nodo.getIndice())
                    && dfsDetectarCiclo(nodo, null, visitados)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsDetectarCiclo(Nodo<T> nodo, Arista<T> aristaEntrada, Marcados visitados) {
        visitados.marcar(nodo.getIndice());
        for (Arista<T> arista : nodo.getAristas()) {
            if (!arista.isActiva() || arista == aristaEntrada) {
                continue;
            }
            Nodo<T> vecino = arista.getDestino() == nodo ? arista.getOrigen() : arista.getDestino();
            if (vecino == nodo) {
                // Lazo sobre el mismo nodo: es un ciclo.
                return true;
            }
            if (visitados.estaMarcado(vecino.getIndice())) {
                return true;
            }
            if (dfsDetectarCiclo(vecino, arista, visitados)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Nodo<T>> iterator() {
        return new IteradorGrafo();
    }

    /**
     * Iterador propio del grafo sobre sus nodos.
     */
    public class IteradorGrafo implements Iterator<Nodo<T>> {

        private final Iterator<Nodo<T>> delegado = nodos.iterator();

        @Override
        public boolean hasNext() {
            return delegado.hasNext();
        }

        @Override
        public Nodo<T> next() {
            if (!delegado.hasNext()) {
                throw new NoSuchElementException("No hay más nodos en el grafo.");
            }
            return delegado.next();
        }
    }

    @Override
    public String toString() {
        return "Grafo[" + nodos.tamano() + " nodos, " + aristas.tamano() + " aristas]";
    }
}
