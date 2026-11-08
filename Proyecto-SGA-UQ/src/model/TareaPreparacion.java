package model;

import java.time.LocalDateTime;

/**
 * Tarea de preparación de un apartamento.
 *
 * Cuando un grupo sale, el apartamento queda Pendiente de preparación y se
 * crea una tarea que entra en la cola de prioridad inteligente:
 *   Prioridad 1: el apartamento tiene una entrada programada para hoy.
 *   Prioridad 2: no tiene entrada hoy.
 *Dentro de la prioridad 1, las tareas se ordenan por la hora estimada de llegada
 * del siguiente grupo.
 */
public class TareaPreparacion {

    private final Apartamento apartamento;
    private final boolean entradaHoy;
    private final java.time.LocalTime horaEstimadaLlegada;
    private final LocalDateTime desdeCuandoPendiente;
    private final String reservaEntrante;

    private LocalDateTime inicioPreparacion;
    private LocalDateTime finPreparacion;

    public TareaPreparacion(Apartamento apartamento, boolean entradaHoy,
                            java.time.LocalTime horaEstimadaLlegada,
                            LocalDateTime desdeCuandoPendiente, String reservaEntrante) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento de la tarea es obligatorio.");
        }
        this.apartamento = apartamento;
        this.entradaHoy = entradaHoy;
        this.horaEstimadaLlegada = horaEstimadaLlegada == null
                ? java.time.LocalTime.MAX : horaEstimadaLlegada;
        this.desdeCuandoPendiente = desdeCuandoPendiente == null
                ? LocalDateTime.now() : desdeCuandoPendiente;
        this.reservaEntrante = reservaEntrante;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public boolean isEntradaHoy() {
        return entradaHoy;
    }

    public java.time.LocalTime getHoraEstimadaLlegada() {
        return horaEstimadaLlegada;
    }

    public LocalDateTime getDesdeCuandoPendiente() {
        return desdeCuandoPendiente;
    }

    public String getReservaEntrante() {
        return reservaEntrante;
    }

    public LocalDateTime getInicioPreparacion() {
        return inicioPreparacion;
    }

    public LocalDateTime getFinPreparacion() {
        return finPreparacion;
    }

    /** Prioridad de la cola: 1 si tiene entrada hoy, 2 en caso contrario. */
    public int getPrioridad() {
        return entradaHoy ? 1 : 2;
    }

    /** Marca el inicio de la preparación. */
    public void iniciar() {
        this.inicioPreparacion = LocalDateTime.now();
    }

    /** Marca el fin de la preparación y devuelve los segundos que tardó. */
    public long finalizar() {
        this.finPreparacion = LocalDateTime.now();
        if (inicioPreparacion == null) {
            return 0;
        }
        return java.time.Duration.between(inicioPreparacion, finPreparacion).getSeconds();
    }

    /** Duración de la preparación en segundos, o -1 si no ha terminado. */
    public long duracionSegundos() {
        if (inicioPreparacion == null || finPreparacion == null) {
            return -1;
        }
        return java.time.Duration.between(inicioPreparacion, finPreparacion).getSeconds();
    }

    @Override
    public String toString() {
        return "Preparar " + apartamento.getCodigo()
                + (entradaHoy ? " (entrada hoy " + horaEstimadaLlegada + ")" : " (sin entrada hoy)");
    }
}
