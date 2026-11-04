package model;

/**
 * Canales por los que el alojamiento recibe reservas.
 *
 * Los tres canales operan sobre los mismos apartamentos: por eso la regla de no
 * solapamiento se aplica sin importar el canal de origen.
 */
public enum CanalReserva {

    /** El huésped reserva desde la aplicación; queda Pendiente hasta pagar el anticipo. */
    PORTAL("Portal web"),

    /** La recepción registra una reserva recibida por teléfono o de manera presencial. */
    DIRECTA("Recepción directa"),

    /** Plataforma de terceros simulada: sus solicitudes llegan en cola FIFO. */
    EXTERNA("Plataforma externa");

    private final String descripcion;

    CanalReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
