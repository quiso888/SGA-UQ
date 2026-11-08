package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.Huesped;
import model.Reserva;
import service.SistemaAlojamiento;

/**
 * Panel de huéspedes: lista filtrable, ficha completa, apartamentos favoritos
 * (sin duplicados, en un {@code Conjunto}) e historial de estancias.
 */
public class PanelHuespedes extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final JTextField busqueda = new JTextField();
    private final Modelos.ModeloLista<Huesped> modeloLista = new Modelos.ModeloLista<>();
    private final JList<Huesped> lista = new JList<>(modeloLista);
    private final JLabel contador = new JLabel(" ");

    private final JLabel nombre = new JLabel(" ");
    private final JLabel documento = new JLabel(" ");
    private final JLabel nacimiento = new JLabel(" ");
    private final JLabel telefono = new JLabel(" ");
    private final JLabel correo = new JLabel(" ");
    private final JLabel favoritos = new JLabel(" ");

    private final Modelos.ModeloLista<Apartamento> modeloFavoritos = new Modelos.ModeloLista<>();
    private final JComboBox<Apartamento> comboFavorito = new JComboBox<>(modeloFavoritos);
    private final ModeloTablaHistorial modeloHistorial = new ModeloTablaHistorial();
    private final JTable tablaHistorial = new JTable(modeloHistorial);

    private boolean actualizando;

    public PanelHuespedes(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);

        contexto.registrar(this);
        actualizar();
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Huéspedes"));
        textos.add(Estilo.etiquetaSuave(
                "Ficha, apartamentos favoritos e historial de estancias del huésped"));
        cabecera.add(textos, BorderLayout.WEST);

        JButton nuevo = Estilo.boton("Nuevo huésped", Estilo.EXITO);
        nuevo.addActionListener(evento -> nuevoHuesped());
        cabecera.add(nuevo, BorderLayout.EAST);
        return cabecera;
    }

    private JSplitPane construirCentro() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                construirListado(), construirFicha());
        split.setResizeWeight(0.32);
        split.setDividerLocation(360);
        split.setBorder(null);
        return split;
    }

    private JPanel construirListado() {
        JPanel panel = Estilo.seccion("Huéspedes registrados");

        JPanel superior = new JPanel(new BorderLayout(0, 6));
        superior.setOpaque(false);
        busqueda.setFont(Estilo.normal());
        busqueda.setToolTipText("Buscar por nombre o documento");
        busqueda.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evento) {
                filtrar();
            }
        });
        superior.add(new JLabel("Buscar por nombre o documento:"), BorderLayout.NORTH);
        superior.add(busqueda, BorderLayout.CENTER);
        panel.add(superior, BorderLayout.NORTH);

        lista.setFont(Estilo.normal());
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(30);
        lista.addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !actualizando) {
                mostrarSeleccionado();
            }
        });
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        contador.setFont(Estilo.pequena());
        contador.setForeground(Estilo.TEXTO_SUAVE);
        panel.add(contador, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel construirFicha() {
        JPanel derecha = new JPanel(new BorderLayout(0, 10));
        derecha.setOpaque(false);
        derecha.add(construirDatos(), BorderLayout.NORTH);
        derecha.add(construirHistorial(), BorderLayout.CENTER);
        return derecha;
    }

    private JPanel construirDatos() {
        JPanel ficha = Estilo.seccion("Ficha del huésped");

        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        nombre.setFont(Estilo.subtitulo());
        nombre.setForeground(Estilo.PRIMARIO_OSCURO);
        agregarDato(datos, 0, "Nombre:", nombre);
        agregarDato(datos, 1, "Documento:", documento);
        agregarDato(datos, 2, "Fecha de nacimiento:", nacimiento);
        agregarDato(datos, 3, "Teléfono:", telefono);
        agregarDato(datos, 4, "Correo:", correo);
        agregarDato(datos, 5, "Favoritos:", favoritos);

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(datos, BorderLayout.NORTH);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controles.setOpaque(false);
        controles.add(new JLabel("Apartamento:"));
        comboFavorito.setFont(Estilo.pequena());
        controles.add(comboFavorito);
        JButton alternar = Estilo.boton("Marcar / quitar favorito", Estilo.PRIMARIO);
        alternar.addActionListener(evento -> alternarFavorito());
        controles.add(alternar);
        contenido.add(controles, BorderLayout.CENTER);

        ficha.add(contenido, BorderLayout.CENTER);
        return ficha;
    }

    private void agregarDato(JPanel panel, int fila, String etiqueta, JLabel valor) {
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.insets = new Insets(2, 4, 2, 10);
        restricciones.anchor = GridBagConstraints.WEST;
        JLabel titulo = new JLabel(etiqueta);
        titulo.setFont(Estilo.pequena());
        titulo.setForeground(Estilo.TEXTO_SUAVE);
        panel.add(titulo, restricciones);

        restricciones.gridx = 1;
        restricciones.weightx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        valor.setFont(Estilo.normal());
        valor.setForeground(Estilo.TEXTO);
        panel.add(valor, restricciones);
    }

    private JPanel construirHistorial() {
        JPanel panel = Estilo.seccion("Historial de estancias");

        tablaHistorial.setRowHeight(24);
        tablaHistorial.setFont(Estilo.pequena());
        tablaHistorial.getTableHeader().setFont(Estilo.pequena());
        tablaHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tablaHistorial);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------
    // Datos
    // ------------------------------------------------------------------

    @Override
    public void actualizar() {
        actualizando = true;
        String documentoSeleccionado = null;
        Huesped seleccionado = lista.getSelectedValue();
        if (seleccionado != null) {
            documentoSeleccionado = seleccionado.getDocumento();
        }

        SistemaAlojamiento sistema = contexto.getSistema();
        modeloFavoritos.setDatos(sistema.todosApartamentos());
        filtrar();

        if (documentoSeleccionado != null) {
            for (int i = 0; i < modeloLista.getSize(); i++) {
                if (modeloLista.getElementAt(i).getDocumento().equals(documentoSeleccionado)) {
                    lista.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (lista.getSelectedIndex() < 0 && modeloLista.getSize() > 0) {
            lista.setSelectedIndex(0);
        }
        actualizando = false;
        mostrarSeleccionado();
    }

    private void filtrar() {
        SistemaAlojamiento sistema = contexto.getSistema();
        String texto = busqueda.getText() == null ? "" : busqueda.getText().trim();
        String normalizado = Huesped.normalizarDocumento(texto).toLowerCase();

        ListaEnlazada<Huesped> filtrados = new ListaEnlazada<>();
        for (Huesped huesped : sistema.todosHuespedes()) {
            if (texto.isEmpty()
                    || huesped.getNombre().toLowerCase().contains(texto.toLowerCase())
                    || Huesped.normalizarDocumento(huesped.getDocumento())
                    .toLowerCase().contains(normalizado)) {
                filtrados.agregar(huesped);
            }
        }
        modeloLista.setDatos(filtrados);
        contador.setText(filtrados.tamano() + " de " + sistema.todosHuespedes().tamano()
                + " huésped(es)");
    }

    private void mostrarSeleccionado() {
        Huesped huesped = lista.getSelectedValue();
        if (huesped == null) {
            nombre.setText("Sin huésped seleccionado");
            documento.setText("—");
            nacimiento.setText("—");
            telefono.setText("—");
            correo.setText("—");
            favoritos.setText("—");
            modeloHistorial.setFilas(new ListaEnlazada<Reserva>());
            return;
        }
        nombre.setText(huesped.getNombre());
        documento.setText(huesped.getDocumento());
        nacimiento.setText(Formato.fecha(huesped.getFechaNacimiento()));
        telefono.setText(huesped.getTelefono() == null ? "—" : huesped.getTelefono());
        correo.setText(huesped.getCorreo() == null ? "—" : huesped.getCorreo());

        StringBuilder codigos = new StringBuilder();
        for (String codigo : huesped.getFavoritos()) {
            if (codigos.length() > 0) {
                codigos.append(", ");
            }
            codigos.append(codigo);
        }
        favoritos.setText(codigos.length() == 0 ? "Ninguno" : codigos.toString());

        ListaEnlazada<Reserva> historial = new ListaEnlazada<>();
        for (Reserva reserva : huesped.getHistorial()) {
            historial.agregar(reserva);
        }
        modeloHistorial.setFilas(historial);
    }

    private void alternarFavorito() {
        Huesped huesped = lista.getSelectedValue();
        Apartamento apartamento = (Apartamento) comboFavorito.getSelectedItem();
        if (huesped == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un huésped.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (apartamento == null) {
            return;
        }
        boolean quedoFavorito = huesped.alternarFavorito(apartamento.getCodigo());
        mostrarSeleccionado();
        JOptionPane.showMessageDialog(this,
                (quedoFavorito ? "Se marcó " : "Se quitó ")
                        + apartamento.getCodigo() + (quedoFavorito ? " como favorito" : " de favoritos")
                        + " de " + huesped.getNombre() + ".",
                "Apartamentos favoritos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void nuevoHuesped() {
        if (DialogoHuesped.mostrar(this, contexto)) {
            contexto.notificar();
        }
    }

    // ------------------------------------------------------------------
    // Modelo del historial
    // ------------------------------------------------------------------

    private static class ModeloTablaHistorial extends javax.swing.table.AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] columnas = {"Reserva", "Apartamento", "Entrada", "Salida",
                "Noches", "Estado", "Total"};
        private ListaEnlazada<Reserva> filas = new ListaEnlazada<>();

        void setFilas(ListaEnlazada<Reserva> nuevas) {
            this.filas = nuevas == null ? new ListaEnlazada<Reserva>() : nuevas;
            fireTableDataChanged();
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
            Reserva reserva = filas.obtener(fila);
            switch (columna) {
                case 0:
                    return reserva.getCodigo();
                case 1:
                    return reserva.getApartamento().getCodigo();
                case 2:
                    return Formato.fecha(reserva.getEstancia().getFechaEntrada());
                case 3:
                    return Formato.fecha(reserva.getEstancia().getFechaSalida());
                case 4:
                    return reserva.getEstancia().numeroNoches();
                case 5:
                    return reserva.getEstado().getDescripcion();
                case 6:
                    return Formato.moneda(reserva.getTotalEstimado());
                default:
                    return "";
            }
        }
    }
}
