package exceptions;

/**
 * Se lanza cuando el número de personas de una estancia supera la capacidad máxima
 * del apartamento asignado.
 */
public class CapacidadExcedidaException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final int capacidadMaxima;
    private final int solicitadas;

    public CapacidadExcedidaException(int capacidadMaxima, int solicitadas) {
        super("Se solicitan " + solicitadas + " personas pero la capacidad máxima es "
                + capacidadMaxima + ".");
        this.capacidadMaxima = capacidadMaxima;
        this.solicitadas = solicitadas;
    }

    public CapacidadExcedidaException(String mensaje) {
        super(mensaje);
        this.capacidadMaxima = -1;
        this.solicitadas = -1;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public int getSolicitadas() {
        return solicitadas;
    }
}
