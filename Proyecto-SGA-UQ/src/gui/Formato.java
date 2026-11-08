package gui;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidades de formato para la interfaz: fechas, horas y moneda en pesos
 * colombianos.
 */
public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEDA =
            NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    private Formato() {
        // Clase de utilidades: no se instancia.
    }

    /** Formatea una fecha como dd/MM/yyyy. */
    public static String fecha(LocalDate valor) {
        return valor == null ? "—" : valor.format(FECHA);
    }

    /** Formatea una fecha y hora como dd/MM/yyyy HH:mm. */
    public static String fechaHora(LocalDateTime valor) {
        return valor == null ? "—" : valor.format(FECHA_HORA);
    }

    /** Formatea un monto en pesos colombianos (sin decimales). */
    public static String moneda(double valor) {
        MONEDA.setMaximumFractionDigits(0);
        MONEDA.setMinimumFractionDigits(0);
        return MONEDA.format(valor);
    }

    /** Formatea un porcentaje con un decimal. */
    public static String porcentaje(double valor) {
        return String.format(new Locale("es", "CO"), "%.1f %%", valor);
    }

    /** Convierte un texto en color hexadecimal (por ejemplo 0x2E7D32) a un color. */
    public static java.awt.Color color(int rgb) {
        return new java.awt.Color(rgb);
    }

    /** Texto legible para el color de un estado (blanco o negro según luminosidad). */
    public static java.awt.Color textoContraste(java.awt.Color fondo) {
        double luminosidad = 0.299 * fondo.getRed() + 0.587 * fondo.getGreen()
                + 0.114 * fondo.getBlue();
        return luminosidad > 150 ? java.awt.Color.BLACK : java.awt.Color.WHITE;
    }
}
