package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

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
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.EstadoReserva;
import model.Huesped;
import model.Movimiento;
import model.Reserva;
import service.SistemaAlojamiento;

/**
 * Panel de usuario del huésped: sus reservas con el saldo de cada folio, sus
 * apartamentos favoritos y el historial de sus estancias.
 *
 * Desde aquí el huésped también reserva por el portal (con búsqueda de
 * disponibilidad, orden elegido y cotización noche por noche), paga el anticipo y
 * cancela viendo antes cuánto se le devolvería.
 */
public class PanelUsuario extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS_RESERVAS = {"Reserva", "Apartamento", "Entrada",
            "Salida", "Estado", "Total", "Anticipo", "Saldo del folio"};
    private static final String[] COLUMNAS_HISTORIAL = {"Reserva", "Apartamento", "Entrada",
            "Salida", "Noches", "Estado", "Total"};

    private final Contexto contexto;
    private final JLabel titulo = Estilo.etiquetaTitulo("Mi cuenta");
    private final JLabel subtitulo = Estilo.etiquetaSuave(" ");

    private final Modelos.ModeloTabla<Reserva> modeloReservas = new Modelos.ModeloTabla<>(
            COLUMNAS_RESERVAS, (reserva, columna) -> valorReserva(reserva, columna), null);
    private final JTable tablaReservas = new JTable(modeloReservas);
    private final JTextArea folio = new JTextArea(8, 40);

    private final Modelos.ModeloLista<Apartamento> modeloFavoritos = new Modelos.ModeloLista<>();
    private final JList<Apartamento> listaFavoritos = new JList<>(modeloFavoritos);
    private final Modelos.ModeloLista<Apartamento> modeloCatalogo = new Modelos.ModeloLista<>();
    private final JComboBox<Apartamento> comboCatalogo = new JComboBox<>(modeloCatalogo);

    private final Modelos.ModeloTabla<Reserva> modeloHistorial = new Modelos.ModeloTabla<>(
            COLUMNAS_HISTORIAL, (reserva, columna) -> valorHistorial(reserva, columna), null);
    private final JTable tablaHistorial = new JTable(modeloHistorial);

    private boolean actualizando;

    public PanelUsuario(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);
        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                construirReservas(), construirLateral());
        centro.setResizeWeight(0.62);
        centro.setBorder(null);
        add(centro, BorderLayout.CENTER);

        contexto.registrar(this);
        actualizar();
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(titulo);
        textos.add(subtitulo);
        cabecera.add(textos, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton nueva = Estilo.boton("Buscar y reservar", Estilo.EXITO);
        nueva.addActionListener(evento -> nuevaReserva());
        botones.add(nueva);
        JButton anticipo = Estilo.boton("Pagar anticipo", Estilo.PRIMARIO);
        anticipo.addActionListener(evento -> pagarAnticipo());
        botones.add(anticipo);
        JButton cancelar = Estilo.boton("Cancelar reserva", Estilo.PELIGRO);
        cancelar.addActionListener(evento -> cancelar());
        botones.add(cancelar);
        cabecera.add(botones, BorderLayout.EAST);
        return cabecera;
    }

    private JPanel construirReservas() {
        JPanel panel = Estilo.seccion("Mis reservas activas y saldo de cada folio");

        tablaReservas.setRowHeight(26);
        tablaReservas.setFont(Estilo.pequena());
        tablaReservas.getTableHeader().setFont(Estilo.pequena());
        tablaReservas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaReservas.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !actualizando) {
                mostrarFolio();
            }
        });
        JScrollPane scroll = new JScrollPane(tablaReservas);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));

        folio.setEditable(false);
        folio.setFont(Estilo.mono());
        JScrollPane scrollFolio = new JScrollPane(folio);
        scrollFolio.setBorder(BorderFactory.createTitledBorder("Folio de la reserva seleccionada"));

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, scrollFolio);
        division.setResizeWeight(0.55);
        division.setBorder(null);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirLateral() {
        JPanel lateral = Estilo.transparente(new GridLayout(2, 1, 0, 12));
        lateral.add(construirFavoritos());
        lateral.add(construirHistorial());
        return lateral;
    }

    private JPanel construirFavoritos() {
        JPanel panel = Estilo.seccion("Mis apartamentos favoritos");

        listaFavoritos.setFont(Estilo.normal());
        listaFavoritos.setFixedCellHeight(26);
        JScrollPane scroll = new JScrollPane(listaFavoritos);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        controles.setOpaque(false);
        comboCatalogo.setFont(Estilo.pequena());
        controles.add(comboCatalogo);
        JButton alternar = Estilo.boton("Marcar / quitar", Estilo.PRIMARIO);
        alternar.addActionListener(evento -> alternarFavorito());
        controles.add(alternar);
        panel.add(controles, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel construirHistorial() {
        JPanel panel = Estilo.seccion("Historial de estancias");
        tablaHistorial.setRowHeight(24);
        tablaHistorial.setFont(Estilo.pequena());
        tablaHistorial.getTableHeader().setFont(Estilo.pequena());
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
        SistemaAlojamiento sistema = contexto.getSistema();
        Huesped huesped = contexto.getHuespedActual();

        String codigoSeleccionado = null;
        Reserva seleccionada = reservaSeleccionada();
        if (seleccionada != null) {
            codigoSeleccionado = seleccionada.getCodigo();
        }

        ListaEnlazada<Reserva> activas = new ListaEnlazada<>();
        ListaEnlazada<Reserva> historial = new ListaEnlazada<>();
        ListaEnlazada<Apartamento> favoritos = new ListaEnlazada<>();

        if (huesped == null) {
            titulo.setText("Mi cuenta");
            subtitulo.setText("No hay un huésped con sesión iniciada. Use Sesión → Cambiar de rol.");
        } else {
            titulo.setText("Mi cuenta · " + huesped.getNombre());
            subtitulo.setText("Documento " + huesped.getDocumento()
                    + "  ·  Reservas por el portal con anticipo del 30 %");
            for (Reserva reserva : huesped.getHistorial()) {
                if (esActiva(reserva)) {
                    activas.agregar(reserva);
                } else {
                    historial.agregar(reserva);
                }
            }
            for (String codigo : huesped.getFavoritos()) {
                Apartamento apartamento = sistema.getGestorReservas().buscarApartamentoOpcional(codigo);
                if (apartamento != null) {
                    favoritos.agregar(apartamento);
                }
            }
        }

        modeloReservas.setFilas(activas);
        modeloHistorial.setFilas(historial);
        modeloFavoritos.setDatos(favoritos);
        modeloCatalogo.setDatos(sistema.todosApartamentos());

        if (codigoSeleccionado != null) {
            for (int i = 0; i < modeloReservas.getRowCount(); i++) {
                if (modeloReservas.getFila(i).getCodigo().equals(codigoSeleccionado)) {
                    tablaReservas.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        actualizando = false;
        mostrarFolio();
    }

    private static boolean esActiva(Reserva reserva) {
        EstadoReserva estado = reserva.getEstado();
        return estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.CONFIRMADA
                || estado == EstadoReserva.EN_CURSO;
    }

    private Reserva reservaSeleccionada() {
        int fila = tablaReservas.getSelectedRow();
        if (fila < 0 || fila >= modeloReservas.getRowCount()) {
            return null;
        }
        return modeloReservas.getFila(fila);
    }

    private void mostrarFolio() {
        Reserva reserva = reservaSeleccionada();
        if (reserva == null) {
            folio.setText("Seleccione una reserva para ver su folio.");
            return;
        }
        StringBuilder texto = new StringBuilder();
        texto.append("Folio ").append(reserva.getFolio().getCodigo())
                .append("  ·  saldo: ").append(Formato.moneda(reserva.getFolio().saldo())).append('\n');
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            texto.append("Anticipo exigido: ").append(Formato.moneda(reserva.anticipoRequerido()))
                    .append("  ·  vence ").append(Formato.fechaHora(reserva.getFechaVencimiento()))
                    .append('\n');
        }
        texto.append("Si cancela hoy se le devolverían: ")
                .append(Formato.moneda(reserva.montoDevolucion())).append("\n\n");
        for (Movimiento movimiento : reserva.getFolio().getMovimientos()) {
            texto.append(movimiento).append('\n');
        }
        folio.setText(texto.toString());
        folio.setCaretPosition(0);
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private boolean haySesion() {
        if (contexto.getHuespedActual() == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay un huésped con sesión iniciada. Use Sesión → Cambiar de rol.",
                    "Sin sesión", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void nuevaReserva() {
        if (haySesion() && DialogoReserva.mostrar(this, contexto, contexto.getHuespedActual())) {
            contexto.notificar();
        }
    }

    private void pagarAnticipo() {
        Reserva reserva = reservaSeleccionada();
        if (reserva == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una de sus reservas.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AccionesReserva.registrarAnticipo(this, contexto, reserva);
    }

    private void cancelar() {
        Reserva reserva = reservaSeleccionada();
        if (reserva == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una de sus reservas.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AccionesReserva.cancelar(this, contexto, reserva);
    }

    private void alternarFavorito() {
        Huesped huesped = contexto.getHuespedActual();
        Apartamento apartamento = (Apartamento) comboCatalogo.getSelectedItem();
        if (huesped == null || apartamento == null) {
            return;
        }
        huesped.alternarFavorito(apartamento.getCodigo());
        contexto.notificar();
    }

    // ------------------------------------------------------------------
    // Valores de las tablas
    // ------------------------------------------------------------------

    private static Object valorReserva(Reserva reserva, int columna) {
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
                return reserva.getEstado().getDescripcion();
            case 5:
                return Formato.moneda(reserva.getTotalEstimado());
            case 6:
                return Formato.moneda(reserva.getAnticipoPagado());
            case 7:
                return Formato.moneda(reserva.getFolio().saldo());
            default:
                return "";
        }
    }

    private static Object valorHistorial(Reserva reserva, int columna) {
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
