package model;

/**
 * Empleado del alojamiento (recepcionista o administrador).
 *
 * Los recepcionistas rotan turnos: el turno actual se administra con una
 * {datastructures.ListaCircular} de empleados. El orden natural es por
 * <documento, lo que permite guardar a los empleados en un
 * {datastructures.ArbolBinarioBusqueda}.
 */
public class Empleado implements Comparable<Empleado> {

    /** Cargo del empleado dentro del alojamiento. */
    public enum Cargo {
        RECEPCIONISTA("Recepcionista"),
        ADMINISTRADOR("Administrador"),
        AMA_DE_LLAVES("Ama de llaves");

        private final String descripcion;

        Cargo(String descripcion) {
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

    private final String documento;
    private String nombre;
    private Cargo cargo;
    private boolean activo;

    public Empleado(String documento, String nombre, Cargo cargo) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("El documento del empleado es obligatorio.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio.");
        }
        this.documento = documento.trim();
        this.nombre = nombre.trim();
        this.cargo = cargo == null ? Cargo.RECEPCIONISTA : cargo;
        this.activo = true;
    }

    /**
     * Construye la <b>clave de búsqueda</b> de un empleado: una instancia mínima
     * que solo tiene el documento, para consultar el árbol por documento.
     */
    public static Empleado claveDeBusqueda(String documento) {
        return new Empleado(documento, "-", Cargo.RECEPCIONISTA);
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio.");
        }
        this.nombre = nombre.trim();
    }

    public void setCargo(Cargo cargo) {
        if (cargo == null) {
            throw new IllegalArgumentException("El cargo del empleado es obligatorio.");
        }
        this.cargo = cargo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /** Orden natural: por documento. */
    @Override
    public int compareTo(Empleado otro) {
        return this.documento.compareToIgnoreCase(otro.documento);
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof Empleado)) {
            return false;
        }
        return documento.equalsIgnoreCase(((Empleado) objeto).documento);
    }

    @Override
    public int hashCode() {
        return documento.toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return nombre + " (" + cargo.getDescripcion() + ")";
    }
}
