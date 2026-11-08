package model;

import java.time.LocalDate;

import datastructures.ListaDobleEnlazada;

/**
 * Liquidación de una estancia: desglose noche por noche del valor a cobrar.
 *
 * Como el alojamiento cobra por persona y por noche, y una estancia puede cruzar
 * varias temporadas, la liquidación se calcula recorriendo las noches de la estancia
 * y aplicando a cada una la tarifa de su temporada y el número de personas que pagan
 * esa noche (los menores de seis años no pagan).
 */
public class Liquidacion {

    /** Detalle del cobro de una noche concreta. */
    public static class DetalleNoche {
        private final LocalDate fecha;
        private final Temporada temporada;
        private final int personasQuePagan;
        private final double tarifaPorPersona;
        private final double valor;

        public DetalleNoche(LocalDate fecha, Temporada temporada, int personasQuePagan,
                            double tarifaPorPersona, double valor) {
            this.fecha = fecha;
            this.temporada = temporada;
            this.personasQuePagan = personasQuePagan;
            this.tarifaPorPersona = tarifaPorPersona;
            this.valor = valor;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public Temporada getTemporada() {
            return temporada;
        }

        public int getPersonasQuePagan() {
            return personasQuePagan;
        }

        public double getTarifaPorPersona() {
            return tarifaPorPersona;
        }

        public double getValor() {
            return valor;
        }

        @Override
        public String toString() {
            return fecha + " · " + temporada.getDescripcion() + " · " + personasQuePagan
                    + " pax · $" + String.format("%,.0f", valor);
        }
    }

    private final String codigoReserva;
    private final ListaDobleEnlazada<DetalleNoche> noches = new ListaDobleEnlazada<>();
    private double total;

    public Liquidacion(String codigoReserva) {
        this.codigoReserva = codigoReserva;
        this.total = 0;
    }

    public void agregarNoche(DetalleNoche detalle) {
        noches.agregar(detalle);
        total += detalle.getValor();
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public ListaDobleEnlazada<DetalleNoche> getNoches() {
        return noches;
    }

    public double getTotal() {
        return total;
    }

    /** Fija el total de la liquidación (lo calcula el servicio de liquidación). */
    public void setTotal(double total) {
        this.total = total;
    }

    public int numeroNoches() {
        return noches.tamano();
    }

    /** Impuestos o cargos adicionales aplicados sobre el total. */
    public double totalConImpuesto(double porcentaje) {
        return total * (1 + porcentaje / 100.0);
    }

    /** Devuelve las temporadas involucradas en la liquidación, sin repetir. */
    public datastructures.Conjunto<Temporada> temporadasInvolucradas() {
        datastructures.Conjunto<Temporada> temporadas = new datastructures.Conjunto<>();
        for (DetalleNoche noche : noches) {
            temporadas.agregar(noche.getTemporada());
        }
        return temporadas;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Liquidación de la reserva ").append(codigoReserva).append('\n');
        for (DetalleNoche noche : noches) {
            sb.append("  ").append(noche).append('\n');
        }
        sb.append("  TOTAL: $").append(String.format("%,.0f", total));
        return sb.toString();
    }
}
