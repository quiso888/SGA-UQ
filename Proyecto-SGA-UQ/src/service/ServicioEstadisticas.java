package service;

import datastructures.ListaEnlazada;
import model.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Servicio de estadísticas: calcula los indicadores que muestra el panel de
 * estadísticas de la interfaz.
 *
 * <p>Incluye:</p>
 * <ul>
 *   <li><b>Ocupación</b> por fecha, por apartamento y por temporada.</li>
 *   <li><b>Ingresos</b> por canal, por temporada y por periodo (mes).</li>
 *   <li><b>Tasas</b> de cancelación y de no-presentación.</li>
 *   <li><b>Conflictos</b> del canal externo.</li>
 *   <li><b>Tiempo promedio</b> de preparación de los apartamentos.</li>
 * </ul>
 */
public class ServicioEstadisticas {

    private final GestorReservas gestorReservas;
    private final GestorPreparacion gestorPreparacion;
    private final ProcesadorCanalExterno procesadorCanalExterno;

    public ServicioEstadisticas(GestorReservas gestorReservas,
                                GestorPreparacion gestorPreparacion,
                                ProcesadorCanalExterno procesadorCanalExterno) {
        this.gestorReservas = gestorReservas;
        this.gestorPreparacion = gestorPreparacion;
        this.procesadorCanalExterno = procesadorCanalExterno;
    }

    // ------------------------------------------------------------------
    // Ocupación
    // ------------------------------------------------------------------

    /**
     * Apartments ocupados en una fecha concreta (tienen una reserva activa que
     * incluye esa noche).
     */
    public ListaEnlazada<Apartamento> apartamentosOcupados(LocalDate fecha) {
        ListaEnlazada<Apartamento> ocupados = new ListaEnlazada<>();
        for (Apartamento apartamento : gestorReservas.todosApartamentos()) {
            if (apartamento.getEstado() == EstadoApartamento.OCUPADO) {
                ocupados.agregar(apartamento);
                continue;
            }
            for (Reserva reserva : apartamento.getReservas()) {
                if (reserva.estaVigente() && reserva.getEstancia().ocupaNoche(fecha)) {
                    ocupados.agregar(apartamento);
                    break;
                }
            }
        }
        return ocupados;
    }

    /**
     * Porcentaje de ocupación de una fecha (0 a 100).
     */
    public double porcentajeOcupacion(LocalDate fecha) {
        int total = gestorReservas.todosApartamentos().tamano();
        if (total == 0) {
            return 0;
        }
        return (apartamentosOcupados(fecha).tamano() * 100.0) / total;
    }

    /**
     * Número de apartamentos por estado operativo.
     *
     * @return arreglo con el conteo por estado (mismo orden que
     *         {@link EstadoApartamento#values()})
     */
    public int[] apartamentosPorEstado() {
        int[] conteo = new int[EstadoApartamento.values().length];
        for (Apartamento apartamento : gestorReservas.todosApartamentos()) {
            conteo[apartamento.getEstado().ordinal()]++;
        }
        return conteo;
    }

    /**
     * Noches ocupadas en cada temporada, sumando las de todas las reservas que no
     * fueron canceladas ni quedaron como no-show.
     *
     * @return arreglo con el número de noches por temporada (mismo orden que
     *         {@link Temporada#values()})
     */
    public int[] nochesOcupadasPorTemporada() {
        int[] noches = new int[Temporada.values().length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (!generaIngreso(reserva)) {
                continue;
            }
            for (LocalDate noche : reserva.getEstancia()) {
                noches[gestorReservas.getCalendario().temporadaDe(noche).ordinal()]++;
            }
        }
        return noches;
    }

    /**
     * Ocupación por temporada: noches ocupadas sobre las noches disponibles
     * (apartamentos × noches del rango observado de cada temporada).
     *
     * @return porcentaje por temporada (mismo orden que {@link Temporada#values()});
     *         0 si la temporada no tiene noches observadas
     */
    public double[] porcentajeOcupacionPorTemporada() {
        int[] ocupadas = nochesOcupadasPorTemporada();
        int apartamentos = gestorReservas.todosApartamentos().tamano();

        // Noches distintas con alguna reserva, por temporada: son las noches observadas.
        ListaEnlazada<LocalDate> vistas = new ListaEnlazada<>();
        int[] nochesObservadas = new int[ocupadas.length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (!generaIngreso(reserva)) {
                continue;
            }
            for (LocalDate noche : reserva.getEstancia()) {
                if (!vistas.contiene(noche)) {
                    vistas.agregar(noche);
                    nochesObservadas[gestorReservas.getCalendario().temporadaDe(noche).ordinal()]++;
                }
            }
        }
        double[] porcentaje = new double[ocupadas.length];
        for (int i = 0; i < ocupadas.length; i++) {
            long disponibles = (long) nochesObservadas[i] * apartamentos;
            porcentaje[i] = disponibles == 0 ? 0 : (ocupadas[i] * 100.0) / disponibles;
        }
        return porcentaje;
    }

