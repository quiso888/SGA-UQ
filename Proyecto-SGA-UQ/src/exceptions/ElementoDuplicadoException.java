package exceptions;

/**
 * Se lanza cuando se intenta insertar un elemento que ya existe en una estructura
 * que no admite duplicados, por ejemplo el conjunto de apartamentos favoritos de
 * un huésped o el conjunto de documentos de huéspedes registrados.
 */
public class ElementoDuplicadoException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final String valor;

    public ElementoDuplicadoException(String descripcion, Object valor) {
        super(descripcion + " ya se encuentra registrado: " + valor);
        this.valor = String.valueOf(valor);
    }

    public String getValor() {
        return valor;
    }
}
