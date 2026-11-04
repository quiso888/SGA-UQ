package service;

import datastructures.ListaEnlazada;
import exceptions.SinAlojamientoDisponibleException;
import model.Apartamento;
import model.Estancia;

import java.util.Comparator;

/**
 * Asignador de grupos: reparte un grupo grande entre varios apartamentos.
 *
 * <p>Cuando el grupo no cabe en un solo apartamento, se busca la <b>combinación
 * mínima</b> de apartamentos cuya capacidad sumada alcance al número de personas.
 * La búsqueda se resuelve con <b>backtracking recursivo</b>:</p>
 *
 * <ol>
 *   <li>Se ordenan los candidatos de mayor a menor capacidad.</li>
 *   <li>Se prueban las soluciones de un solo apartamento, luego de dos, de tres…
 *       La <b>primera que se encuentra es la de menor número de apartamentos</b>.</li>
 *   <li>Para cada tamaño se recorre el árbol de combinaciones; cuando una rama no
 *       llega a una solución válida se <b>retrocede</b> (backtracking) y se prueba el
 *       siguiente candidato.</li>
 *   <li>Se poda la rama cuando la capacidad que queda por explorar es insuficiente
 *       para acomodar al grupo.</li>
 * </ol>
 *
 * <p>Solo se consideran apartamentos libres en el rango de fechas solicitado, de
 * modo que la asignación nunca genera un solapamiento.</p>
 */
public class AsignadorGrupos {

    /** Criterio de ordenamiento de los candidatos: mayor capacidad primero. */
    private static final Comparator<Apartamento> POR_CAPACIDAD = new Comparator<Apartamento>() {
        @Override
        public int compare(Apartamento a, Apartamento b) {
            int resultado = Integer.compare(b.getCapacidadMaxima(), a.getCapacidadMaxima());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareToIgnoreCase(b.getCodigo());
        }
    };

    private final GestorReservas gestorReservas;

    public AsignadorGrupos(GestorReservas gestorReservas) {
        this.gestorReservas = gestorReservas;
    }

    /**
     * Busca la combinación <b>mínima</b> de apartamentos libres capaz de alojar al
     * grupo.
     *
     * @param estancia  rango de fechas de la estancia
     * @param personas  número de personas del grupo
     * @return los apartamentos asignados, en el orden en que se eligieron
     * @throws SinAlojamientoDisponibleException si no hay combinación posible
     */
    public ListaEnlazada<Apartamento> asignar(Estancia estancia, int personas) {
        if (personas <= 0) {
            throw new IllegalArgumentException("El número de personas debe ser mayor que cero.");
        }
        ListaEnlazada<Apartamento> candidatos = candidatosValidos(estancia);
        if (candidatos.estaVacia()) {
            throw new SinAlojamientoDisponibleException(personas);
        }
        candidatos.ordenar(POR_CAPACIDAD);

        // Se explora por tamaño creciente: la primera solución hallada es la
        // que usa el menor número de apartamentos.
        for (int objetivo = 1; objetivo <= candidatos.tamano(); objetivo++) {
            ListaEnlazada<Apartamento> actual = new ListaEnlazada<>();
            if (buscarCombinacion(candidatos, 0, personas, actual, objetivo)) {
                return actual;
            }
        }
        throw new SinAlojamientoDisponibleException(personas);
    }

    /**
     * Backtracking: busca una combinación de <b>exactamente</b> {@code objetivo}
     * apartamentos cuya capacidad total cubra a las personas del grupo.
     *
     * @param candidatos        lista de apartamentos disponibles, ya ordenada
     * @param indiceActual      candidato desde el que se explora
     * @param personasRestantes personas que faltan por acomodar
     * @param actual            combinación que se está construyendo
     * @param objetivo          número de apartamentos de la solución buscada
     * @return {@code true} si se halló una solución
     */
    private boolean buscarCombinacion(ListaEnlazada<Apartamento> candidatos, int indiceActual,
                                      int personasRestantes, ListaEnlazada<Apartamento> actual,
                                      int objetivo) {
        if (actual.tamano() == objetivo) {
            return personasRestantes <= 0;
        }
        int restantes = candidatos.tamano() - indiceActual;
        if (restantes < objetivo - actual.tamano()) {
            // No quedan suficientes candidatos para completar la combinación.
            return false;
        }
        for (int i = indiceActual; i < candidatos.tamano(); i++) {
            // Poda: la capacidad de los candidatos que quedan por explorar no
            // alcanza para acomodar al grupo.
            if (capacidadRestante(candidatos, i) < personasRestantes) {
                break;
            }
            Apartamento candidato = candidatos.obtener(i);
            actual.agregar(candidato);
            boolean encontro = buscarCombinacion(candidatos, i + 1,
                    personasRestantes - candidato.getCapacidadMaxima(), actual, objetivo);
            if (encontro) {
                return true;
            }
            // Retroceso: se deshace la elección y se prueba el siguiente candidato.
            actual.remover(actual.tamano() - 1);
        }
        return false;
    }

    /**
     * Suma de las capacidades de los candidatos desde la posición indicada. Sirve
     * para podar las ramas imposibles del backtracking.
     */
    private int capacidadRestante(ListaEnlazada<Apartamento> candidatos, int indice) {
        int total = 0;
        for (int i = indice; i < candidatos.tamano(); i++) {
            total += candidatos.obtener(i).getCapacidadMaxima();
        }
        return total;
    }

    /**
     * Candidatos válidos: apartamentos libres en el rango y no fuera de servicio.
     */
    private ListaEnlazada<Apartamento> candidatosValidos(Estancia estancia) {
        ListaEnlazada<Apartamento> candidatos = new ListaEnlazada<>();
        for (Apartamento apartamento : gestorReservas.todosApartamentos()) {
            if (!gestorReservas.estaDisponible(apartamento, estancia)) {
                continue;
            }
            candidatos.agregar(apartamento);
        }
        return candidatos;
    }

    /**
     * Indica si un grupo cabe en un solo apartamento sin importar cuál sea.
     */
    public boolean cabeEnUnApartamento(Estancia estancia, int personas) {
        for (Apartamento apartamento : gestorReservas.todosApartamentos()) {
            if (gestorReservas.estaDisponible(apartamento, estancia)
                    && apartamento.getCapacidadMaxima() >= personas) {
                return true;
            }
        }
        return false;
    }
}
