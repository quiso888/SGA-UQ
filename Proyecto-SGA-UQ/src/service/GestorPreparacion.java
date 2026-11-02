package service;

import datastructures.ColaPrioridad;
import datastructures.Grafo;
import datastructures.ListaEnlazada;
import exceptions.ElementoNoEncontradoException;
import exceptions.EstructuraVaciaException;
import exceptions.TransicionEstadoInvalidaException;
import model.Apartamento;
import model.Comparadores;
import model.EstadoApartamento;
import model.TareaPreparacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Gestor de preparación: coordina la limpieza de los apartamentos que quedan
 * <b>Pendiente de preparación</b> después de un check-out.
 *
 * <p>Usa dos estructuras propias del SGA:</p>
 * <ul>
 *   <li>Una <b>cola de prioridad</b> ({@link ColaPrioridad}) que ordena las tareas
 *       de limpieza. Entra con prioridad 1 el apartamento que tiene entrada
 *       programada para hoy y con prioridad 2 el que no la tiene; dentro de cada
 *       grupo se desempata por la hora estimada de llegada del siguiente grupo y por
 *       la antigüedad de la tarea ({@link Comparadores.PrioridadPreparacion}).</li>
 *   <li>El <b>grafo del edificio</b> ({@link GestorEdificio}) para calcular, con
 *       Dijkstra, la <b>ruta de limpieza</b>: el orden en que el personal debe
 *       recorrer los apartamentos pendientes partiendo de la recepción.</li>
 * </ul>
 */
public class GestorPreparacion {

    /** Cola inteligente de tareas de preparación. */
    private final ColaPrioridad<TareaPreparacion> cola =
            new ColaPrioridad<>(new Comparadores.PrioridadPreparacion());

    /** Tareas ya terminadas (se conservan para las estadísticas). */
    private final ListaEnlazada<TareaPreparacion> historial = new ListaEnlazada<>();

    private final GestorEdificio edificio;

    public GestorPreparacion(GestorEdificio edificio) {
        this.edificio = edificio;
    }

    // ------------------------------------------------------------------
    // Encolado y consulta de la cola
    // ------------------------------------------------------------------

