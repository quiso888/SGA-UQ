package gui;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import datastructures.ListaEnlazada;

/**
 * Formularios sencillos de una columna de campos para las pantallas de
 * administración: texto libre, texto de solo lectura o lista de opciones.
 */
final class Formularios {

    /** Un campo del formulario. */
    static final class Campo {
        private final String etiqueta;
        private final String valor;
        private final boolean editable;
        private final String[] opciones;

        private Campo(String etiqueta, String valor, boolean editable, String[] opciones) {
            this.etiqueta = etiqueta;
            this.valor = valor == null ? "" : valor;
            this.editable = editable;
            this.opciones = opciones;
        }

        /** Campo de texto editable. */
        static Campo texto(String etiqueta, String valor) {
            return new Campo(etiqueta, valor, true, null);
        }

        /** Campo de texto que solo se muestra. */
        static Campo fijo(String etiqueta, String valor) {
            return new Campo(etiqueta, valor, false, null);
        }

        /** Lista desplegable de opciones. */
        static Campo opciones(String etiqueta, String valor, String... opciones) {
            return new Campo(etiqueta, valor, true, opciones);
        }
    }

    private Formularios() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Muestra el formulario y devuelve los valores en el mismo orden de los campos,
     * o {@code null} si el usuario cancela.
     */
    static String[] pedir(Component padre, String titulo, ListaEnlazada<Campo> campos) {
        JPanel panel = new JPanel(new GridLayout(campos.tamano(), 2, 8, 6));
        JComponent[] controles = new JComponent[campos.tamano()];
        int indice = 0;
        for (Campo campo : campos) {
            JLabel etiqueta = new JLabel(campo.etiqueta);
            etiqueta.setFont(Estilo.normal());
            panel.add(etiqueta);
            JComponent control;
            if (campo.opciones != null) {
                JComboBox<String> combo = new JComboBox<>(campo.opciones);
                combo.setEditable(true);
                combo.setSelectedItem(campo.valor);
                control = combo;
            } else {
                JTextField texto = new JTextField(campo.valor, 22);
                texto.setEditable(campo.editable);
                control = texto;
            }
            control.setFont(Estilo.normal());
            controles[indice++] = control;
            panel.add(control);
        }
        int opcion = JOptionPane.showConfirmDialog(padre, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return null;
        }
        String[] valores = new String[controles.length];
        for (int i = 0; i < controles.length; i++) {
            Object valor = controles[i] instanceof JComboBox
                    ? ((JComboBox<?>) controles[i]).getSelectedItem()
                    : ((JTextField) controles[i]).getText();
            valores[i] = valor == null ? "" : valor.toString().trim();
        }
        return valores;
    }

    /** Atajo para construir la lista de campos. */
    static ListaEnlazada<Campo> campos(Campo... campos) {
        ListaEnlazada<Campo> lista = new ListaEnlazada<>();
        for (Campo campo : campos) {
            lista.agregar(campo);
        }
        return lista;
    }
}
