package model;

import datastructures.ArbolBinarioBusqueda;
import datastructures.Conjunto;

/**
 * Apartamento del alojamiento.
 *
 * Implementa {Comparable} con orden natural por código, lo que
 * permite localizarlo en el árbol binario de búsqueda en milisegundos.
 *
 * Cada apartamento:
 *   se entrega completo y en exclusiva a un solo grupo,
 *   tiene una tarifa por persona y por noche para cada temporada,
 *   mantiene sus reservas en un árbol binario ordenado por fecha de entrada,
 *       que es lo que hace eficiente la detección de solapamientos,
 *   tiene una posición (x, y) para dibujarlo en el mapa interactivo.
 */
public class Apartamento implements Comparable<Apartamento> {

    private final String codigo;
    private String nombre;
    private String bloque;
    private int piso;
    private int numeroDormitorios;
    private int capacidadMaxima;
    private final Conjunto<String> caracteristicas = new Conjunto<>();
    private final Tarifa[] tarifas = new Tarifa[Temporada.values().length];

    private EstadoApartamento estado;
    private String motivoFueraDeServicio;

    private double posicionX;
    private double posicionY;

    /** Reservas del apartamento ordenadas por fecha de entrada (árbol de búsqueda). */
    private final ArbolBinarioBusqueda<Reserva> reservas = new ArbolBinarioBusqueda<>();

    public Apartamento(String codigo, String nombre, String bloque, int piso,
                       int numeroDormitorios, int capacidadMaxima, double posicionX, double posicionY) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del apartamento es obligatorio.");
        }
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero.");
        }
        if (numeroDormitorios < 0) {
            throw new IllegalArgumentException("El número de dormitorios no puede ser negativo.");
        }
        this.codigo = codigo.trim();
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.numeroDormitorios = numeroDormitorios;
        this.capacidadMaxima = capacidadMaxima;
        this.estado = EstadoApartamento.PREPARADO;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

    /**
     * Construye la <b>clave de búsqueda</b> de un apartamento: una instancia mínima
     * que solo tiene el código, usada para consultar el árbol binario por código
     * sin necesidad de tener el apartamento completo.
     */
    public static Apartamento claveDeBusqueda(String codigo) {
        return new Apartamento(codigo, "", "", 0, 0, 1, 0, 0);
    }

    /**
     * Actualiza los datos editables del apartamento (el código no cambia porque es
     * la clave del árbol de búsqueda).
     */
    public void actualizarDatos(String nombre, String bloque, int piso,
                                int numeroDormitorios, int capacidadMaxima) {
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero.");
        }
        if (numeroDormitorios < 0) {
            throw new IllegalArgumentException("El número de dormitorios no puede ser negativo.");
        }
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.numeroDormitorios = numeroDormitorios;
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getBloque() {
        return bloque;
    }

    public int getPiso() {
        return piso;
    }

    public int getNumeroDormitorios() {
        return numeroDormitorios;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public Conjunto<String> getCaracteristicas() {
        return caracteristicas;
    }

    public void agregarCaracteristica(String caracteristica) {
        if (caracteristica != null && !caracteristica.trim().isEmpty()) {
            caracteristicas.agregar(caracteristica.trim());
        }
    }

    public EstadoApartamento getEstado() {
        return estado;
    }

    public String getMotivoFueraDeServicio() {
        return motivoFueraDeServicio;
    }

    public double getPosicionX() {
        return posicionX;
    }

    public double getPosicionY() {
        return posicionY;
    }

    public void setPosicion(double x, double y) {
        this.posicionX = x;
        this.posicionY = y;
    }

    /** Árbol de reservas del apartamento, ordenado por fecha de entrada. */
    public ArbolBinarioBusqueda<Reserva> getReservas() {
        return reservas;
    }

    /**
     * Define la tarifa de una temporada.
     */
    public void definirTarifa(Temporada temporada, double precioPorPersonaPorNoche) {
        if (temporada == null) {
            throw new IllegalArgumentException("La temporada es obligatoria.");
        }
        tarifas[temporada.ordinal()] = new Tarifa(temporada, precioPorPersonaPorNoche);
    }

    /**
     * Devuelve la tarifa de la temporada indicada. Si no se definió, se usa la tarifa
     * de la temporada Baja como base.
     */
    public Tarifa tarifa(Temporada temporada) {
        Tarifa tarifa = tarifas[temporada.ordinal()];
        if (tarifa == null) {
            tarifa = tarifas[Temporada.BAJA.ordinal()];
        }
        if (tarifa == null) {
            throw new IllegalStateException(
                    "El apartamento " + codigo + " no tiene tarifas definidas.");
        }
        return tarifa;
    }

    /** Precio por persona y noche para una temporada. */
    public double precioPorPersona(Temporada temporada) {
        return tarifa(temporada).getPrecioPorPersonaPorNoche();
    }

    /**
     * Cambia el estado operativo del apartamento validando la transición.
     *
     * @param nuevo  nuevo estado
     * @param motivo motivo obligatorio cuando el nuevo estado es Fuera de servicio
     */
    public void cambiarEstado(EstadoApartamento nuevo, String motivo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }
        if (nuevo == EstadoApartamento.FUERA_DE_SERVICIO
                && (motivo == null || motivo.trim().isEmpty())) {
            throw new IllegalArgumentException(
                    "Para pasar a Fuera de servicio se debe registrar el motivo.");
        }
        this.estado = nuevo;
        this.motivoFueraDeServicio = nuevo == EstadoApartamento.FUERA_DE_SERVICIO ? motivo.trim() : null;
    }

    public void cambiarEstado(EstadoApartamento nuevo) {
        cambiarEstado(nuevo, null);
    }

    /** {@code true} si el apartamento está Preparado y puede recibir un nuevo grupo. */
    public boolean estaDisponibleParaCheckIn() {
        return estado == EstadoApartamento.PREPARADO;
    }

    /** Número de reservas registradas en el árbol del apartamento. */
    public int numeroReservas() {
        return reservas.tamano();
    }

    /**
     * Orden natural: por código de apartamento.
     */
    @Override
    public int compareTo(Apartamento otro) {
        return this.codigo.compareToIgnoreCase(otro.codigo);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Apartamento)) {
            return false;
        }
        return codigo.equalsIgnoreCase(((Apartamento) objeto).codigo);
    }

    @Override
    public int hashCode() {
        return codigo.toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
