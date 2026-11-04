package service;

import datastructures.ArbolBinarioBusqueda;
import datastructures.ListaEnlazada;
import exceptions.ElementoNoEncontradoException;
import model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fachada del Sistema de Gestión de Alojamiento.
 *
 * <p>Esta clase es el <b>punto de entrada único</b> que usan la interfaz gráfica y
 * la autocomprobación de consola. Concentra los servicios especializados:</p>
 *
 * <ul>
 *   <li>{@link GestorReservas}: catálogo, creación y ciclo de vida de las reservas.</li>
 *   <li>{@link GestorEdificio}: grafo del mapa del alojamiento.</li>
 *   <li>{@link GestorPreparacion}: cola de prioridad de limpieza y ruta con Dijkstra.</li>
 *   <li>{@link AsignadorGrupos}: backtracking para grupos grandes.</li>
 *   <li>{@link ProcesadorCanalExterno}: cola FIFO del canal de terceros.</li>
 *   <li>{@link GestorFolios}: movimientos, contra-movimientos y deshacer.</li>
 *   <li>{@link ServicioTemporadas} y {@link ServicioEstadisticas}: reportes.</li>
 * </ul>
 */
public class SistemaAlojamiento {

    private final model.CalendarioTemporadas calendario = new model.CalendarioTemporadas();
    private final GestorReservas gestorReservas = new GestorReservas(calendario);
    private final GestorEdificio gestorEdificio = new GestorEdificio();
    private final GestorPreparacion gestorPreparacion = new GestorPreparacion(gestorEdificio);
    private final AsignadorGrupos asignadorGrupos = new AsignadorGrupos(gestorReservas);
    private final ProcesadorCanalExterno procesadorCanalExterno = new ProcesadorCanalExterno(gestorReservas);
    private final GestorFolios gestorFolios = new GestorFolios(gestorReservas);
    private final ServicioTemporadas servicioTemporadas = new ServicioTemporadas(calendario);
    private final ServicioEstadisticas servicioEstadisticas =
            new ServicioEstadisticas(gestorReservas, gestorPreparacion, procesadorCanalExterno);

    /** Empleados del alojamiento, organizados en un árbol de búsqueda por documento. */
    private final ArbolBinarioBusqueda<Empleado> empleados = new ArbolBinarioBusqueda<>();

    /**
     * Turno de recepción: los recepcionistas rotan de forma circular, de modo que
     * al terminar el turno actual pasa a atender el siguiente.
     */
    private final datastructures.ListaCircular<Empleado> turnoRecepcion =
            new datastructures.ListaCircular<>();

    // ------------------------------------------------------------------
    // Catálogos
    // ------------------------------------------------------------------

    /**
     * Registra un apartamento en el catálogo y lo agrega al mapa del edificio.
     */
    public void registrarApartamento(Apartamento apartamento) {
        gestorReservas.registrarApartamento(apartamento);
        gestorEdificio.registrarApartamento(apartamento);
    }

    /**
     * Registra un huésped en el catálogo.
     */
    public void registrarHuesped(Huesped huesped) {
        gestorReservas.registrarHuesped(huesped);
    }

    /** Registra un empleado del alojamiento (árbol de búsqueda por documento). */
    public void registrarEmpleado(Empleado empleado) {
        if (empleado != null && empleados.insertar(empleado)
                && empleado.getCargo() == Empleado.Cargo.RECEPCIONISTA) {
            turnoRecepcion.agregar(empleado);
        }
    }

    /**
     * Busca un empleado por documento.
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Empleado buscarEmpleado(String documento) {
        Empleado empleado = empleados.buscar(Empleado.claveDeBusqueda(documento));
        if (empleado == null) {
            throw new ElementoNoEncontradoException("El empleado", documento);
        }
        return empleado;
    }

    /**
     * Cambia el nombre y el cargo de un empleado, manteniendo coherente la
     * rotación de turnos de recepción.
     */
    public void actualizarEmpleado(String documento, String nombre, Empleado.Cargo cargo) {
        Empleado empleado = buscarEmpleado(documento);
        boolean eraRecepcionista = empleado.getCargo() == Empleado.Cargo.RECEPCIONISTA;
        empleado.setNombre(nombre);
        empleado.setCargo(cargo);
        boolean esRecepcionista = empleado.getCargo() == Empleado.Cargo.RECEPCIONISTA;
        if (eraRecepcionista && !esRecepcionista) {
            turnoRecepcion.remover(empleado);
        } else if (!eraRecepcionista && esRecepcionista) {
            turnoRecepcion.agregar(empleado);
        }
    }

    /** Da de baja a un empleado y lo saca de la rotación de turnos. */
    public void eliminarEmpleado(String documento) {
        Empleado empleado = buscarEmpleado(documento);
        empleados.eliminar(empleado);
        turnoRecepcion.remover(empleado);
    }

    /** Árbol de empleados por documento (para mostrar sus recorridos). */
    public ArbolBinarioBusqueda<Empleado> getArbolEmpleados() {
        return empleados;
    }

