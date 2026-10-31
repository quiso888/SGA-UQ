package model;

import java.time.LocalDate;

import datastructures.Conjunto;
import datastructures.ListaDobleEnlazada;

/**
 * Huésped del alojamiento.
 *
 * <p>Implementa {@link Comparable} con <b>orden natural por documento</b>, que es lo
 * que permite almacenarlo en el árbol binario de búsqueda para localizarlo en
 * milisegundos.</p>
 *
 * <p>Guarda además:</p>
 * <ul>
 *   <li>su conjunto de <b>apartamentos favoritos</b> (sin duplicados),</li>
 *   <li>su <b>historial de estancias</b> en una lista doblemente enlazada.</li>
 * </ul>
 */
public class Huesped implements Comparable<Huesped> {

    private final String documento;
    private String nombre;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String rutaFotografia;

    /** Códigos de los apartamentos favoritos: conjunto sin duplicados. */
    private final Conjunto<String> favoritos = new Conjunto<>();

    /** Historial de estancias, de la más antigua a la más reciente. */
    private final ListaDobleEnlazada<Reserva> historial = new ListaDobleEnlazada<>();

    public Huesped(String documento, String nombre, LocalDate fechaNacimiento,
                   String telefono, String correo) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("El documento del huésped es obligatorio.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del huésped es obligatorio.");
        }
        this.documento = documento.trim();
        this.nombre = nombre.trim();
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.correo = correo;
        this.rutaFotografia = null;
    }

    /**
     * Constructor privado de <b>clave de búsqueda</b>: crea una instancia mínima que
     * solo tiene el documento. Se usa para consultar el árbol binario por documento
     * sin necesidad de tener el huésped completo, por lo que se salta la validación
     * del nombre (que en una clave de búsqueda no aporta información).
     *
     * @param documento documento del huésped
     */
    private Huesped(String documento, boolean soloClave) {
        if (!soloClave) {
            throw new IllegalStateException("Este constructor es interno.");
        }
        this.documento = documento == null ? "" : documento.trim();
        this.nombre = "";
        this.fechaNacimiento = null;
        this.telefono = null;
        this.correo = null;
        this.rutaFotografia = null;
    }

    /**
     * Construye la <b>clave de búsqueda</b> de un huésped: una instancia mínima que
     * solo tiene el documento, usada para consultar el árbol binario por documento
     * sin necesidad de tener el huésped completo.
     */
    public static Huesped claveDeBusqueda(String documento) {
        return new Huesped(documento, true);
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRutaFotografia() {
        return rutaFotografia;
    }

    public void setRutaFotografia(String rutaFotografia) {
        this.rutaFotografia = rutaFotografia;
    }

    /** Conjunto de códigos de apartamentos favoritos (sin repetidos). */
    public Conjunto<String> getFavoritos() {
        return favoritos;
    }

    /**
     * Marca o desmarca un apartamento como favorito.
     *
     * @return {@code true} si quedó como favorito, {@code false} si se quitó
     */
    public boolean alternarFavorito(String codigoApartamento) {
        if (codigoApartamento == null) {
            return false;
        }
        if (favoritos.contiene(codigoApartamento)) {
            favoritos.remover(codigoApartamento);
            return false;
        }
        favoritos.agregar(codigoApartamento);
        return true;
    }

    public boolean esFavorito(String codigoApartamento) {
        return favoritos.contiene(codigoApartamento);
    }

    /** Historial de estancias del huésped. */
    public ListaDobleEnlazada<Reserva> getHistorial() {
        return historial;
    }

    /** Registra una reserva en el historial (solo si no está ya). */
    public void registrarEnHistorial(Reserva reserva) {
        if (reserva != null && !historial.contiene(reserva)) {
            historial.agregar(reserva);
        }
    }

    /**
     * Orden natural del huésped: por documento (comparación alfanumérica, ignorando
     * espacios y guiones para que "1.020-304" y "1020304" sean el mismo huésped).
     */
    @Override
    public int compareTo(Huesped otro) {
        return normalizarDocumento(this.documento).compareTo(normalizarDocumento(otro.documento));
    }

    /** Normaliza un documento quitando puntos, guiones y espacios. */
    public static String normalizarDocumento(String documento) {
        if (documento == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documento.length(); i++) {
            char c = documento.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Huesped)) {
            return false;
        }
        Huesped otro = (Huesped) objeto;
        return normalizarDocumento(documento).equals(normalizarDocumento(otro.documento));
    }

    @Override
    public int hashCode() {
        return normalizarDocumento(documento).hashCode();
    }

    @Override
    public String toString() {
        return nombre + " (" + documento + ")";
    }
}
