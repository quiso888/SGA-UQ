package service;

import datastructures.Cola;
import datastructures.ListaEnlazada;
import exceptions.CapacidadExcedidaException;
import exceptions.ElementoNoEncontradoException;
import exceptions.FechasInvalidasException;
import exceptions.ReservaSolapadaException;
import model.*;

import java.time.LocalDateTime;

/**
 * Procesador del canal externo (plataforma de terceros).
 *
 * <p>Las solicitudes llegan a una <b>cola FIFO</b> ({@link Cola}) y se procesan una
 * a una, respetando el orden de llegada. La regla del alojamiento se aplica igual
 * que en los demás canales: si la solicitud choca con una reserva vigente del mismo
 * apartamento, <b>se rechaza</b> y el hecho queda registrado como
 * {@link ConflictoCanal} para los reportes de la interfaz.</p>
 *
 * <p>El canal externo no exige anticipo: las reservas que crea quedan
 * <b>Confirmadas</b> de una vez.</p>
 */
public class ProcesadorCanalExterno {

    /** Cola de solicitudes pendientes, en orden de llegada. */
    private final Cola<SolicitudExterna> cola = new Cola<>();

    /** Conflictos detectados al procesar las solicitudes. */
    private final ListaEnlazada<ConflictoCanal> conflictos = new ListaEnlazada<>();

    /** Solicitudes ya procesadas (aceptadas o rechazadas). */
    private final ListaEnlazada<SolicitudExterna> procesadas = new ListaEnlazada<>();

    private final GestorReservas gestorReservas;

    public ProcesadorCanalExterno(GestorReservas gestorReservas) {
        this.gestorReservas = gestorReservas;
    }

    // ------------------------------------------------------------------
    // Entrada de solicitudes
    // ------------------------------------------------------------------

    /**
     * Encola una solicitud recibida de la plataforma externa.
     */
    public void encolarSolicitud(SolicitudExterna solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria.");
        }
        cola.encolar(solicitud);
    }

    /** Solicitudes que esperan ser procesadas. */
    public ListaEnlazada<SolicitudExterna> solicitudesPendientes() {
        ListaEnlazada<SolicitudExterna> pendientes = new ListaEnlazada<>();
        for (SolicitudExterna solicitud : cola) {
            pendientes.agregar(solicitud);
        }
        return pendientes;
    }

    public int numeroSolicitudesPendientes() {
        return cola.tamano();
    }

    public boolean haySolicitudesPendientes() {
        return !cola.estaVacia();
    }

    // ------------------------------------------------------------------
    // Procesamiento
    // ------------------------------------------------------------------

    /**
     * Procesa <b>una</b> solicitud: la primera de la cola FIFO.
     *
     * @return la reserva creada, o {@code null} si la solicitud fue rechazada
     * @throws ElementoNoEncontradoException si el apartamento o el huésped no existen
     */
    public Reserva procesarSiguiente() {
        if (cola.estaVacia()) {
            return null;
        }
        SolicitudExterna solicitud = cola.desencolar();
        return procesar(solicitud);
    }

    /**
     * Procesa todas las solicitudes pendientes, en orden de llegada.
     *
     * @return las reservas creadas
     */
    public ListaEnlazada<Reserva> procesarTodas() {
        ListaEnlazada<Reserva> creadas = new ListaEnlazada<>();
        while (!cola.estaVacia()) {
            Reserva reserva = procesarSiguiente();
            if (reserva != null) {
                creadas.agregar(reserva);
            }
        }
        return creadas;
    }

    /**
     * Procesa una solicitud concreta.
     *
     * <p>Si la solicitud choca con una reserva vigente, se <b>rechaza</b> y se
     * registra el conflicto de canal; en ese caso se devuelve {@code null}.</p>
     */
    private Reserva procesar(SolicitudExterna solicitud) {
        Apartamento apartamento = gestorReservas.buscarApartamento(solicitud.getCodigoApartamento());
        Huesped titular = huespedDe(solicitud);
        try {
            Reserva reserva = gestorReservas.crearReservaDesdeSolicitud(solicitud, titular, apartamento);
            solicitud.aceptar(reserva.getCodigo());
            procesadas.agregar(solicitud);
            return reserva;
        } catch (ReservaSolapadaException conflicto) {
            // Se registra el conflicto de canal: la solicitud se rechaza.
            conflictos.agregar(new ConflictoCanal(solicitud.getIdentificador(),
                    solicitud.getCodigoApartamento(), conflicto.getDetalle(),
                    reservaEnConflicto(solicitud), LocalDateTime.now()));
            solicitud.rechazar("Conflicto de disponibilidad: " + conflicto.getDetalle());
            procesadas.agregar(solicitud);
            return null;
        } catch (CapacidadExcedidaException capacidad) {
            conflictos.agregar(new ConflictoCanal(solicitud.getIdentificador(),
                    solicitud.getCodigoApartamento(),
                    "El grupo no cabe en el apartamento: " + capacidad.getMessage(),
                    null, LocalDateTime.now()));
            solicitud.rechazar(capacidad.getMessage());
            procesadas.agregar(solicitud);
            return null;
        } catch (FechasInvalidasException fechas) {
            conflictos.agregar(new ConflictoCanal(solicitud.getIdentificador(),
                    solicitud.getCodigoApartamento(), fechas.getMessage(),
                    null, LocalDateTime.now()));
            solicitud.rechazar(fechas.getMessage());
            procesadas.agregar(solicitud);
            return null;
        }
    }

    /**
     * Localiza el huésped de la solicitud; si no está registrado, se registra
     * automáticamente con los datos que envía el canal.
     */
    private Huesped huespedDe(SolicitudExterna solicitud) {
        Huesped existente = gestorReservas.buscarHuespedOpcional(solicitud.getDocumento());
        if (existente != null) {
            return existente;
        }
        Huesped nuevo = new Huesped(solicitud.getDocumento(),
                solicitud.getNombreSolicitante(), null, solicitud.getTelefono(),
                solicitud.getCorreo());
        gestorReservas.registrarHuesped(nuevo);
        return nuevo;
    }

    /**
     * Busca el código de la reserva vigente que choca con la solicitud, para dejar
     * trazabilidad en el reporte de conflictos.
     */
    private String reservaEnConflicto(SolicitudExterna solicitud) {
        try {
            Apartamento apartamento = gestorReservas.buscarApartamento(solicitud.getCodigoApartamento());
            Reserva conflicto = gestorReservas.buscarSolapamiento(apartamento, solicitud.getEstancia());
            return conflicto == null ? null : conflicto.getCodigo();
        } catch (ElementoNoEncontradoException excepcion) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Reportes
    // ------------------------------------------------------------------

    /** Conflictos de canal detectados. */
    public ListaEnlazada<ConflictoCanal> getConflictos() {
        return conflictos;
    }

    /** Número de conflictos de canal detectados. */
    public int numeroConflictos() {
        return conflictos.tamano();
    }

    /** Solicitudes ya procesadas. */
    public ListaEnlazada<SolicitudExterna> getProcesadas() {
        return procesadas;
    }
}
