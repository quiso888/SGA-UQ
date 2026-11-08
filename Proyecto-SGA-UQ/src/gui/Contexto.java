package gui;

import datastructures.ListaEnlazada;
import service.SistemaAlojamiento;

/**
 * Contexto compartido por todos los paneles de la interfaz.
 *
 * Mantiene el sistema de alojamiento activo y la lista de paneles que deben
 * refrescarse cuando cambian los datos. Así, al cargar un escenario nuevo desde
 * cualquier panel, todas las vistas se actualizan de forma coordinada.
 */
public class Contexto {

    /** Paneles que saben refrescarse cuando cambian los datos. */
    public interface Actualizable {
        void actualizar();
    }

    private SistemaAlojamiento sistema;
    private Rol rol = Rol.RECEPCIONISTA;
    private String documentoHuesped;
    private final ListaEnlazada<Actualizable> paneles = new ListaEnlazada<>();

    public Contexto(SistemaAlojamiento sistema) {
        this.sistema = sistema;
    }

    public SistemaAlojamiento getSistema() {
        return sistema;
    }

    /** Rol con el que se está usando la aplicación. */
    public Rol getRol() {
        return rol;
    }

    /** Documento del huésped que inició sesión (solo con el rol Huésped). */
    public String getDocumentoHuesped() {
        return documentoHuesped;
    }

    /**
     * Huésped que inició sesión o {@code null} si no hay o ya no existe en el
     * sistema cargado.
     */
    public model.Huesped getHuespedActual() {
        return documentoHuesped == null ? null : sistema.buscarHuespedOpcional(documentoHuesped);
    }

    /** Cambia la sesión actual. */
    public void iniciarSesion(Rol nuevoRol, String documento) {
        this.rol = nuevoRol == null ? Rol.RECEPCIONISTA : nuevoRol;
        this.documentoHuesped = nuevoRol == Rol.HUESPED ? documento : null;
    }

    /**
     * Reemplaza el sistema activo y refresca todos los paneles registrados.
     */
    public void setSistema(SistemaAlojamiento nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El sistema no puede ser nulo.");
        }
        this.sistema = nuevo;
        notificar();
    }

    /** Registra un panel para que reciba las notificaciones de cambio. */
    public void registrar(Actualizable panel) {
        if (panel != null && !paneles.contiene(panel)) {
            paneles.agregar(panel);
        }
    }

    /** Olvida los paneles registrados (al reconstruir las pantallas de otro rol). */
    public void limpiarPaneles() {
        paneles.limpiar();
    }

    /** Refresca todos los paneles registrados. */
    public void notificar() {
        for (Actualizable panel : paneles) {
            panel.actualizar();
        }
    }
}
