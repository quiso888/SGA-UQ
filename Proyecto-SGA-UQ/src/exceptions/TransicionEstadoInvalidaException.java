package exceptions;

/**
 * Se lanza cuando se intenta ejecutar una transición de estado que no está permitida
 * por la máquina de estados de la reserva o del apartamento.
 *
 * <p>Ejemplos: confirmar una reserva que ya está cancelada, pasar un apartamento de
 * "Preparado" a "Ocupado" sin check-in, o sacar de servicio un apartamento que
 * tiene una estancia en curso.</p>
 */
public class TransicionEstadoInvalidaException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final String estadoActual;
    private final String estadoDestino;

    public TransicionEstadoInvalidaException(String estadoActual, String estadoDestino) {
        super("Transición de estado inválida: no se puede pasar de \"" + estadoActual
                + "\" a \"" + estadoDestino + "\".");
        this.estadoActual = estadoActual;
        this.estadoDestino = estadoDestino;
    }

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
        this.estadoActual = "";
        this.estadoDestino = "";
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }
}
