package model;

/**
 * Temporadas comerciales del alojamiento.
 *
 * Baja es la temporada base; Media y Alta tienen tarifas superiores. Una estancia
 * que cruza varias temporadas se liquida noche por noche, de modo que cada noche se
 * cobra con la tarifa de la temporada a la que pertenece.
 */
public enum Temporada {

    /** Temporada base. */
    BAJA("Baja"),

    /** Temporada intermedia. */
    MEDIA("Media"),

    /** Temporada alta. */
    ALTA("Alta");

    private final String descripcion;

    Temporada(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Devuelve la temporada a la que pertenece una fecha, según el calendario
     * definido por el administrador.
     *
     * @param calendario calendario de temporadas del alojamiento
     * @param fecha      fecha a clasificar
     * @return la temporada vigente
     */
    public static Temporada de(CalendarioTemporadas calendario, java.time.LocalDate fecha) {
        return calendario.temporadaDe(fecha);
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
