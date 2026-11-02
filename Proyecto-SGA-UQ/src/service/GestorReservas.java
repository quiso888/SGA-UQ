package service;

import datastructures.ArbolBinarioBusqueda;
import datastructures.ColaPrioridad;
import datastructures.ListaEnlazada;
import exceptions.*;
import model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Gestor de reservas: corazón de las reglas de negocio del SGA.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Registrar apartamentos (árbol por código) y huéspedes (árbol por documento).</li>
 *   <li>Crear reservas validando fechas, capacidad y <b>solapamiento</b>.</li>
 *   <li>Detectar solapamientos recorriendo <b>de forma recursiva el árbol de
 *       reservas de cada apartamento</b>, podando por el orden de las fechas de
 *       entrada, sin revisar todas las reservas del alojamiento.</li>
 *   <li>Gestionar el ciclo de vida: confirmar, cancelar, no-show, check-in, check-out.</li>
 *   <li>Ordenar las reservas <b>Pendientes</b> por hora de vencimiento para poder
 *       cancelarlas automáticamente.</li>
 *   <li>Buscar disponibilidad filtrando por fechas y número de personas.</li>
 * </ul>
 */
public class GestorReservas {

    private final ArbolBinarioBusqueda<Apartamento> catalogoApartamentos = new ArbolBinarioBusqueda<>();
    private final ArbolBinarioBusqueda<Huesped> catalogoHuespedes = new ArbolBinarioBusqueda<>();

    /** Todas las reservas del alojamiento, ordenadas por fecha de entrada. */
    private final ArbolBinarioBusqueda<Reserva> reservasGlobales = new ArbolBinarioBusqueda<>();

    /**
     * Reservas en estado Pendiente, ordenadas por su hora de vencimiento
     * (24 horas después de creadas).
     */
    private final ColaPrioridad<Reserva> pendientesPorVencimiento = new ColaPrioridad<>();

    private final CalendarioTemporadas calendario;
    private final ServicioLiquidacion liquidacion;
    private int contadorReservas;

    public GestorReservas(CalendarioTemporadas calendario) {
        this.calendario = calendario;
        this.liquidacion = new ServicioLiquidacion(calendario);
    }

    // ------------------------------------------------------------------
    // Catálogos
    // ------------------------------------------------------------------

