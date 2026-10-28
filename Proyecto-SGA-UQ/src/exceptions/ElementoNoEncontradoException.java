package exceptions;

/**
 * Se lanza cuando se busca un elemento que no existe en la estructura consultada:
 * un huésped por documento, un apartamento por código, una reserva por folio, etc.
 */
public class ElementoNoEncontradoException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final String clave;

    public ElementoNoEncontradoException(String descripcion, Object clave) {
        super(descripcion + " no fue encontrado: " + clave);
        this.clave = String.valueOf(clave);
    }

    public String getClave() {
        return clave;
    }
}
