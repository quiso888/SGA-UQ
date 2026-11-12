package model;

/**
 * Estados operativos de un apartamento.
 *
 * Colores usados en el tablero de la interfaz gráfica:
 *   Verde: {#PREPARADO}
 *   Azul: {#OCUPADO}
 *   Amarillo: {#PENDIENTE_PREPARACION} y {#EN_PREPARACION}
 *   Rojo: {#FUERA_DE_SERVICIO}
 *
 */
public enum EstadoApartamento {

    /** Limpio y disponible: puede recibir un nuevo grupo. */
    PREPARADO("Preparado", 0x2E7D32),

    /** Tiene un grupo alojado. */
    OCUPADO("Ocupado", 0x1565C0),

    /** Un grupo salió y el apartamento espera limpieza: no puede recibir otro grupo. */
    PENDIENTE_PREPARACION("Pendiente de preparación", 0xF9A825),

    /** El personal está limpiando el apartamento en este momento. */
    EN_PREPARACION("En preparación", 0xFBC02D),

    /** Retirado de la operación; debe tener un motivo registrado. */
    FUERA_DE_SERVICIO("Fuera de servicio", 0xC62828);

    private final String descripcion;
    private final int colorRgb;

    EstadoApartamento(String descripcion, int colorRgb) {
        this.descripcion = descripcion;
        this.colorRgb = colorRgb;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Color representativo en formato RGB, usado por el tablero y el mapa. */
    public int getColorRgb() {
        return colorRgb;
    }

    /** {@code true} si el apartamento admite un nuevo grupo. */
    public boolean admiteHuespedes() {
        return this == PREPARADO;
    }

    /** {@code true} si el apartamento debe pasar por preparación. */
    public boolean requierePreparacion() {
        return this == PENDIENTE_PREPARACION || this == EN_PREPARACION;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
