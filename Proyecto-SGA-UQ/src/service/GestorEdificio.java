package service;

import datastructures.Grafo;
import datastructures.ListaEnlazada;
import exceptions.ElementoNoEncontradoException;
import model.Apartamento;

/**
 * Gestor del edificio: administra el <b>grafo</b> que representa el mapa del
 * alojamiento (Aparta-Suites UQ, Salento).
 *
 * <p>El grafo tiene nodos de dos tipos:</p>
 * <ul>
 *   <li><b>Apartamentos:</b> su etiqueta es el código del apartamento (por ejemplo
 *       {@code A-101}) y su posición en el mapa.</li>
 *   <li><b>Instalaciones:</b> recepción, escaleras y cuartos de aseo, con
 *       etiquetas que empiezan por {@code SERV-}.</li>
 * </ul>
 *
 * <p>Los pasillos son aristas con un peso (metros que hay que recorrer). Sobre este
 * grafo se aplican:</p>
 * <ul>
 *   <li><b>Dijkstra</b> para calcular la ruta más corta entre la recepción y un
 *       apartamento (usada por el panel de preparación para planear la ruta de
 *       limpieza).</li>
 *   <li><b>BFS</b> para detectar apartamentos que quedaron <b>inalcanzables</b> al
 *       cerrar un pasillo y para agrupar por bloques.</li>
 *   <li><b>DFS recursivo</b> para verificar que el edificio no tenga ciclos
 *       de pasillos ni zonas aisladas.</li>
 * </ul>
 */
public class GestorEdificio {

    /** Etiqueta del nodo que representa la recepción del alojamiento. */
    public static final String RECEPCION = "RECEPCION";

    /** Prefijo reservado para las instalaciones (escaleras, aseos, recepción). */
    public static final String PREFIJO_INSTALACION = "SERV-";

    /** Agrupación de apartamentos por bloque del edificio. */
    public static class Bloque {

        private final String nombre;
        private final ListaEnlazada<Apartamento> apartamentos = new ListaEnlazada<>();

        Bloque(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() {
            return nombre;
        }

        public ListaEnlazada<Apartamento> getApartamentos() {
            return apartamentos;
        }

        public int cantidad() {
            return apartamentos.tamano();
        }

        @Override
        public String toString() {
            return nombre + " (" + cantidad() + " apartamentos)";
        }
    }

    private final Grafo<String> grafo = new Grafo<>();
    private final ListaEnlazada<Apartamento> apartamentos = new ListaEnlazada<>();

    // ------------------------------------------------------------------
    // Registro de nodos y aristas
    // ------------------------------------------------------------------

    /**
     * Registra un apartamento como nodo del grafo, usando su código como etiqueta y
     * su posición como coordenadas. Si el apartamento ya estaba registrado, solo
     * actualiza la posición.
     */
    public void registrarApartamento(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento es obligatorio.");
        }
        if (!apartamentos.contiene(apartamento)) {
            apartamentos.agregar(apartamento);
        }
        if (grafo.buscarNodo(apartamento.getCodigo()) == null) {
            grafo.agregarNodo(apartamento.getCodigo(),
                    apartamento.getPosicionX(), apartamento.getPosicionY());
        } else {
            actualizarPosicion(apartamento.getCodigo(),
                    apartamento.getPosicionX(), apartamento.getPosicionY());
        }
    }

    /**
     * Retira un apartamento del mapa: elimina su nodo y los pasillos que llegan a él.
     */
    public void eliminarApartamento(String codigo) {
        for (Apartamento apartamento : apartamentos) {
            if (apartamento.getCodigo().equalsIgnoreCase(codigo)) {
                apartamentos.removerValor(apartamento);
                break;
            }
        }
        grafo.eliminarNodo(codigo);
    }

    /**
     * Registra una instalación del edificio (escalera, cuarto de aseo, recepción).
     *
     * @param etiqueta etiqueta del nodo
     * @param x        posición horizontal
     * @param y        posición vertical
     */
    public void registrarInstalacion(String etiqueta, double x, double y) {
        if (etiqueta == null || etiqueta.trim().isEmpty()) {
            throw new IllegalArgumentException("La etiqueta de la instalación es obligatoria.");
        }
        String limpia = etiqueta.trim();
        if (grafo.buscarNodo(limpia) == null) {
            grafo.agregarNodo(limpia, x, y);
        } else {
            actualizarPosicion(limpia, x, y);
        }
    }

    /**
     * Conecta dos nodos con un pasillo.
     *
     * @param peso distancia en metros del pasillo
     */
    public void conectar(String origen, String destino, int peso) {
        grafo.agregarArista(origen, destino, peso);
    }

