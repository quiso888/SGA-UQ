package model;

import java.util.Comparator;

/**
 * Colección de comparadores alternativos exigidos por el enunciado.
 *
 * Se implementan al menos cuatro:
 *   {PorPrecio} — apartamentos por precio de la temporada.
 *   {PorCapacidad} — apartamentos por capacidad máxima.
 *   {PorNumeroReservas} — apartamentos por número de reservas registradas.
 *   {PrioridadPreparacion} — prioridad de la cola de preparación, que
 *       combina comparadores con {thenComparing}: primero la entrada de
 *       hoy, luego la hora estimada de llegada y por último el tiempo que el
 *       apartamento lleva pendiente
 *
 * Se añade { PorNumeroDormitorios} como criterio adicional de búsqueda de
 * disponibilidad.
 */
public final class Comparadores {

    private Comparadores() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Ordena apartamentos por precio por persona y noche de una temporada.
     * Del más barato al más caro.
     */
    public static class PorPrecio implements Comparator<Apartamento> {

        private final Temporada temporada;

        public PorPrecio() {
            this(Temporada.BAJA);
        }

        public PorPrecio(Temporada temporada) {
            this.temporada = temporada == null ? Temporada.BAJA : temporada;
        }

        public Temporada getTemporada() {
            return temporada;
        }

        @Override
        public int compare(Apartamento a, Apartamento b) {
            double precioA = a.precioPorPersona(temporada);
            double precioB = b.precioPorPersona(temporada);
            int resultado = Double.compare(precioA, precioB);
            if (resultado != 0) {
                return resultado;
            }
            // Desempate estable por código.
            return a.getCodigo().compareToIgnoreCase(b.getCodigo());
        }
    }

    /**
     * Ordena apartamentos por capacidad máxima, de mayor a menor.
     */
    public static class PorCapacidad implements Comparator<Apartamento> {

        @Override
        public int compare(Apartamento a, Apartamento b) {
            int resultado = Integer.compare(b.getCapacidadMaxima(), a.getCapacidadMaxima());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareToIgnoreCase(b.getCodigo());
        }
    }

    /**
     * Ordena apartamentos por número de dormitorios, de mayor a menor.
     */
    public static class PorNumeroDormitorios implements Comparator<Apartamento> {

        @Override
        public int compare(Apartamento a, Apartamento b) {
            int resultado = Integer.compare(b.getNumeroDormitorios(), a.getNumeroDormitorios());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareToIgnoreCase(b.getCodigo());
        }
    }

    /**
     * Ordena apartamentos por número de reservas registradas, de mayor a menor.
     */
    public static class PorNumeroReservas implements Comparator<Apartamento> {

        @Override
        public int compare(Apartamento a, Apartamento b) {
            int resultado = Integer.compare(b.numeroReservas(), a.numeroReservas());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareToIgnoreCase(b.getCodigo());
        }
    }

    /**
     * Ordena las tareas de preparación por prioridad de la cola inteligente.
     *
     * <p>Combina tres comparadores con {@code thenComparing}:</p>
     * <ol>
     *   <li>prioridad (entrada hoy = 1, resto = 2),</li>
     *   <li>hora estimada de llegada del siguiente grupo,</li>
     *   <li>antigüedad de la tarea (la más antigua primero).</li>
     * </ol>
     */
    public static class PrioridadPreparacion implements Comparator<TareaPreparacion> {

        @Override
        public int compare(TareaPreparacion a, TareaPreparacion b) {
            return Comparator.comparingInt(TareaPreparacion::getPrioridad)
                    .thenComparing(TareaPreparacion::getHoraEstimadaLlegada)
                    .thenComparing(TareaPreparacion::getDesdeCuandoPendiente)
                    .thenComparing(tarea -> tarea.getApartamento().getCodigo())
                    .compare(a, b);
        }
    }

    /**
     * Ordena reservas por su hora estimada de llegada, de la más temprana a la más
     * tardía. Se usa para ordenar las reservas del día.
     */
    public static class PorHoraDeLlegada implements Comparator<Reserva> {

        @Override
        public int compare(Reserva a, Reserva b) {
            int resultado = a.getHoraEstimadaLlegada().compareTo(b.getHoraEstimadaLlegada());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareTo(b.getCodigo());
        }
    }

    /**
     * Ordena huéspedes por nombre, de forma alfabética.
     */
    public static class PorNombre implements Comparator<Huesped> {

        @Override
        public int compare(Huesped a, Huesped b) {
            return a.getNombre().compareToIgnoreCase(b.getNombre());
        }
    }

    /**
     * Ordena reservas <b>Pendientes</b> por su hora de vencimiento (24 horas después
     * de la creación). La más próxima a vencer queda primero.
     */
    public static class PorVencimiento implements Comparator<Reserva> {

        @Override
        public int compare(Reserva a, Reserva b) {
            int resultado = a.getFechaVencimiento().compareTo(b.getFechaVencimiento());
            if (resultado != 0) {
                return resultado;
            }
            return a.getCodigo().compareTo(b.getCodigo());
        }
    }
}