    /**
     * Da de baja un apartamento del catálogo y del mapa. Solo es posible si nunca
     * tuvo reservas y no está esperando preparación.
     */
    public void eliminarApartamento(String codigo) {
        Apartamento apartamento = gestorReservas.buscarApartamento(codigo);
        if (apartamento.getEstado().requierePreparacion()) {
            throw new IllegalStateException("El apartamento " + apartamento.getCodigo()
                    + " está en el flujo de preparación y no se puede eliminar.");
        }
        gestorReservas.eliminarApartamento(codigo);
        gestorEdificio.eliminarApartamento(codigo);
    }

    /** Da de baja a un huésped que no tenga reservas en su historial. */
    public void eliminarHuesped(String documento) {
        gestorReservas.eliminarHuesped(documento);
    }

    /**
     * Recepcionista que tiene el turno actual.
     */
    public Empleado recepcionistaDeTurno() {
        return turnoRecepcion.estaVacia() ? null : turnoRecepcion.actual();
    }

    /**
     * Cierra el turno actual y pasa al siguiente recepcionista.
     *
     * @return el recepcionista que queda de turno
     */
    public Empleado rotarTurnoRecepcion() {
        if (turnoRecepcion.estaVacia()) {
            return null;
        }
        turnoRecepcion.rotar();
        return turnoRecepcion.actual();
    }

    /** Empleados ordenados por documento (recorrido in-orden del árbol). */
    public ListaEnlazada<Empleado> getEmpleados() {
        return empleados.recorridoInOrden();
    }

    public GestorReservas getGestorReservas() {
        return gestorReservas;
    }

    public GestorEdificio getGestorEdificio() {
        return gestorEdificio;
    }

    public GestorPreparacion getGestorPreparacion() {
        return gestorPreparacion;
    }

    public AsignadorGrupos getAsignadorGrupos() {
        return asignadorGrupos;
    }

    public ProcesadorCanalExterno getProcesadorCanalExterno() {
        return procesadorCanalExterno;
    }

    public GestorFolios getGestorFolios() {
        return gestorFolios;
    }

    public ServicioTemporadas getServicioTemporadas() {
        return servicioTemporadas;
    }

    public ServicioEstadisticas getServicioEstadisticas() {
        return servicioEstadisticas;
    }

    public model.CalendarioTemporadas getCalendario() {
        return calendario;
    }

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    /**
     * Crea una reserva desde el portal web: queda <b>Pendiente</b> hasta que se
     * registre el anticipo del 30 %.
     */
    public Reserva crearReservaPortal(Huesped titular, Apartamento apartamento,
                                      Estancia estancia, ListaEnlazada<Ocupante> ocupantes,
                                      LocalTime horaEstimadaLlegada) {
        return gestorReservas.crearReserva(titular, apartamento, CanalReserva.PORTAL,
                estancia, ocupantes, horaEstimadaLlegada);
    }