    /**
     * Encola la preparación de un apartamento.
     *
     * @param apartamento      apartamento a preparar
     * @param entradaHoy       {@code true} si el siguiente grupo entra hoy
     * @param horaLlegada      hora estimada de llegada del siguiente grupo
     * @param reservaEntrante  código de la reserva entrante (puede ser {@code null})
     */
    public TareaPreparacion encolar(Apartamento apartamento, boolean entradaHoy,
                                    LocalTime horaLlegada, String reservaEntrante) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento es obligatorio.");
        }
        TareaPreparacion tarea = new TareaPreparacion(apartamento, entradaHoy, horaLlegada,
                LocalDateTime.now(), reservaEntrante);
        // La prioridad numérica coincide con la prioridad lógica de la tarea:
        // 1 = entrada hoy, 2 = sin entrada hoy.
        cola.encolar(tarea, tarea.getPrioridad());
        return tarea;
    }

    /**
     * Siguiente tarea a atender, sin retirarla de la cola.
     *
     * @throws EstructuraVaciaException si no hay tareas pendientes
     */
    public TareaPreparacion siguienteTarea() {
        if (cola.estaVacia()) {
            throw new EstructuraVaciaException("Cola de preparación", "consultar la siguiente tarea");
        }
        return cola.verFrente();
    }

    /**
     * Todas las tareas pendientes, en el orden en que deben atenderse.
     */
    public ListaEnlazada<TareaPreparacion> tareasPendientes() {
        ListaEnlazada<TareaPreparacion> tareas = new ListaEnlazada<>();
        for (TareaPreparacion tarea : cola) {
            tareas.agregar(tarea);
        }
        return tareas;
    }

    /** Tareas que tienen entrada programada para hoy. */
    public ListaEnlazada<TareaPreparacion> tareasConEntradaHoy() {
        ListaEnlazada<TareaPreparacion> tareas = new ListaEnlazada<>();
        for (TareaPreparacion tarea : cola) {
            if (tarea.isEntradaHoy()) {
                tareas.agregar(tarea);
            }
        }
        return tareas;
    }

    public int numeroTareasPendientes() {
        return cola.tamano();
    }

    public boolean hayTareasPendientes() {
        return !cola.estaVacia();
    }

    // ------------------------------------------------------------------
    // Ejecución de la preparación
    // ------------------------------------------------------------------

    /**
     * Comienza la preparación de un apartamento: el apartamento pasa a
     * <b>En preparación</b> y la tarea queda en curso.
     *
     * @throws TransicionEstadoInvalidaException si el apartamento no estaba pendiente
     */
    public TareaPreparacion iniciar(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento es obligatorio.");
        }
        if (apartamento.getEstado() != EstadoApartamento.PENDIENTE_PREPARACION) {
            throw new TransicionEstadoInvalidaException(
                    apartamento.getEstado().getDescripcion(), EstadoApartamento.EN_PREPARACION.getDescripcion());
        }
        TareaPreparacion tarea = buscarTarea(apartamento);
        tarea.iniciar();
        apartamento.cambiarEstado(EstadoApartamento.EN_PREPARACION);
        return tarea;
    }

    /**
     * Termina la preparación: el apartamento queda <b>Preparado</b> y vuelve a
     * admitir huéspedes.
     *
     * @return los segundos que duró la limpieza
     */
    public long completar(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento es obligatorio.");
        }
        if (apartamento.getEstado() != EstadoApartamento.EN_PREPARACION) {
            throw new TransicionEstadoInvalidaException(
                    apartamento.getEstado().getDescripcion(), EstadoApartamento.PREPARADO.getDescripcion());
        }
        TareaPreparacion tarea = buscarTarea(apartamento);
        long duracion = tarea.finalizar();
        apartamento.cambiarEstado(EstadoApartamento.PREPARADO);
        cola.remover(tarea);
        historial.agregar(tarea);
        return duracion;
    }

    /**
     * Busca la tarea de un apartamento en la cola de prioridad.
     *
     * @throws ElementoNoEncontradoException si el apartamento no tiene tarea encolada
     */
    public TareaPreparacion buscarTarea(Apartamento apartamento) {
        for (TareaPreparacion tarea : cola) {
            if (tarea.getApartamento().getCodigo().equals(apartamento.getCodigo())) {
                return tarea;
            }
        }
        throw new ElementoNoEncontradoException(
                "La tarea de preparación del apartamento", apartamento.getCodigo());
    }

    /** Tareas de preparación ya completadas. */
    public ListaEnlazada<TareaPreparacion> historial() {
        return historial;
    }

    /**
     * Tiempo promedio de preparación, en minutos. Devuelve 0 si todavía no se ha
     * completado ninguna limpieza.
     */
    public double tiempoPromedioPreparacionMinutos() {
        if (historial.estaVacia()) {
            return 0;
        }
        long total = 0;
        int cantidad = 0;
        for (TareaPreparacion tarea : historial) {
            long duracion = tarea.duracionSegundos();
            if (duracion >= 0) {
                total += duracion;
                cantidad++;
            }
        }
        if (cantidad == 0) {
            return 0;
        }
        return (total / (double) cantidad) / 60.0;
    }

    // ------------------------------------------------------------------
    // Ruta de limpieza (Dijkstra)
    // ------------------------------------------------------------------

    /**
     * Calcula la <b>ruta de limpieza</b>: el orden en que el personal debe atender
     * los apartamentos pendientes de preparación, partiendo de la recepción y
     * eligiendo en cada paso el apartamento más cercano.
     *
     * <p>La distancia entre dos puntos se obtiene con Dijkstra sobre el grafo del
     * edificio, de modo que la ruta respeta los pasillos abiertos y sus pesos.</p>
     *
     * @return los apartamentos en el orden en que deben atenderse
     */
    public ListaEnlazada<Apartamento> rutaDeLimpieza() {
        return rutaDeLimpieza(GestorEdificio.RECEPCION);
    }

    /**
     * Ruta de limpieza partiendo de un punto concreto del mapa.
     */
    public ListaEnlazada<Apartamento> rutaDeLimpieza(String puntoPartida) {
        ListaEnlazada<Apartamento> ruta = new ListaEnlazada<>();
        if (edificio == null) {
            return ruta;
        }
        // Copia de los pendientes: el recorrido los va consumiendo.
        ListaEnlazada<TareaPreparacion> pendientes = new ListaEnlazada<>();
        for (TareaPreparacion tarea : cola) {
            pendientes.agregar(tarea);
        }
        String actual = puntoPartida;
        while (!pendientes.estaVacia()) {
            TareaPreparacion masCercana = null;
            int mejorDistancia = Integer.MAX_VALUE;
            for (TareaPreparacion tarea : pendientes) {
                Grafo.Ruta<String> rutaCandidata = edificio.ruta(actual, tarea.getApartamento().getCodigo());
                if (!rutaCandidata.esAlcanzable()) {
                    continue;
                }
                if (rutaCandidata.getDistancia() < mejorDistancia) {
                    mejorDistancia = rutaCandidata.getDistancia();
                    masCercana = tarea;
                }
            }
            if (masCercana == null) {
                // Los apartamentos restantes son inalcanzables: se agregan al final.
                for (TareaPreparacion tarea : pendientes) {
                    ruta.agregar(tarea.getApartamento());
                }
                break;
            }
            ruta.agregar(masCercana.getApartamento());
            pendientes.removerValor(masCercana);
            actual = masCercana.getApartamento().getCodigo();
        }
        return ruta;
    }

    /**
     * Distancia total (en metros) de la ruta de limpieza desde la recepción.
     */
    public int distanciaRutaDeLimpieza() {
        int distancia = 0;
        String actual = GestorEdificio.RECEPCION;
        for (Apartamento apartamento : rutaDeLimpieza()) {
            Grafo.Ruta<String> ruta = edificio.ruta(actual, apartamento.getCodigo());
            if (ruta.esAlcanzable()) {
                distancia += ruta.getDistancia();
            }
            actual = apartamento.getCodigo();
        }
        return distancia;
    }

    /**
     * Comprueba si un apartamento recibe un grupo hoy (para asignar la prioridad 1
     * de la cola de preparación).
     *
     * @param codigoReserva código de la reserva entrante
     */
    public static boolean esEntradaHoy(LocalDate hoy, LocalDate fechaEntrada) {
        return hoy != null && fechaEntrada != null && hoy.equals(fechaEntrada);
    }
}
