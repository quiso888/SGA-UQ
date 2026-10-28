package exceptions;

/**
 * Excepción base de todo el Sistema de Gestión de Alojamiento (SGA).
 *
 * <p>Todas las excepciones propias del sistema heredan de esta clase, de modo que la
 * GUI o cualquier capa superior pueda capturar de forma genérica cualquier fallo
 * originado por las reglas de negocio del alojamiento.</p>
 *
 * <p>Se declara como {@code RuntimeException} (no verificada) para no obligar a que
 * cada camino del flujo de la GUI declare la captura; no obstante, la GUI realiza
 * capturas explícitas de las excepciones concretas de dominio
 * ({@link ReservaSolapadaException}, {@link CapacidadExcedidaException},
 * {@link FechasInvalidasException}, {@link TransicionEstadoInvalidaException},
 * {@link EstructuraVaciaException}) para mostrar mensajes descriptivos al usuario.</p>
 */
public class SgaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SgaException(String mensaje) {
        super(mensaje);
    }

    public SgaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
