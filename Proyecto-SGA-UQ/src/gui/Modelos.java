package gui;

import datastructures.ListaEnlazada;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.table.AbstractTableModel;

/**
 * Adaptadores entre las estructuras de datos propias del SGA y los modelos de
 * Swing.
 *
 * El núcleo del sistema usa únicamente { ListaEnlazada} y las demás
 * estructuras propias; para mostrarlas en pantalla se envuelven en modelos de
 * Swing ({AbstractListModel}, { ComboBoxModel} y
 * {AbstractTableModel}), que pertenecen a la interfaz y no al núcleo.
 */
public final class Modelos {

    private Modelos() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Modelo de lista y de combo respaldado por una {@link ListaEnlazada}.
     */
    public static class ModeloLista<T> extends AbstractListModel<T> implements ComboBoxModel<T> {

        private static final long serialVersionUID = 1L;

        private ListaEnlazada<T> datos;
        private Object seleccionado;

        public ModeloLista() {
            this.datos = new ListaEnlazada<>();
        }

        public ModeloLista(ListaEnlazada<T> datos) {
            this.datos = datos == null ? new ListaEnlazada<T>() : datos;
        }

        /** Reemplaza los datos del modelo y notifica a la vista. */
        public void setDatos(ListaEnlazada<T> nuevos) {
            this.datos = nuevos == null ? new ListaEnlazada<T>() : nuevos;
            if (datos.estaVacia()) {
                seleccionado = null;
            } else if (seleccionado == null || !datos.contiene((T) seleccionado)) {
                seleccionado = datos.primero();
            }
            fireContentsChanged(this, 0, Math.max(0, datos.tamano() - 1));
        }

        public ListaEnlazada<T> getDatos() {
            return datos;
        }

        /** Elemento seleccionado, ya convertido al tipo del modelo. */
        public T getSeleccionado() {
            return (T) seleccionado;
        }

        @Override
        public int getSize() {
            return datos.tamano();
        }

        @Override
        public T getElementAt(int indice) {
            return datos.obtener(indice);
        }

        @Override
        public void setSelectedItem(Object item) {
            seleccionado = item;
            fireContentsChanged(this, -1, -1);
        }

        @Override
        public Object getSelectedItem() {
            return seleccionado;
        }
    }

    /**
     * Modelo de tabla genérico: una fila por elemento de la lista y una columna por
     * cada valor que devuelve el {@link Extractor}.
     */
    public static class ModeloTabla<T> extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        /** Obtiene el valor de una columna para un elemento. */
        public interface Extractor<T> {
            Object valor(T elemento, int columna);
        }

        private final String[] columnas;
        private final Extractor<T> extractor;
        private ListaEnlazada<T> filas;

        public ModeloTabla(String[] columnas, Extractor<T> extractor, ListaEnlazada<T> filas) {
            this.columnas = columnas;
            this.extractor = extractor;
            this.filas = filas == null ? new ListaEnlazada<T>() : filas;
        }

        public void setFilas(ListaEnlazada<T> nuevas) {
            this.filas = nuevas == null ? new ListaEnlazada<T>() : nuevas;
            fireTableDataChanged();
        }

        public ListaEnlazada<T> getFilas() {
            return filas;
        }

        public T getFila(int indice) {
            return filas.obtener(indice);
        }

        @Override
        public int getRowCount() {
            return filas.tamano();
        }

        @Override
        public int getColumnCount() {
            return columnas.length;
        }

        @Override
        public String getColumnName(int columna) {
            return columnas[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            return extractor.valor(filas.obtener(fila), columna);
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    }
}
