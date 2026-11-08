package gui;

import java.awt.Component;

import javax.swing.JOptionPane;

import exceptions.SgaException;
import exceptions.TransicionEstadoInvalidaException;
import model.Reserva;

/**
 * Acciones sobre una reserva que comparten el panel de recepción y el panel del
 * huésped: pagar el anticipo y cancelar viendo antes la devolución.
 */
final class AccionesReserva {

    private AccionesReserva() {
        // Clase de utilidades: no se instancia.
    }

    /** Convierte un texto de monto en pesos a número, aceptando puntos y comas. */
    static double leerMonto(String texto) {
        return Double.parseDouble(texto.trim().replace(".", "").replace(",", "."));
    }

    /** Pide el monto y registra el anticipo de la reserva. */
    static void registrarAnticipo(Component padre, Contexto contexto, Reserva reserva) {
        String entrada = JOptionPane.showInputDialog(padre,
                "Monto del anticipo para " + reserva.getCodigo() + "\n"
                        + "Anticipo exigido (30 %): " + Formato.moneda(reserva.anticipoRequerido()),
                Formato.moneda(reserva.anticipoRequerido()));
        if (entrada == null) {
            return;
        }
        double monto;
        try {
            monto = leerMonto(entrada);
        } catch (NumberFormatException excepcion) {
            error(padre, "Monto inválido", "Escriba un valor numérico, por ejemplo 150000.");
            return;
        }
        try {
            Reserva actualizada = contexto.getSistema().registrarAnticipo(reserva.getCodigo(), monto);
            contexto.notificar();
            JOptionPane.showMessageDialog(padre,
                    "Anticipo registrado: " + Formato.moneda(monto)
                            + "\nEstado actual: " + actualizada.getEstado().getDescripcion(),
                    "Anticipo", JOptionPane.INFORMATION_MESSAGE);
        } catch (TransicionEstadoInvalidaException excepcion) {
            error(padre, "No se puede registrar el anticipo", excepcion.getMessage());
        } catch (SgaException | IllegalArgumentException excepcion) {
            error(padre, "No se pudo registrar el anticipo", excepcion.getMessage());
        }
    }

    /** Muestra la devolución estimada y, si se confirma, cancela la reserva. */
    static void cancelar(Component padre, Contexto contexto, Reserva reserva) {
        double devolucion = reserva.montoDevolucion();
        String mensaje = "Reserva " + reserva.getCodigo() + " · " + reserva.getTitular().getNombre()
                + "\nEntrada: " + Formato.fecha(reserva.getEstancia().getFechaEntrada())
                + "   ·   Anticipo pagado: " + Formato.moneda(reserva.getAnticipoPagado())
                + "\n\nSi cancela hoy, la devolución estimada es: "
                + Formato.moneda(devolucion)
                + "\n(El anticipo se devuelve completo cuando la cancelación se hace antes"
                + " de la fecha de entrada; el mismo día del ingreso no hay devolución.)"
                + "\n\n¿Desea continuar con la cancelación?";
        int confirmacion = JOptionPane.showConfirmDialog(padre, mensaje,
                "Cancelación de reserva", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        String motivo = JOptionPane.showInputDialog(padre,
                "Motivo de la cancelación de " + reserva.getCodigo() + ":",
                "Cancelación de reserva", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        try {
            contexto.getSistema().cancelarReserva(reserva.getCodigo(), motivo);
            contexto.notificar();
            JOptionPane.showMessageDialog(padre,
                    "Reserva cancelada. Noches liberadas.\nDevolución estimada al huésped: "
                            + Formato.moneda(devolucion),
                    "Reserva cancelada", JOptionPane.INFORMATION_MESSAGE);
        } catch (TransicionEstadoInvalidaException excepcion) {
            error(padre, "Cancelación no permitida", excepcion.getMessage());
        } catch (SgaException excepcion) {
            error(padre, "No se pudo cancelar la reserva", excepcion.getMessage());
        }
    }

    static void error(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }
}
