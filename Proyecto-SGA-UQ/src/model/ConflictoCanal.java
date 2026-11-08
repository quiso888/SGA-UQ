package model;

import java.time.LocalDateTime;

/**
 * Registro de un conflicto detectado al procesar el canal externo.
 * Cuando una solicitud de la plataforma de terceros choca con una reserva vigente
 * del mismo apartamento, se rechaza y queda registrado aquí para los reportes de
 * conflictos de canal.
 */
public class ConflictoCanal {

    private final String identificadorSolicitud;
    private final String codigoApartamento;
    private final String motivo;
    private final LocalDateTime fecha;
    private final String reservaEnConflicto;

    public ConflictoCanal(String identificadorSolicitud, String codigoApartamento, String motivo,
                          String reservaEnConflicto, LocalDateTime fecha) {
        this.identificadorSolicitud = identificadorSolicitud;
        this.codigoApartamento = codigoApartamento;
        this.motivo = motivo;
        this.reservaEnConflicto = reservaEnConflicto;
        this.fecha = fecha == null ? LocalDateTime.now() : fecha;
    }

    public String getIdentificadorSolicitud() {
        return identificadorSolicitud;
    }

    public String getCodigoApartamento() {
        return codigoApartamento;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReservaEnConflicto() {
        return reservaEnConflicto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return "Conflicto " + identificadorSolicitud + " (" + codigoApartamento + "): " + motivo;
    }
}
