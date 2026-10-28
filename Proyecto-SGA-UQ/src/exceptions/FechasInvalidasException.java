package exceptions;

/**
 * Se lanza cuando el rango de fechas de una estancia es inválido: la fecha de salida
 * no es posterior a la de entrada, o se intenta operar con fechas nulas.
 */
public class FechasInvalidasException extends SgaException {

    private static final long serialVersionUID = 1L;

    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }

    public FechasInvalidasException(String fechaEntrada, String fechaSalida) {
        super("El rango de fechas es inválido: entrada " + fechaEntrada
                + ", salida " + fechaSalida + ". La fecha de salida debe ser posterior "
                + "a la de entrada y la estancia debe tener al menos una noche.");
    }
}
