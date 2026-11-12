package model;

import java.time.LocalDate;

/**
 * Ocupante de una reserva.
 *
 * El huésped titular registra a sus ocupantes con su fecha de nacimiento. Los
 * ocupantes menores de seis años ocupan cupo pero no pagan, por lo que el
 * sistema necesita su fecha de nacimiento para determinarlo en cada noche.
 */
public class Ocupante {

    /** Edad mínima a partir de la cual un ocupante paga tarifa. */
    public static final int EDAD_MINIMA_PAGADORA = 6;

    private final String nombre;
    private final LocalDate fechaNacimiento;
    private final boolean titular;

    public Ocupante(String nombre, LocalDate fechaNacimiento) {
        this(nombre, fechaNacimiento, false);
    }

    public Ocupante(String nombre, LocalDate fechaNacimiento, boolean titular) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del ocupante es obligatorio.");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento del ocupante es obligatoria.");
        }
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        this.nombre = nombre.trim();
        this.fechaNacimiento = fechaNacimiento;
        this.titular = titular;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean esTitular() {
        return titular;
    }

    /**
     * Edad cumplida del ocupante en la fecha de referencia.
     */
    public int edadEn(LocalDate referencia) {
        if (referencia == null) {
            return 0;
        }
        int edad = referencia.getYear() - fechaNacimiento.getYear();
        if (referencia.getMonthValue() < fechaNacimiento.getMonthValue()
                || (referencia.getMonthValue() == fechaNacimiento.getMonthValue()
                && referencia.getDayOfMonth() < fechaNacimiento.getDayOfMonth())) {
            edad--;
        }
        return edad;
    }

    /**
     * {@code true} si el ocupante tiene seis años o más en la fecha de referencia,
     * es decir, si paga la tarifa de esa noche.
     */
    public boolean pagaEn(LocalDate referencia) {
        return edadEn(referencia) >= EDAD_MINIMA_PAGADORA;
    }

    @Override
    public String toString() {
        return nombre + (titular ? " (titular)" : "");
    }
}
