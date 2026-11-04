package model;

/**
 * Estados del ciclo de vida de una reserva.
 *
 * Flujo normal: {@code PENDIENTE → CONFIRMADA → EN_CURSO → FINALIZADA}.
 * Flujos alternativos: una reserva puede terminar antes como
 * {@code CANCELADA} o {@code NO_SHOW}.
 */
public enum EstadoReserva {

    /** Creada, todavía sin anticipo (canal portal) o sin registrar por el canal externo. */
    PENDIENTE("Pendiente"),

    /** Anticipo pagado o reserva directa registrada: las noches quedan bloqueadas. */
    CONFIRMADA("Confirmada"),

    /** El grupo hizo check-in: el apartamento pasa a Ocupado. */
    EN_CURSO("En curso"),

    /** La estancia terminó con check-out. */
    FINALIZADA("Finalizada"),

    /** La reserva se canceló: se liberan las noches. */
    CANCELADA("Cancelada"),

    /** El grupo no llegó y se aplicó la política de no-presentación. */
    NO_SHOW("No-show");

    private final String descripcion;

    EstadoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** {@code true} si la reserva sigue occupying noches (no ha terminado). */
    public boolean estaActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }

    /** {@code true} si la reserva ya no ocupa noches. */
    public boolean estaCerrada() {
        return this == FINALIZADA || this == CANCELADA || this == NO_SHOW;
    }

    /** {@code true} si es posible pasar de este estado al estado indicado. */
    public boolean puedeTransicionarA(EstadoReserva destino) {
        switch (this) {
            case PENDIENTE:
                return destino == CONFIRMADA || destino == CANCELADA || destino == NO_SHOW;
            case CONFIRMADA:
                return destino == EN_CURSO || destino == CANCELADA || destino == NO_SHOW;
            case EN_CURSO:
                return destino == FINALIZADA;
            case FINALIZADA:
            case CANCELADA:
            case NO_SHOW:
                return false;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
