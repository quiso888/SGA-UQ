package exceptions;

/**
 * Se lanza cuando la búsqueda con backtracking no encuentra ninguna combinación
 * de apartamentos able para alojar a un grupo grande.
 */
public class SinAlojamientoDisponibleException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final int personas;

    public SinAlojamientoDisponibleException(int personas) {
        super("No existe una combinación de apartamentos disponibles capaz de alojar "
                + "a un grupo de " + personas + " personas en el rango solicitado.");
        this.personas = personas;
    }

    public int getPersonas() {
        return personas;
    }
}
