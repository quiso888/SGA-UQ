package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Movimiento de un folio: un cargo, un pago o un abono.
 *
 * <p>Los movimientos <b>nunca se borran</b>. Cuando hay que corregir una operación se
 * registra un movimiento contrario (por ejemplo, un abono que revierte un cargo).</p>
 */
public class Movimiento {

    /** Tipos de movimiento de un folio. */
    public enum Tipo {
        /** Cargo a la cuenta del huésped (aumenta lo que debe). */
        CARGO("Cargo"),
        /** Pago recibido del huésped (disminuye lo que debe). */
        PAGO("Pago"),
        /** Abono o devolución a favor del huésped (disminuye lo que debe). */
        ABONO("Abono");

        private final String descripcion;

        Tipo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Tipo tipo;
    private final String concepto;
    private final double valor;
    private final LocalDateTime fecha;
    private final String referencia;

    public Movimiento(Tipo tipo, String concepto, double valor, LocalDateTime fecha, String referencia) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (concepto == null || concepto.trim().isEmpty()) {
            throw new IllegalArgumentException("El concepto del movimiento es obligatorio.");
        }
        if (valor <= 0) {
            throw new IllegalArgumentException("El valor del movimiento debe ser positivo.");
        }
        this.tipo = tipo;
        this.concepto = concepto.trim();
        this.valor = valor;
        this.fecha = fecha == null ? LocalDateTime.now() : fecha;
        this.referencia = referencia;
    }

    public Movimiento(Tipo tipo, String concepto, double valor) {
        this(tipo, concepto, valor, LocalDateTime.now(), null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getConcepto() {
        return concepto;
    }

    public double getValor() {
        return valor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getReferencia() {
        return referencia;
    }

    /**
     * Efecto del movimiento sobre el saldo del folio.
     * Positivo para cargos (aumenta la deuda) y negativo para pagos y abonos.
     */
    public double efectoEnSaldo() {
        switch (tipo) {
            case CARGO:
                return valor;
            case PAGO:
            case ABONO:
                return -valor;
            default:
                return 0;
        }
    }

    /**
     * Crea el movimiento contrario que revierte este movimiento. El movimiento
     * original permanece en el folio; solo se agrega el contrario.
     */
    public Movimiento inverso() {
        switch (tipo) {
            case CARGO:
                return new Movimiento(Tipo.ABONO,
                        "Reversión del cargo: " + concepto, valor, LocalDateTime.now(), referencia);
            case PAGO:
                // Revertir un pago vuelve a cargar la deuda al folio.
                return new Movimiento(Tipo.CARGO,
                        "Reversión del pago: " + concepto, valor, LocalDateTime.now(), referencia);
            case ABONO:
                return new Movimiento(Tipo.CARGO,
                        "Reversión del abono: " + concepto, valor, LocalDateTime.now(), referencia);
            default:
                throw new IllegalStateException("Tipo de movimiento no soportado: " + tipo);
        }
    }

    /** {@code true} si el movimiento es una reversión generada al deshacer. */
    public boolean esReversion() {
        return concepto.startsWith("Reversión");
    }

    @Override
    public String toString() {
        String signo = tipo == Tipo.CARGO ? "+" : "-";
        return String.format("%s $%s %s — %s (%s)", tipo.getDescripcion(), signo + String.format("%,.0f", valor),
                concepto, fecha.format(FORMATO), referencia == null ? "" : referencia);
    }
}