    /** Registra un apartamento en el catálogo indexado por código. */
    public void registrarApartamento(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento no puede ser nulo.");
        }
        catalogoApartamentos.insertar(apartamento);
    }

    /**
     * Busca un apartamento por su código (búsqueda en el árbol binario).
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Apartamento buscarApartamento(String codigo) {
        Apartamento apartamento = catalogoApartamentos.buscar(Apartamento.claveDeBusqueda(codigo));
        if (apartamento == null) {
            throw new ElementoNoEncontradoException("El apartamento", codigo);
        }
        return apartamento;
    }

    /** Busca un apartamento por código o devuelve {@code null} si no existe. */
    public Apartamento buscarApartamentoOpcional(String codigo) {
        if (codigo == null) {
            return null;
        }
        return catalogoApartamentos.buscar(Apartamento.claveDeBusqueda(codigo));
    }

    /** Todos los apartamentos del alojamiento, ordenados por código. */
    public ListaEnlazada<Apartamento> todosApartamentos() {
        return catalogoApartamentos.recorridoInOrden();
    }

    /** Registra un huésped en el catálogo indexado por documento. */
    public void registrarHuesped(Huesped huesped) {
        if (huesped == null) {
            throw new IllegalArgumentException("El huésped no puede ser nulo.");
        }
        catalogoHuespedes.insertar(huesped);
    }

    /**
     * Busca un huésped por su documento (búsqueda en el árbol binario).
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Huesped buscarHuesped(String documento) {
        Huesped huesped = catalogoHuespedes.buscar(Huesped.claveDeBusqueda(documento));
        if (huesped == null) {
            throw new ElementoNoEncontradoException("El huésped", documento);
        }
        return huesped;
    }

    /** Busca un huésped por documento o devuelve {@code null} si no existe. */
    public Huesped buscarHuespedOpcional(String documento) {
        if (documento == null) {
            return null;
        }
        return catalogoHuespedes.buscar(Huesped.claveDeBusqueda(documento));
    }

    /** Árbol de apartamentos por código (para mostrar sus recorridos). */
    public ArbolBinarioBusqueda<Apartamento> getCatalogoApartamentos() {
        return catalogoApartamentos;
    }

    /** Árbol de huéspedes por documento (para mostrar sus recorridos). */
    public ArbolBinarioBusqueda<Huesped> getCatalogoHuespedes() {
        return catalogoHuespedes;
    }

    /**
     * Da de baja un apartamento del catálogo. Solo se permite si nunca tuvo
     * reservas: de lo contrario se perdería el rastro de las estancias; en ese caso
     * conviene marcarlo Fuera de servicio.
     *
     * @throws ElementoNoEncontradoException si no existe
     * @throws IllegalStateException         si tiene reservas registradas
     */
    public Apartamento eliminarApartamento(String codigo) {
        Apartamento apartamento = buscarApartamento(codigo);
        if (apartamento.numeroReservas() > 0) {
            throw new IllegalStateException("El apartamento " + apartamento.getCodigo()
                    + " tiene " + apartamento.numeroReservas() + " reserva(s) registrada(s). "
                    + "Para retirarlo de la venta márquelo Fuera de servicio.");
        }
        catalogoApartamentos.eliminar(apartamento);
        return apartamento;
    }

    /**
     * Da de baja a un huésped del catálogo. Solo se permite si no tiene reservas
     * en su historial.
     *
     * @throws ElementoNoEncontradoException si no existe
     * @throws IllegalStateException         si tiene reservas registradas
     */
    public Huesped eliminarHuesped(String documento) {
        Huesped huesped = buscarHuesped(documento);
        if (!huesped.getHistorial().estaVacia()) {
            throw new IllegalStateException("El huésped " + huesped.getNombre() + " tiene "
                    + huesped.getHistorial().tamano() + " reserva(s) en su historial y no se "
                    + "puede eliminar.");
        }
        catalogoHuespedes.eliminar(huesped);
        return huesped;
    }

    /** Todos los huéspedes registrados, ordenados por documento. */
    public ListaEnlazada<Huesped> todosHuespedes() {
        return catalogoHuespedes.recorridoInOrden();
    }

    // ------------------------------------------------------------------
    // Creación de reservas
    // ------------------------------------------------------------------

    /**
     * Crea una reserva validando las reglas del alojamiento.
     *
     * @throws FechasInvalidasException     si el rango de fechas no es válido
     * @throws CapacidadExcedidaException   si el grupo excede la capacidad
     * @throws ReservaSolapadaException     si choca con una reserva vigente
     * @throws TransicionEstadoInvalidaException si el apartamento está fuera de servicio
     */
    public Reserva crearReserva(Huesped titular, Apartamento apartamento, CanalReserva canal,
                                 Estancia estancia, ListaEnlazada<Ocupante> ocupantes,
                                 LocalTime horaEstimadaLlegada) {
        validarApartamentoOperativo(apartamento);
        validarCapacidad(apartamento, ocupantes);
        validarSinSolapamiento(apartamento, estancia);

        Reserva reserva = new Reserva(generarCodigoReserva(), titular, apartamento, canal,
                estancia, horaEstimadaLlegada);
        for (Ocupante ocupante : ocupantes) {
            reserva.agregarOcupante(ocupante);
        }

        // Liquidación noche por noche para conocer el valor de la estancia.
        double total = liquidacion.totalEstancia(apartamento, estancia, ocupantes);
        reserva.setTotalEstimado(total);
        reserva.getFolio().registrarCargo("Valor de la estancia " + estancia, total);

        // La reserva se guarda en el árbol del apartamento (ordenada por entrada).
        apartamento.getReservas().insertar(reserva);
        reservasGlobales.insertar(reserva);
        titular.registrarEnHistorial(reserva);

        // Las reservas pendientes se ordenan por su hora de vencimiento.
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            pendientesPorVencimiento.encolar(reserva,
                    reserva.getFechaVencimiento().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return reserva;
    }

    /**
     * Crea una reserva a partir de una solicitud del canal externo.
     *
     * <p>El titular de la solicitud se registra como primer ocupante y, si el grupo
     * es más grande, se añaden los acompañantes declarados por el canal. La reserva
     * externa no exige anticipo: el pago se hace contra entrega, por lo que queda
     * <b>Confirmada</b> de una vez.</p>
     *
     * @throws ReservaSolapadaException si la solicitud choca con una reserva vigente
     */
    public Reserva crearReservaDesdeSolicitud(SolicitudExterna solicitud, Huesped titular,
                                               Apartamento apartamento) {
        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        LocalDate fechaNacimiento = titular != null && titular.getFechaNacimiento() != null
                ? titular.getFechaNacimiento()
                : LocalDate.now().minusYears(30);
        ocupantes.agregar(new Ocupante(titular == null ? solicitud.getNombreSolicitante()
                : titular.getNombre(), fechaNacimiento, true));
        for (int i = 1; i < solicitud.getNumeroPersonas(); i++) {
            ocupantes.agregar(new Ocupante("Acompañante " + i, fechaNacimiento));
        }
        Reserva reserva = crearReserva(titular, apartamento, CanalReserva.EXTERNA,
                solicitud.getEstancia(), ocupantes, LocalTime.of(15, 0));
        // La reserva externa no requiere anticipo: el pago se hace contra entrega.
        reserva.cambiarEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    private void validarApartamentoOperativo(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento es obligatorio.");
        }
        if (apartamento.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) {
            throw new TransicionEstadoInvalidaException(
                    "El apartamento " + apartamento.getCodigo() + " está fuera de servicio",
                    "reservar");
        }
    }

    private void validarCapacidad(Apartamento apartamento, ListaEnlazada<Ocupante> ocupantes) {
        int personas = ocupantes == null ? 0 : ocupantes.tamano();
        if (personas <= 0) {
            throw new CapacidadExcedidaException(
                    "Debe registrarse al menos un ocupante en la reserva.");
        }
        if (personas > apartamento.getCapacidadMaxima()) {
            throw new CapacidadExcedidaException(apartamento.getCapacidadMaxima(), personas);
        }
    }

    private void validarSinSolapamiento(Apartamento apartamento, Estancia estancia) {
        if (estancia == null) {
            throw new FechasInvalidasException("La estancia es obligatoria.");
        }
        if (estancia.numeroNoches() <= 0) {
            throw new FechasInvalidasException(estancia.getFechaEntrada().toString(),
                    estancia.getFechaSalida().toString());
        }
        Reserva enConflicto = buscarSolapamiento(apartamento, estancia);
        if (enConflicto != null) {
            throw new ReservaSolapadaException(apartamento.getCodigo(),
                    "La reserva " + enConflicto.getCodigo() + " ocupa del "
                            + enConflicto.getEstancia().getFechaEntrada() + " al "
                            + enConflicto.getEstancia().getFechaSalida() + ".");
        }
    }

    // ------------------------------------------------------------------
    // Detección de solapamientos (recursiva sobre el árbol del apartamento)
    // ------------------------------------------------------------------

    /**
     * Busca, en el árbol de reservas del apartamento, una reserva vigente que
     * comparta al menos una noche con la estancia indicada.
     *
     * <p>El recorrido es <b>recursivo</b> y aprovecha el orden del árbol (las
     * reservas están ordenadas por fecha de entrada) para <b>podar</b>: cuando la
     * fecha de entrada de un nodo es posterior o igual a la fecha de salida de la
     * nueva estancia, todo su subárbol derecho tiene entradas posteriores y no puede
     * contener solapamientos, por lo que esa rama no se visita.</p>
     *
     * @return la reserva en conflicto o {@code null} si no hay conflicto
     */
    public Reserva buscarSolapamiento(Apartamento apartamento, Estancia estancia) {
        return buscarSolapamiento(apartamento.getReservas().getRaiz(), estancia);
    }

    private Reserva buscarSolapamiento(ArbolBinarioBusqueda.Nodo<Reserva> nodo, Estancia estancia) {
        if (nodo == null) {
            return null;
        }
        Reserva actual = nodo.getClave();
        if (actual.estaVigente() && actual.getEstancia().seSolapa(estancia)) {
            return actual;
        }
        // Poda del subárbol derecho: sus entradas son posteriores a la de este nodo.
        if (!actual.getEstancia().getFechaEntrada().isBefore(estancia.getFechaSalida())) {
            return buscarSolapamiento(nodo.getIzquierdo(), estancia);
        }
        Reserva enIzquierda = buscarSolapamiento(nodo.getIzquierdo(), estancia);
        if (enIzquierda != null) {
            return enIzquierda;
        }
        return buscarSolapamiento(nodo.getDerecho(), estancia);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida de la reserva
    // ------------------------------------------------------------------

    /**
     * Registra el pago del anticipo. Si el monto cubre el 30 % exigido, la reserva
     * pasa a Confirmada.
     */
    public Reserva registrarAnticipo(String codigoReserva, double monto) {
        Reserva reserva = buscarReserva(codigoReserva);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    reserva.getEstado().getDescripcion(), "registrar anticipo");
        }
        // Reserva.registrarAnticipo ya cambia a Confirmada cuando el monto cubre
        // el 30 % exigido, por lo que no hace falta confirmar de nuevo.
        reserva.registrarAnticipo(monto);
        return reserva;
    }

    /**
     * Confirma una reserva pendiente.
     */
    public Reserva confirmarReserva(String codigoReserva) {
        Reserva reserva = buscarReserva(codigoReserva);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    reserva.getEstado().getDescripcion(), "Confirmada");
        }
        if (reserva.getAnticipoPagado() + 0.01 < reserva.anticipoRequerido()) {
            throw new TransicionEstadoInvalidaException("Pendiente sin anticipo suficiente",
                    "Confirmada");
        }
        reserva.cambiarEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    /**
     * Cancela una reserva y libera sus noches.
     */
    public Reserva cancelarReserva(String codigoReserva, String motivo) {
        Reserva reserva = buscarReserva(codigoReserva);
        double devolucion = reserva.montoDevolucion();
        reserva.cerrar(EstadoReserva.CANCELADA, motivo);
        if (devolucion > 0) {
            reserva.getFolio().registrarAbono("Devolución por cancelación de " + codigoReserva,
                    devolucion);
        }
        return reserva;
    }

    /**
     * Registra que el grupo no llegó (no-show).
     */
    public Reserva registrarNoShow(String codigoReserva, String motivo) {
        Reserva reserva = buscarReserva(codigoReserva);
        reserva.cerrar(EstadoReserva.NO_SHOW, motivo);
        return reserva;
    }

    /**
     * Registra el check-in. El apartamento debe estar <b>Preparado</b>.
     *
     * @throws TransicionEstadoInvalidaException si el apartamento no está preparado
     */
    public Reserva registrarCheckIn(String codigoReserva) {
        Reserva reserva = buscarReserva(codigoReserva);
        Apartamento apartamento = reserva.getApartamento();
        if (apartamento.getEstado() != EstadoApartamento.PREPARADO) {
            throw new TransicionEstadoInvalidaException(
                    apartamento.getEstado().getDescripcion(), "Ocupado");
        }
        reserva.cambiarEstado(EstadoReserva.EN_CURSO);
        apartamento.cambiarEstado(EstadoApartamento.OCUPADO);
        return reserva;
    }

    /**
     * Registra el check-out. El apartamento queda <b>Pendiente de preparación</b>,
     * por lo que no puede recibir un nuevo grupo hasta que quede Preparado.
     */
    public Reserva registrarCheckOut(String codigoReserva) {
        Reserva reserva = buscarReserva(codigoReserva);
        if (reserva.getEstado() != EstadoReserva.EN_CURSO) {
            throw new TransicionEstadoInvalidaException(
                    reserva.getEstado().getDescripcion(), "Finalizada");
        }
        reserva.cambiarEstado(EstadoReserva.FINALIZADA);
        reserva.getApartamento().cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION);
        return reserva;
    }

    /**
     * Busca una reserva por su código.
     *
     * @throws ElementoNoEncontradoException si no existe
     */
    public Reserva buscarReserva(String codigoReserva) {
        Reserva reserva = buscarReservaRecursivo(reservasGlobales.getRaiz(), codigoReserva);
        if (reserva == null) {
            throw new ElementoNoEncontradoException("La reserva", codigoReserva);
        }
        return reserva;
    }

    private Reserva buscarReservaRecursivo(ArbolBinarioBusqueda.Nodo<Reserva> nodo, String codigo) {
        if (nodo == null) {
            return null;
        }
        if (nodo.getClave().getCodigo().equals(codigo)) {
            return nodo.getClave();
        }
        Reserva enIzquierda = buscarReservaRecursivo(nodo.getIzquierdo(), codigo);
        if (enIzquierda != null) {
            return enIzquierda;
        }
        return buscarReservaRecursivo(nodo.getDerecho(), codigo);
    }

    /** Todas las reservas del alojamiento, ordenadas por fecha de entrada. */
    public ListaEnlazada<Reserva> todasReservas() {
        return reservasGlobales.recorridoInOrden();
    }

    /** Reservas en estado Pendiente, ordenadas por hora de vencimiento. */
    public ListaEnlazada<Reserva> reservasPendientes() {
        ListaEnlazada<Reserva> pendientes = new ListaEnlazada<>();
        for (Reserva reserva : reservasGlobales) {
            if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
                pendientes.agregar(reserva);
            }
        }
        pendientes.ordenar(new Comparadores.PorVencimiento());
        return pendientes;
    }

    /**
     * Procesa los vencimientos: cancela las reservas Pendientes que ya superaron su
     * plazo de 24 horas y libera sus noches.
     *
     * <p>La cola de prioridad ordena por hora de vencimiento, de modo que solo hay
     * que revisar la cabeza: si la reserva de la cabeza no ha vencido, ninguna de las
     * demás lo ha hecho.</p>
     *
     * @return las reservas que se cancelaron por vencimiento
     */
    public ListaEnlazada<Reserva> procesarVencimientos(LocalDateTime ahora) {
        ListaEnlazada<Reserva> canceladas = new ListaEnlazada<>();
        while (!pendientesPorVencimiento.estaVacia()) {
            Reserva reserva = pendientesPorVencimiento.verFrente();
            if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
                // Ya no está pendiente: se descarta de la cola.
                pendientesPorVencimiento.desencolar();
                continue;
            }
            if (!reserva.estaVencida(ahora)) {
                break;
            }
            pendientesPorVencimiento.desencolar();
            reserva.cerrar(EstadoReserva.CANCELADA, "Vencimiento automático del plazo de pago");
            canceladas.agregar(reserva);
        }
        return canceladas;
    }

    // ------------------------------------------------------------------
    // Búsqueda de disponibilidad
    // ------------------------------------------------------------------

    /**
     * Devuelve los apartamentos disponibles para un rango de fechas y un número de
     * personas, ordenados según el criterio indicado.
     *
     * @param estancia   rango solicitado
     * @param personas  número de personas del grupo
     * @param orden      criterio de ordenamiento (precio, capacidad, dormitorios, etc.)
     */
    public ListaEnlazada<Apartamento> apartamentosDisponibles(Estancia estancia, int personas,
                                                              java.util.Comparator<Apartamento> orden) {
        ListaEnlazada<Apartamento> disponibles = new ListaEnlazada<>();
        for (Apartamento apartamento : catalogoApartamentos) {
            if (apartamento.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) {
                continue;
            }
            if (apartamento.getCapacidadMaxima() < personas) {
                continue;
            }
            if (buscarSolapamiento(apartamento, estancia) != null) {
                continue;
            }
            disponibles.agregar(apartamento);
        }
        if (orden != null) {
            disponibles.ordenar(orden);
        }
        return disponibles;
    }

    /** Apartamentos disponibles ordenados por precio de la temporada indicada. */
    public ListaEnlazada<Apartamento> apartamentosDisponiblesPorPrecio(Estancia estancia,
                                                                        int personas,
                                                                        Temporada temporada) {
        return apartamentosDisponibles(estancia, personas, new Comparadores.PorPrecio(temporada));
    }

    /** {@code true} si el apartamento está libre en el rango indicado. */
    public boolean estaDisponible(Apartamento apartamento, Estancia estancia) {
        return apartamento.getEstado() != EstadoApartamento.FUERA_DE_SERVICIO
                && buscarSolapamiento(apartamento, estancia) == null;
    }

    /** Genera el siguiente código de reserva correlativo. */
    private String generarCodigoReserva() {
        contadorReservas++;
        return String.format("R-%04d", contadorReservas);
    }

    public CalendarioTemporadas getCalendario() {
        return calendario;
    }
}
