package model;

import java.time.LocalDateTime;

import datastructures.ListaDobleEnlazada;
import datastructures.Pila;
import exceptions.EstructuraVaciaException;

/**
 * Folio: la cuenta de una reserva.
 *
 * Contiene todos los cargos y pagos asociados a la estancia. Los movimientos
 * nunca se borran: para corregir una operación se registra el movimiento
 * contrario.
 *
 * Cada operación que se registra se apila en una pila. Al deshacer la
 * última operación, se saca de la pila y se agrega al folio su movimiento inverso,
 * de modo que el historial queda completo y auditable.
 */
public class Folio {

    private final String codigo;
    private final Huesped huesped;
    private final ListaDobleEnlazada<Movimiento> movimientos = new ListaDobleEnlazada<>();
    private final Pila<Movimiento> pilaOperaciones = new Pila<>();

    public Folio(String codigo, Huesped huesped) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del folio es obligatorio.");
        }
        this.codigo = codigo.trim();
        this.huesped = huesped;
    }

    public String getCodigo() {
        return codigo;
    }

    public Huesped getHuesped() {
        return huesped;
    }

    /** Movimientos del folio, en orden cronológico. */
    public ListaDobleEnlazada<Movimiento> getMovimientos() {
        return movimientos;
    }

    /** Pila de operaciones que pueden deshacerse. */
    public Pila<Movimiento> getPilaOperaciones() {
        return pilaOperaciones;
    }

    /** Registra un cargo en la cuenta. */
    public Movimiento registrarCargo(String concepto, double valor) {
        Movimiento movimiento = new Movimiento(Movimiento.Tipo.CARGO, concepto, valor,
                LocalDateTime.now(), codigo);
        movimientos.agregar(movimiento);
        pilaOperaciones.apilar(movimiento);
        return movimiento;
    }

    /** Registra un pago recibido. */
    public Movimiento registrarPago(String concepto, double valor) {
        Movimiento movimiento = new Movimiento(Movimiento.Tipo.PAGO, concepto, valor,
                LocalDateTime.now(), codigo);
        movimientos.agregar(movimiento);
        pilaOperaciones.apilar(movimiento);
        return movimiento;
    }

    /** Registra un abono o devolución a favor del huésped. */
    public Movimiento registrarAbono(String concepto, double valor) {
        Movimiento movimiento = new Movimiento(Movimiento.Tipo.ABONO, concepto, valor,
                LocalDateTime.now(), codigo);
        movimientos.agregar(movimiento);
        pilaOperaciones.apilar(movimiento);
        return movimiento;
    }

    /**
     * Deshace la última operación registrada.
     *
     * <p>El movimiento original <b>no se borra</b>: se agrega al folio su movimiento
     * inverso, y la operación deshecha ya no puede volver a deshacerse.</p>
     *
     * @return el movimiento inverso que se registró
     * @throws EstructuraVaciaException si no hay operaciones por deshacer
     */
    public Movimiento deshacerUltimaOperacion() {
        if (pilaOperaciones.estaVacia()) {
            throw new EstructuraVaciaException("Pila de operaciones del folio", "deshacer");
        }
        Movimiento original = pilaOperaciones.desapilar();
        Movimiento inverso = original.inverso();
        movimientos.agregar(inverso);
        return inverso;
    }

    /**
     * Saldo pendiente del folio, calculado de forma <b>recursiva</b> sobre la lista
     * doblemente enlazada de movimientos.
     */
    public double saldo() {
        return saldoRecursivo(movimientos.iterator(), 0.0);
    }

    private double saldoRecursivo(java.util.Iterator<Movimiento> iterador, double acumulado) {
        if (!iterador.hasNext()) {
            return acumulado;
        }
        Movimiento movimiento = iterador.next();
        return saldoRecursivo(iterador, acumulado + movimiento.efectoEnSaldo());
    }

    /** Total de cargos registrados (incluye reversiones de cargos). */
    public double totalCargos() {
        double total = 0;
        for (Movimiento movimiento : movimientos) {
            if (movimiento.getTipo() == Movimiento.Tipo.CARGO) {
                total += movimiento.getValor();
            }
        }
        return total;
    }

    /** Total de pagos y abonos registrados. */
    public double totalPagos() {
        double total = 0;
        for (Movimiento movimiento : movimientos) {
            if (movimiento.getTipo() != Movimiento.Tipo.CARGO) {
                total += movimiento.getValor();
            }
        }
        return total;
    }

    public int numeroMovimientos() {
        return movimientos.tamano();
    }

    public boolean estaSaldado() {
        return Math.abs(saldo()) < 0.01;
    }

    @Override
    public String toString() {
        return "Folio " + codigo + " — saldo $" + String.format("%,.0f", saldo());
    }
}
