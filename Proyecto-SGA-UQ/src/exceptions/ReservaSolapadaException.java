package exceptions;

/**
 * Se lanza cuando se intenta crear o confirmar una reserva que comparte al menos
 * una noche con otra reserva activa del mismo apartamento.
 *
 * <p>Regla fundamental del SGA: un apartamento no puede tener dos reservas activas
 * que compartan al menos una noche, sin importar el canal de origen.</p>
 */
public class ReservaSolapadaException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final String codigoApartamento;
    private final String detalle;

    public ReservaSolapadaException(String codigoApartamento, String detalle) {
        super("El apartamento " + codigoApartamento + " ya tiene una reserva activa que "
                + "comparte al menos una noche con la estancia solicitada. " + detalle);
        this.codigoApartamento = codigoApartamento;
        this.detalle = detalle;
    }

    public String getCodigoApartamento() {
        return codigoApartamento;
    }

    public String getDetalle() {
        return detalle;
    }
}