    /** Mueve un nodo del mapa (permite arrastrar los apartamentos en la interfaz). */
    public void actualizarPosicion(String etiqueta, double x, double y) {
        Grafo.Nodo<String> nodo = grafo.buscarNodo(etiqueta);
        if (nodo == null) {
            throw new ElementoNoEncontradoException("El nodo del mapa", etiqueta);
        }
        nodo.setPosicion(x, y);
        for (Apartamento apartamento : apartamentos) {
            if (apartamento.getCodigo().equals(etiqueta)) {
                apartamento.setPosicion(x, y);
            }
        }
    }

    /** Cierra un pasillo: los apartamentos que dependan de él quedan inalcanzables. */
    public boolean cerrarPasillo(String origen, String destino) {
        return grafo.cerrarPasillo(origen, destino);
    }

    /** Reabre un pasillo cerrado. */
    public boolean abrirPasillo(String origen, String destino) {
        return grafo.abrirPasillo(origen, destino);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Grafo<String> getGrafo() {
        return grafo;
    }

    public ListaEnlazada<Apartamento> getApartamentos() {
        return apartamentos;
    }

    /**
     * Busca el apartamento asociado a un código.
     *
     * @throws ElementoNoEncontradoException si el apartamento no está en el edificio
     */
    public Apartamento buscarApartamento(String codigo) {
        for (Apartamento apartamento : apartamentos) {
            if (apartamento.getCodigo().equalsIgnoreCase(codigo)) {
                return apartamento;
            }
        }
        throw new ElementoNoEncontradoException("El apartamento", codigo);
    }

    /**
     * Apartments que <b>no</b> se pueden alcanzar desde la recepción (por ejemplo
     * porque se cerró el pasillo que los conecta).
     */
    public ListaEnlazada<Apartamento> apartamentosInalcanzables() {
        ListaEnlazada<Apartamento> resultado = new ListaEnlazada<>();
        if (grafo.buscarNodo(RECEPCION) == null) {
            return resultado;
        }
        ListaEnlazada<Grafo.Nodo<String>> inalcanzables = grafo.inalcanzables(RECEPCION);
        for (Grafo.Nodo<String> nodo : inalcanzables) {
            for (Apartamento apartamento : apartamentos) {
                if (apartamento.getCodigo().equals(nodo.getEtiqueta())) {
                    resultado.agregar(apartamento);
                }
            }
        }
        return resultado;
    }

    /**
     * Agrupa los apartamentos por <b>bloque</b> (torres A, B, C...).
     */
    public ListaEnlazada<Bloque> apartamentosPorBloque() {
        ListaEnlazada<Bloque> bloques = new ListaEnlazada<>();
        for (Apartamento apartamento : apartamentos) {
            String nombre = apartamento.getBloque() == null || apartamento.getBloque().isEmpty()
                    ? "Sin bloque" : apartamento.getBloque();
            Bloque bloque = buscarBloque(bloques, nombre);
            if (bloque == null) {
                bloque = new Bloque(nombre);
                bloques.agregar(bloque);
            }
            bloque.getApartamentos().agregar(apartamento);
        }
        return bloques;
    }

    private Bloque buscarBloque(ListaEnlazada<Bloque> bloques, String nombre) {
        for (Bloque bloque : bloques) {
            if (bloque.getNombre().equals(nombre)) {
                return bloque;
            }
        }
        return null;
    }

    /**
     * Ruta más corta (Dijkstra) entre la recepción y un apartamento.
     */
    public Grafo.Ruta<String> rutaRecepcionApartamento(String codigoApartamento) {
        return grafo.caminoMasCorto(RECEPCION, codigoApartamento);
    }

    /**
     * Ruta más corta entre dos nodos cualquiera del mapa.
     */
    public Grafo.Ruta<String> ruta(String origen, String destino) {
        return grafo.caminoMasCorto(origen, destino);
    }

    /**
     * {@code true} si el edificio tiene al menos un ciclo de pasillos.
     */
    public boolean tieneCiclo() {
        return grafo.tieneCiclo();
    }

    /**
     * Componentes conexas del edificio: sirve para detectar zonas aisladas.
     */
    public ListaEnlazada<ListaEnlazada<Grafo.Nodo<String>>> componentesConexas() {
        return grafo.componentesConexas();
    }

    /** Número de nodos del mapa. */
    public int numeroNodos() {
        return grafo.numeroNodos();
    }

    /** Número de pasillos del mapa. */
    public int numeroPasillos() {
        return grafo.numeroAristas();
    }
}
