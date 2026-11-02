package service;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.CalendarioTemporadas;
import model.Estancia;
import model.Temporada;

import java.time.LocalDate;

/**
 * Servicio de temporadas: responde "¿en qué temporada cae esta noche?" y calcula
 * los reportes por temporada.
 *
 * <p>Delega el almacenamiento en {@link CalendarioTemporadas} y añade el análisis
 * que necesita la interfaz: distribución de las noches de una estancia por
 * temporada y tarifa aplicable.</p>
 */
public class ServicioTemporadas {

    private final CalendarioTemporadas calendario;

    public ServicioTemporadas(CalendarioTemporadas calendario) {
        this.calendario = calendario;
    }

    public CalendarioTemporadas getCalendario() {
        return calendario;
    }

    /**
     * Registra el rango de fechas de una temporada.
     */
    public void registrar(Temporada temporada, LocalDate inicio, LocalDate fin) {
        calendario.registrar(temporada, inicio, fin);
    }

    /** Elimina un periodo de temporada; las fechas que cubría vuelven a la temporada base. */
    public boolean eliminar(CalendarioTemporadas.Periodo periodo) {
        return calendario.eliminar(periodo);
    }

    /** Temporada a la que pertenece una fecha. */
    public Temporada temporadaDe(LocalDate fecha) {
        return calendario.temporadaDe(fecha);
    }

    /**
     * Distribución de las noches de una estancia por temporada.
     *
     * @return arreglo con el número de noches de cada temporada (mismo orden que
     *         {@link Temporada#values()})
     */
    public int[] nochesPorTemporada(Estancia estancia) {
        int[] conteo = new int[Temporada.values().length];
        for (LocalDate noche : estancia) {
            Temporada temporada = calendario.temporadaDe(noche);
            conteo[temporada.ordinal()]++;
        }
        return conteo;
    }

    /**
     * Temporada en la que cae la <b>mayor parte</b> de la estancia.
     */
    public Temporada temporadaPredominante(Estancia estancia) {
        int[] conteo = nochesPorTemporada(estancia);
        int mejor = 0;
        for (int i = 1; i < conteo.length; i++) {
            if (conteo[i] > conteo[mejor]) {
                mejor = i;
            }
        }
        return Temporada.values()[mejor];
    }

    /**
     * Tarifa por persona y noche que se aplica a un apartamento en una fecha.
     */
    public double tarifaDe(Apartamento apartamento, LocalDate fecha) {
        return apartamento.precioPorPersona(calendario.temporadaDe(fecha));
    }

    /**
     * Variación porcentual de la tarifa de una temporada respecto de la temporada
     * base (Baja). Sirve para mostrar en la interfaz cuánto sube el precio en
     * temporada alta.
     *
     * @return variación en porcentaje (por ejemplo, 35.0 significa un 35 % más caro)
     */
    public double variacionRespectoABase(Apartamento apartamento, Temporada temporada) {
        double base = apartamento.precioPorPersona(Temporada.BAJA);
        if (base <= 0) {
            return 0;
        }
        double precio = apartamento.precioPorPersona(temporada);
        return ((precio - base) / base) * 100.0;
    }

    /**
     * Periodos de temporada configurados.
     */
    public ListaEnlazada<CalendarioTemporadas.Periodo> periodos() {
        return calendario.getPeriodos();
    }
}
