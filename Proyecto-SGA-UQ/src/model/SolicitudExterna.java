package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solicitud que llega desde la plataforma externa (canal simulado de terceros).
 *
 * Las solicitudes llegan en orden y se procesan una a una desde una cola FIFO. Si
 * una solicitud choca con una reserva vigente, se rechaza y se registra como
 * conflicto de canal.
 */
public class SolicitudExterna {

    /** Estado de procesamiento de la solicitud externa. */
    public enum EstadoSolicitud {
        PENDIENTE("Pendiente de procesar"),
        ACEPTADA("Aceptada"),
        RECHAZADA("Rechazada");

        private final String descripcion;

        EstadoSolicitud(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

    private final String identificador;
    private final String nombreSolicitante;
    private final String documento;
    private final String correo;
    private final String telefono;
    private final String codigoApartamento;
    private final Estancia estancia;
    private final int numeroPersonas;
    private final LocalDateTime fechaRecepcion;

    private EstadoSolicitud estado;
    private String motivo;

    public SolicitudExterna(String identificador, String nombreSolicitante, String documento,
                            String correo, String telefono, String codigoApartamento,
                            Estancia estancia, int numeroPersonas, LocalDateTime fechaRecepcion) {
        if (identificador == null || identificador.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador de la solicitud es obligatorio.");
        }
        if (codigoApartamento == null || estancia == null || numeroPersonas <= 0) {
            throw new IllegalArgumentException("Los datos de la solicitud externa son incompletos.");
        }
        this.identificador = identificador.trim();
        this.nombreSolicitante = nombreSolicitante;
        this.documento = documento;
        this.correo = correo;
        this.telefono = telefono;
        this.codigoApartamento = codigoApartamento;
        this.estancia = estancia;
        this.numeroPersonas = numeroPersonas;
        this.fechaRecepcion = fechaRecepcion == null ? LocalDateTime.now() : fechaRecepcion;
        this.estado = EstadoSolicitud.PENDIENTE;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getNombreSolicitante() {
        return nombreSolicitante;
    }

    public String getDocumento() {
        return documento;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCodigoApartamento() {
        return codigoApartamento;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public LocalDate getFechaEntrada() {
        return estancia.getFechaEntrada();
    }

    public LocalDate getFechaSalida() {
        return estancia.getFechaSalida();
    }

    public int getNumeroPersonas() {
        return numeroPersonas;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void aceptar(String referenciaReserva) {
        this.estado = EstadoSolicitud.ACEPTADA;
        this.motivo = "Reserva creada: " + referenciaReserva;
    }

    public void rechazar(String motivo) {
        this.estado = EstadoSolicitud.RECHAZADA;
        this.motivo = motivo;
    }

    @Override
    public String toString() {
        return identificador + " · " + codigoApartamento + " · " + estancia + " · "
                + numeroPersonas + " personas · " + estado.getDescripcion();
    }
}
