package exceptions;

/**
 * Se lanza al intentar extraer un elemento de una estructura propia que no tiene
 * elementos: desencolar una cola vacía, desapilar una pila vacía, obtener el mínimo
 * de un árbol vacío, etc.
 */
public class EstructuraVaciaException extends SgaException {

    private static final long serialVersionUID = 1L;

    private final String nombreEstructura;

    public EstructuraVaciaException(String nombreEstructura) {
        super("La estructura \"" + nombreEstructura + "\" está vacía: no se puede "
                + "extraer ningún elemento.");
        this.nombreEstructura = nombreEstructura;
    }

    public EstructuraVaciaException(String nombreEstructura, String operacion) {
        super("No se puede " + operacion + " en la estructura \"" + nombreEstructura
                + "\" porque está vacía.");
        this.nombreEstructura = nombreEstructura;
    }

    public String getNombreEstructura() {
        return nombreEstructura;
    }
}