    /**
     * Crea una reserva registrada directamente por la recepción: queda
     * <b>Confirmada</b> de una vez.
     */
    public Reserva crearReservaDirecta(Huesped titular, Apartamento apartamento,
                                       Estancia estancia, ListaEnlazada<Ocupante> ocupantes,
                                       LocalTime horaEstimadaLlegada) {
        Reserva reserva = gestorReservas.crearReserva(titular, apartamento, CanalReserva.DIRECTA,
                estancia, ocupantes, horaEstimadaLlegada);
        reserva.cambiarEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    /** Registra el anticipo y confirma la reserva si el monto es suficiente. */
    public Reserva registrarAnticipo(String codigoReserva, double monto) {
        return gestorReservas.registrarAnticipo(codigoReserva, monto);
    }

    /** Cancela una reserva y libera sus noches. */
    public Reserva cancelarReserva(String codigoReserva, String motivo) {
        return gestorReservas.cancelarReserva(codigoReserva, motivo);
    }

    /** Registra un no-show. */
    public Reserva registrarNoShow(String codigoReserva, String motivo) {
        return gestorReservas.registrarNoShow(codigoReserva, motivo);
    }

    /** Check-in: el apartamento pasa a Ocupado. */
    public Reserva registrarCheckIn(String codigoReserva) {
        return gestorReservas.registrarCheckIn(codigoReserva);
    }

    /**
     * Check-out: la reserva queda Finalizada y el apartamento pasa a
     * <b>Pendiente de preparación</b>, con su tarea en la cola de prioridad.
     */
    public Reserva registrarCheckOut(String codigoReserva) {
        Reserva reserva = gestorReservas.registrarCheckOut(codigoReserva);
        encolarPreparacion(reserva.getApartamento(), reserva);
        return reserva;
    }

    /**
     * Encola la preparación del apartamento que quedó libre.
     */
    private void encolarPreparacion(Apartamento apartamento, Reserva reservaSaliente) {
        // Se busca la siguiente reserva del apartamento: si entra hoy, la limpieza
        // es urgente (prioridad 1).
        LocalDate hoy = LocalDate.now();
        boolean entradaHoy = false;
        LocalTime horaLlegada = LocalTime.MAX;
        String codigoEntrante = null;
        for (Reserva candidata : apartamento.getReservas()) {
            if (candidata.getEstado() == EstadoReserva.CANCELADA
                    || candidata.getEstado() == EstadoReserva.NO_SHOW) {
                continue;
            }
            if (candidata.getEstancia().getFechaEntrada().isBefore(hoy)) {
                continue;
            }
            if (candidata.getEstancia().getFechaEntrada().equals(hoy)) {
                entradaHoy = true;
                horaLlegada = candidata.getHoraEstimadaLlegada();
                codigoEntrante = candidata.getCodigo();
                break;
            }
        }
        gestorPreparacion.encolar(apartamento, entradaHoy, horaLlegada, codigoEntrante);
    }

    /** Procesa los vencimientos: cancela las reservas pendientes que superaron 24 h. */
    public ListaEnlazada<Reserva> procesarVencimientos(LocalDateTime ahora) {
        return gestorReservas.procesarVencimientos(ahora);
    }

    /**
     * Apartamentos disponibles para un rango de fechas y un número de personas,
     * ordenados por el criterio indicado.
     */
    public ListaEnlazada<Apartamento> apartamentosDisponibles(Estancia estancia, int personas,
                                                               java.util.Comparator<Apartamento> orden) {
        return gestorReservas.apartamentosDisponibles(estancia, personas, orden);
    }

    /** Apartamentos disponibles ordenados por precio de la temporada indicada. */
    public ListaEnlazada<Apartamento> apartamentosDisponiblesPorPrecio(Estancia estancia,
                                                                        int personas,
                                                                        Temporada temporada) {
        return gestorReservas.apartamentosDisponiblesPorPrecio(estancia, personas, temporada);
    }

    /** Apartamentos disponibles ordenados por capacidad. */
    public ListaEnlazada<Apartamento> apartamentosDisponiblesPorCapacidad(Estancia estancia,
                                                                          int personas) {
        return gestorReservas.apartamentosDisponibles(estancia, personas,
                new Comparadores.PorCapacidad());
    }

    /** Apartamentos disponibles ordenados por número de reservas registradas. */
    public ListaEnlazada<Apartamento> apartamentosDisponiblesPorNumeroReservas(Estancia estancia,
                                                                              int personas) {
        return gestorReservas.apartamentosDisponibles(estancia, personas,
                new Comparadores.PorNumeroReservas());
    }

    /**
     * Asigna un grupo grande al conjunto mínimo de apartamentos (backtracking).
     */
    public ListaEnlazada<Apartamento> asignarGrupo(Estancia estancia, int personas) {
        return asignadorGrupos.asignar(estancia, personas);
    }

    // ------------------------------------------------------------------
    // Canal externo
    // ------------------------------------------------------------------

    /** Encola una solicitud de la plataforma externa. */
    public void encolarSolicitudExterna(SolicitudExterna solicitud) {
        procesadorCanalExterno.encolarSolicitud(solicitud);
    }

    /** Procesa todas las solicitudes externas pendientes. */
    public ListaEnlazada<Reserva> procesarCanalExterno() {
        return procesadorCanalExterno.procesarTodas();
    }

    // ------------------------------------------------------------------
    // Preparación
    // ------------------------------------------------------------------

    /** Ruta de limpieza de los apartamentos pendientes. */
    public ListaEnlazada<Apartamento> rutaDeLimpieza() {
        return gestorPreparacion.rutaDeLimpieza();
    }

    // ------------------------------------------------------------------
    // Utilidades para la interfaz
    // ------------------------------------------------------------------

    /**
     * Devuelve el apartamento con el código indicado.
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Apartamento buscarApartamento(String codigo) {
        return gestorReservas.buscarApartamento(codigo);
    }

    /**
     * Devuelve el huésped con el documento indicado.
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Huesped buscarHuesped(String documento) {
        return gestorReservas.buscarHuesped(documento);
    }

    /**
     * Devuelve el huésped con el documento indicado, o {@code null} si no existe.
     */
    public Huesped buscarHuespedOpcional(String documento) {
        return gestorReservas.buscarHuespedOpcional(documento);
    }

    /** Todos los apartamentos del alojamiento. */
    public ListaEnlazada<Apartamento> todosApartamentos() {
        return gestorReservas.todosApartamentos();
    }

    /** Todos los huéspedes registrados. */
    public ListaEnlazada<Huesped> todosHuespedes() {
        return gestorReservas.todosHuespedes();
    }

    /** Todas las reservas del alojamiento. */
    public ListaEnlazada<Reserva> todasReservas() {
        return gestorReservas.todasReservas();
    }

    /**
     * Cambia el estado operativo de un apartamento.
     */
    public void cambiarEstadoApartamento(String codigo, EstadoApartamento estado, String motivo) {
        Apartamento apartamento = gestorReservas.buscarApartamento(codigo);
        if (estado == EstadoApartamento.FUERA_DE_SERVICIO) {
            apartamento.cambiarEstado(estado, motivo);
        } else {
            apartamento.cambiarEstado(estado);
        }
    }
}