    // ------------------------------------------------------------------
    // Ingresos
    // ------------------------------------------------------------------

    /** Ingreso de un periodo (mes) con las noches que lo generaron. */
    public static class IngresoPeriodo {
        private final YearMonth periodo;
        private double ingreso;
        private int noches;

        IngresoPeriodo(YearMonth periodo) {
            this.periodo = periodo;
        }

        public YearMonth getPeriodo() {
            return periodo;
        }

        public double getIngreso() {
            return ingreso;
        }

        public int getNoches() {
            return noches;
        }
    }

    /**
     * Ingresos por mes, en orden cronológico. Cada noche se asigna al mes en que
     * ocurre, usando la tarifa de su temporada y las personas que pagan.
     */
    public ListaEnlazada<IngresoPeriodo> ingresosPorMes() {
        ListaEnlazada<IngresoPeriodo> meses = new ListaEnlazada<>();
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (!generaIngreso(reserva)) {
                continue;
            }
            for (LocalDate noche : reserva.getEstancia()) {
                IngresoPeriodo mes = buscarOCrear(meses, YearMonth.from(noche));
                mes.ingreso += valorNoche(reserva, noche);
                mes.noches++;
            }
        }
        meses.ordenar((a, b) -> a.periodo.compareTo(b.periodo));
        return meses;
    }

    /**
     * Ingresos de las noches comprendidas entre dos fechas (ambas incluidas).
     */
    public double ingresosEntre(LocalDate desde, LocalDate hasta) {
        double total = 0;
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (!generaIngreso(reserva)) {
                continue;
            }
            for (LocalDate noche : reserva.getEstancia()) {
                if (!noche.isBefore(desde) && !noche.isAfter(hasta)) {
                    total += valorNoche(reserva, noche);
                }
            }
        }
        return total;
    }

    private IngresoPeriodo buscarOCrear(ListaEnlazada<IngresoPeriodo> meses, YearMonth periodo) {
        for (IngresoPeriodo mes : meses) {
            if (mes.periodo.equals(periodo)) {
                return mes;
            }
        }
        IngresoPeriodo nuevo = new IngresoPeriodo(periodo);
        meses.agregar(nuevo);
        return nuevo;
    }

    private double valorNoche(Reserva reserva, LocalDate noche) {
        Temporada temporada = gestorReservas.getCalendario().temporadaDe(noche);
        return reserva.getApartamento().precioPorPersona(temporada)
                * reserva.personasQuePagan(noche);
    }

    private boolean generaIngreso(Reserva reserva) {
        return reserva.getEstado() != EstadoReserva.CANCELADA
                && reserva.getEstado() != EstadoReserva.NO_SHOW;
    }

    /**
     * Ingresos registrados por canal.
     *
     * <p>Se cuenta el total estimado de las reservas que no fueron canceladas ni
     * marcadas como no-presentación: son las que generaron ingreso real.</p>
     *
     * @return arreglo con el total por canal (mismo orden que
     *         {@link CanalReserva#values()})
     */
    public double[] ingresosPorCanal() {
        double[] ingresos = new double[CanalReserva.values().length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (reserva.getEstado() == EstadoReserva.CANCELADA
                    || reserva.getEstado() == EstadoReserva.NO_SHOW) {
                continue;
            }
            ingresos[reserva.getCanal().ordinal()] += reserva.getTotalEstimado();
        }
        return ingresos;
    }

    /**
     * Ingresos por temporada.
     *
     * <p>Una estancia que cruza dos temporadas se reparte: cada noche aporta a la
     * temporada que le corresponde, de modo que la suma coincide con el total
     * cobrado.</p>
     *
     * @return arreglo con el total por temporada (mismo orden que
     *         {@link Temporada#values()})
     */
    public double[] ingresosPorTemporada() {
        double[] ingresos = new double[Temporada.values().length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (reserva.getEstado() == EstadoReserva.CANCELADA
                    || reserva.getEstado() == EstadoReserva.NO_SHOW) {
                continue;
            }
            Apartamento apartamento = reserva.getApartamento();
            for (LocalDate noche : reserva.getEstancia()) {
                Temporada temporada = gestorReservas.getCalendario().temporadaDe(noche);
                double tarifa = apartamento.precioPorPersona(temporada);
                ingresos[temporada.ordinal()] += tarifa * reserva.personasQuePagan(noche);
            }
        }
        return ingresos;
    }

    /** Ingreso total del alojamiento (reservas vigentes y finalizadas). */
    public double ingresoTotal() {
        double[] porCanal = ingresosPorCanal();
        double total = 0;
        for (double valor : porCanal) {
            total += valor;
        }
        return total;
    }

    /** Ingreso medio por reserva. */
    public double ingresoPromedio() {        int cantidad = reservasConIngreso();
        if (cantidad == 0) {
            return 0;
        }
        return ingresoTotal() / cantidad;
    }

    private int reservasConIngreso() {
        int cantidad = 0;
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (reserva.getEstado() != EstadoReserva.CANCELADA
                    && reserva.getEstado() != EstadoReserva.NO_SHOW) {
                cantidad++;
            }
        }
        return cantidad;
    }

    // ------------------------------------------------------------------
    // Tasas de cancelación y no-presentación
    // ------------------------------------------------------------------

    /**
     * Número de reservas por estado del ciclo de vida.
     *
     * @return arreglo con el conteo por estado (mismo orden que
     *         {@link EstadoReserva#values()})
     */
    public int[] reservasPorEstado() {
        int[] conteo = new int[EstadoReserva.values().length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            conteo[reserva.getEstado().ordinal()]++;
        }
        return conteo;
    }

    /**
     * Número de reservas por canal, <b>incluidas las canceladas y los no-show</b>,
     * tal como lo exige el reporte de canales.
     *
     * @return arreglo con el conteo por canal (mismo orden que
     *         {@link CanalReserva#values()})
     */
    public int[] reservasPorCanal() {
        int[] conteo = new int[CanalReserva.values().length];
        for (Reserva reserva : gestorReservas.todasReservas()) {
            conteo[reserva.getCanal().ordinal()]++;
        }
        return conteo;
    }

    /**
     * Noches reservadas por cada apartamento, en el mismo orden en que
     * {@link GestorReservas#todosApartamentos()} los devuelve (por código).
     *
     * <p>Se cuentan las noches de todas las reservas que llegaron a ocupar el
     * apartamento; las canceladas y los no-show no suman.</p>
     */
    public int[] nochesReservadasPorApartamento() {
        ListaEnlazada<Apartamento> apartamentos = gestorReservas.todosApartamentos();
        int[] noches = new int[apartamentos.tamano()];
        int indice = 0;
        for (Apartamento apartamento : apartamentos) {
            for (Reserva reserva : apartamento.getReservas()) {
                if (reserva.getEstado() == EstadoReserva.CANCELADA
                        || reserva.getEstado() == EstadoReserva.NO_SHOW) {
                    continue;
                }
                for (LocalDate noche : reserva.getEstancia()) {
                    noches[indice]++;
                }
            }
            indice++;
        }
        return noches;
    }

    /** Tasa de cancelación, en porcentaje sobre el total de reservas. */
    public double tasaCancelacion() {
        return porcentaje(EstadoReserva.CANCELADA);
    }

    /** Tasa de no-presentación, en porcentaje sobre el total de reservas. */
    public double tasaNoShow() {
        return porcentaje(EstadoReserva.NO_SHOW);
    }

    private double porcentaje(EstadoReserva estado) {
        int total = gestorReservas.todasReservas().tamano();
        if (total == 0) {
            return 0;
        }
        int conteo = 0;
        for (Reserva reserva : gestorReservas.todasReservas()) {
            if (reserva.getEstado() == estado) {
                conteo++;
            }
        }
        return (conteo * 100.0) / total;
    }

    // ------------------------------------------------------------------
    // Conflictos y preparación
    // ------------------------------------------------------------------

    /** Número de conflictos registrados por el canal externo. */
    public int numeroConflictosCanal() {
        return procesadorCanalExterno == null ? 0 : procesadorCanalExterno.numeroConflictos();
    }

    /** Tiempo promedio de preparación de los apartamentos, en minutos. */
    public double tiempoMedioPreparacionMinutos() {
        return gestorPreparacion == null ? 0 : gestorPreparacion.tiempoPromedioPreparacionMinutos();
    }

    /** Número de apartamentos que esperan preparación. */
    public int apartamentosPorPreparar() {
        int cantidad = 0;
        for (Apartamento apartamento : gestorReservas.todosApartamentos()) {
            if (apartamento.getEstado().requierePreparacion()) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /** Total de reservas registradas en el alojamiento. */
    public int totalReservas() {
        return gestorReservas.todasReservas().tamano();
    }
}
