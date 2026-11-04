package service;

import datastructures.ListaDobleEnlazada;
import exceptions.ElementoNoEncontradoException;
import model.Folio;
import model.Movimiento;
import model.Reserva;

/**
 * Gestor de folios: centraliza los movimientos de la cuenta de cada reserva.
 *
 * <p>Reglas del alojamiento:</p>
 * <ul>
 *   <li>Los movimientos <b>nunca se borran</b>.</li>
 *   <li>Para corregir una operación se registra el <b>movimiento contrario</b>
 *       (contra-movimiento), de modo que el historial queda auditable.</li>
 *   <li>Deshacer una operación usa la <b>pila de operaciones</b> del folio: al
 *       desapilar, se agrega al folio el movimiento inverso.</li>
 * </ul>
 */
public class GestorFolios {

    private final GestorReservas gestorReservas;

    public GestorFolios(GestorReservas gestorReservas) {
        this.gestorReservas = gestorReservas;
    }

    /**
     * Registra un cargo en el folio de la reserva.
     */
    public Movimiento registrarCargo(String codigoReserva, String concepto, double valor) {
        return folio(codigoReserva).registrarCargo(concepto, valor);
    }

    /**
     * Registra un pago recibido en el folio de la reserva.
     */
    public Movimiento registrarPago(String codigoReserva, String concepto, double valor) {
        return folio(codigoReserva).registrarPago(concepto, valor);
    }

    /**
     * Registra un abono o devolución a favor del huésped.
     */
    public Movimiento registrarAbono(String codigoReserva, String concepto, double valor) {
        return folio(codigoReserva).registrarAbono(concepto, valor);
    }

    /**
     * Corrige un movimiento registrando su <b>contra-movimiento</b>. El movimiento
     * original permanece en el folio.
     *
     * @param codigoReserva reserva a la que pertenece el folio
     * @param movimiento    movimiento que se quiere corregir
     * @return el contra-movimiento registrado
     */
    public Movimiento corregir(String codigoReserva, Movimiento movimiento) {
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento a corregir es obligatorio.");
        }
        Folio folio = folio(codigoReserva);
        Movimiento inverso = movimiento.inverso();
        folio.getMovimientos().agregar(inverso);
        return inverso;
    }

    /**
     * Deshace la última operación registrada en el folio.
     *
     * @return el contra-movimiento que se registró
     */
    public Movimiento deshacerUltimaOperacion(String codigoReserva) {
        return folio(codigoReserva).deshacerUltimaOperacion();
    }

    /** Saldo pendiente del folio de una reserva. */
    public double saldo(String codigoReserva) {
        return folio(codigoReserva).saldo();
    }

    /** Historial completo de movimientos de una reserva. */
    public ListaDobleEnlazada<Movimiento> movimientos(String codigoReserva) {
        return folio(codigoReserva).getMovimientos();
    }

    /**
     * Folio de una reserva.
     *
     * @throws ElementoNoEncontradoException si la reserva no existe
     */
    public Folio folio(String codigoReserva) {
        return gestorReservas.buscarReserva(codigoReserva).getFolio();
    }

    /**
     * Saldo total pendiente de todas las reservas del alojamiento.
     */
    public double saldoTotalPendiente() {
        double total = 0;
        for (Reserva reserva : gestorReservas.todasReservas()) {
            total += reserva.getFolio().saldo();
        }
        return total;
    }
}
