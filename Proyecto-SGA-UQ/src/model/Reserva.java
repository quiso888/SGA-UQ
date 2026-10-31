package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import datastructures.ListaDobleEnlazada;
import exceptions.TransicionEstadoInvalidaException;

/**
 * Reserva de una estancia en un apartamento.
 *
 * Implementa {@link Comparable} con orden natural por fecha de entrada (y
 * código como desempate), lo que permite almacenar las reservas de cada apartamento
 * en un árbol binario ordenado y detectar solapamientos de forma recursiva.
 *
 * Ciclo de vida: {@code Pendiente → Confirmada → En curso → Finalizada}, con salidas
 * alternativas a {@code Cancelada} o {@code No-show}.
 */
public class Reserva implements Comparable<Reserva> {

    /** Porcentaje del anticipo exigido a las reservas del portal. */
    public static final double PORCENTAJE_ANTICIPO = 0.30;

    /** Horas que tiene una reserva Pendiente antes de vencer automáticamente. */
    public static final int HORAS_VENCIMIENTO = 24;

    private final String codigo;
    private final Huesped titular;
    private final Apartamento apartamento;
    private final CanalReserva canal;
    private final Estancia estancia;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaVencimiento;
    private final LocalTime horaEstimadaLlegada;
    private final ListaDobleEnlazada<Ocupante> ocupantes = new ListaDobleEnlazada<>();
    private final Folio folio;

    private EstadoReserva estado;
    private double totalEstimado;
    private double anticipoPagado;
    private String motivoCierre;

    public Reserva(String codigo, Huesped titular, Apartamento apartamento, CanalReserva canal,
                   Estancia estancia, LocalTime horaEstimadaLlegada) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de la reserva es obligatorio.");
        }
        if (titular == null || apartamento == null || canal == null || estancia == null) {
            throw new IllegalArgumentException(
                    "El titular, el apartamento, el canal y la estancia son obligatorios.");
        }
        this.codigo = codigo.trim();
        this.titular = titular;
        this.apartamento = apartamento;
        this.canal = canal;
        this.estancia = estancia;
        this.horaEstimadaLlegada = horaEstimadaLlegada == null ? LocalTime.of(15, 0) : horaEstimadaLlegada;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaVencimiento = fechaCreacion.plusHours(HORAS_VENCIMIENTO);
        this.estado = EstadoReserva.PENDIENTE;
        this.folio = new Folio("F-" + codigo, titular);
        this.totalEstimado = 0;
    }

    public String getCodigo() {
        return codigo;
    }

    public Huesped getTitular() {
        return titular;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public CanalReserva getCanal() {
        return canal;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalTime getHoraEstimadaLlegada() {
        return horaEstimadaLlegada;
    }

    public ListaDobleEnlazada<Ocupante> getOcupantes() {
        return ocupantes;
    }

    public Folio getFolio() {
        return folio;
    }

    public double getTotalEstimado() {
        return totalEstimado;
    }

    public void setTotalEstimado(double totalEstimado) {
        this.totalEstimado = totalEstimado;
    }

    public double getAnticipoPagado() {
        return anticipoPagado;
    }

    public String getMotivoCierre() {
        return motivoCierre;
    }

    /** Agrega un ocupante a la reserva. */
    public void agregarOcupante(Ocupante ocupante) {
        if (ocupante != null) {
            ocupantes.agregar(ocupante);
        }
    }

    /** Número de personas de la reserva (ocupantes registrados). */
    public int numeroPersonas() {
        return ocupantes.tamano();
    }

    /**
     * Número de personas que pagan en una noche concreta: los menores de seis años
     * ocupan cupo pero no pagan.
     */
    public int personasQuePagan(LocalDate noche) {
        int pagadoras = 0;
        for (Ocupante ocupante : ocupantes) {
            if (ocupante.pagaEn(noche)) {
                pagadoras++;
            }
        }
        return pagadoras;
    }

    /** Anticipo exigido para confirmar la reserva (30 % del total estimado). */
    public double anticipoRequerido() {
        return totalEstimado * PORCENTAJE_ANTICIPO;
    }

    /**
     * Monto que se devolvería si se cancela en este momento: el anticipo completo
     * si la cancelación es antes de la fecha de entrada y nada el mismo día del
     * ingreso o después.
     */
    public double montoDevolucion() {
        if (anticipoPagado <= 0) {
            return 0;
        }
        return LocalDate.now().isBefore(estancia.getFechaEntrada()) ? anticipoPagado : 0;
    }

    /** {@code true} si la reserva sigue ocupando noches del apartamento. */
    public boolean estaVigente() {
        return estado.estaActiva();
    }

    /** {@code true} si la reserva está Pendiente y ya pasó su hora de vencimiento. */
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoReserva.PENDIENTE && ahora.isAfter(fechaVencimiento);
    }

    /**
     * Registra un pago de anticipo y confirma la reserva si cubre el monto exigido.
     */
    public void registrarAnticipo(double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto del anticipo debe ser positivo.");
        }
        this.anticipoPagado += monto;
        folio.registrarPago("Anticipo de la reserva " + codigo, monto);
        if (anticipoPagado + 0.01 >= anticipoRequerido() && estado == EstadoReserva.PENDIENTE) {
            cambiarEstado(EstadoReserva.CONFIRMADA);
        }
    }

    /**
     * Cambia el estado de la reserva validando la transición.
     *
     * @throws TransicionEstadoInvalidaException si la transición no está permitida
     */
    public void cambiarEstado(EstadoReserva nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }
        if (!estado.puedeTransicionarA(nuevo)) {
            throw new TransicionEstadoInvalidaException(estado.getDescripcion(), nuevo.getDescripcion());
        }
        this.estado = nuevo;
    }

    /**
     * Cierra la reserva como cancelada o no-show registrando el motivo.
     *
     * @throws TransicionEstadoInvalidaException si el estado actual no permite cerrarla
     */
    public void cerrar(EstadoReserva estadoCierre, String motivo) {
        if (estadoCierre != EstadoReserva.CANCELADA && estadoCierre != EstadoReserva.NO_SHOW) {
            throw new IllegalArgumentException(
                    "Solo se puede cerrar una reserva como Cancelada o No-show.");
        }
        cambiarEstado(estadoCierre);
        this.motivoCierre = motivo;
    }

    /** {@code true} si la reserva se cruza con la estancia indicada. */
    public boolean seSolapaCon(Estancia otra) {
        return estancia.seSolapa(otra);
    }

    /**
     * Orden natural de la reserva: por fecha de entrada y, en caso de empate, por
     * código. El desempate es necesario para que el árbol binario no descarte dos
     * reservas históricas que empezaron el mismo día.
     */
    @Override
    public int compareTo(Reserva otra) {
        int comparacion = this.estancia.getFechaEntrada().compareTo(otra.estancia.getFechaEntrada());
        if (comparacion != 0) {
            return comparacion;
        }
        return this.codigo.compareTo(otra.codigo);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Reserva)) {
            return false;
        }
        return codigo.equals(((Reserva) objeto).codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return codigo + " · " + apartamento.getCodigo() + " · " + estancia + " · " + estado.getDescripcion();
    }
}
