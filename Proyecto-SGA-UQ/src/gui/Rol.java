package gui;

/**
 * Roles del sistema. Cada rol ve las pantallas que necesita para su trabajo.
 */
public enum Rol {

    HUESPED("Huésped", "Consulta disponibilidad, reserva por el portal y revisa su cuenta"),
    RECEPCIONISTA("Recepcionista", "Reservas, llegadas y salidas, folios, preparación y turnos"),
    ADMINISTRADOR("Administrador", "Mapa, empleados, apartamentos, huéspedes, tarifas y reportes");

    private final String descripcion;
    private final String resumen;

    Rol(String descripcion, String resumen) {
        this.descripcion = descripcion;
        this.resumen = resumen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getResumen() {
        return resumen;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}