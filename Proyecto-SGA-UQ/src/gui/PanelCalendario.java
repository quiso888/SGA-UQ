package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.EstadoApartamento;
import model.EstadoReserva;
import model.Reserva;
import model.Temporada;
import service.SistemaAlojamiento;

/**
 * Panel del calendario de ocupación: una fila por apartamento y una columna
 * por noche, con el código de la reserva que ocupa esa noche.
 *
 * La cabecera de cada columna muestra la fecha y la temporada a la que pertenece,
 * de modo que se ve de un vistazo cuándo cambian las tarifas. Las celdas se colorean
 * según el estado de la reserva y el estado operativo del apartamento.
 */
public class PanelCalendario extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private static final int DIAS_VISIBLES = 30;

    private final Contexto contexto;
    private final JTable tabla = new JTable();
    private final JLabel rango = new JLabel(" ");
    private final JLabel detalle = new JLabel("Seleccione una celda para ver el detalle.");
    private LocalDate inicio = LocalDate.now();

    public PanelCalendario(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);
        add(construirTabla(), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        detalle.setFont(Estilo.pequena());
        detalle.setForeground(Estilo.TEXTO_SUAVE);
        pie.add(detalle, BorderLayout.WEST);
        pie.add(construirLeyenda(), BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        contexto.registrar(this);
        actualizar();
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout(0, 8));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Calendario de ocupación"));
        textos.add(Estilo.etiquetaSuave(
                "La noche de salida no se ocupa: dos estancias que se tocan no se solapan."));
        cabecera.add(textos, BorderLayout.WEST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controles.setOpaque(false);

        JButton anteriorSemana = Estilo.boton("◀ 7 días", Estilo.PRIMARIO);
        anteriorSemana.addActionListener(evento -> mover(-7));
        controles.add(anteriorSemana);

        JButton anteriorDia = Estilo.boton("◀", Estilo.PRIMARIO);
        anteriorDia.addActionListener(evento -> mover(-1));
        controles.add(anteriorDia);

        JButton hoy = Estilo.boton("Hoy", Estilo.PRIMARIO_OSCURO);
        hoy.addActionListener(evento -> {
            inicio = LocalDate.now();
            actualizar();
        });
        controles.add(hoy);

        JButton siguienteDia = Estilo.boton("▶", Estilo.PRIMARIO);
        siguienteDia.addActionListener(evento -> mover(1));
        controles.add(siguienteDia);

        JButton siguienteSemana = Estilo.boton("7 días ▶", Estilo.PRIMARIO);
        siguienteSemana.addActionListener(evento -> mover(7));
        controles.add(siguienteSemana);

        rango.setFont(Estilo.normal());
        rango.setForeground(Estilo.TEXTO);
        controles.add(rango);
        cabecera.add(controles, BorderLayout.EAST);
        return cabecera;
    }

    private JScrollPane construirTabla() {
        tabla.setRowHeight(30);
        tabla.setFont(Estilo.pequena());
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setCellSelectionEnabled(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setGridColor(Estilo.BORDE);
        tabla.setShowGrid(true);

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setFont(Estilo.pequena());
        encabezado.setBackground(new Color(0xECEFF1));
        encabezado.setForeground(Estilo.TEXTO);
        encabezado.setReorderingAllowed(false);

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                mostrarDetalle(tabla.getSelectedRow(), tabla.getSelectedColumn());
            }
        });

        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(javax.swing.BorderFactory.createLineBorder(Estilo.BORDE));
        desplazamiento.getViewport().setBackground(Color.WHITE);
        return desplazamiento;
    }

    private JPanel construirLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        leyenda.setOpaque(false);
        leyenda.add(item("Pendiente", new Color(0xFFF8E1)));
        leyenda.add(item("Confirmada", new Color(0xE3F2FD)));
        leyenda.add(item("En curso", new Color(0xE8F5E9)));
        leyenda.add(item("Fuera de servicio", new Color(0xFFEBEE)));
        return leyenda;
    }

    private JPanel item(String texto, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel muestra = new JLabel("   ");
        muestra.setOpaque(true);
        muestra.setBackground(color);
        muestra.setBorder(javax.swing.BorderFactory.createLineBorder(Estilo.BORDE));
        item.add(muestra);
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Estilo.pequena());
        etiqueta.setForeground(Estilo.TEXTO_SUAVE);
        item.add(etiqueta);
        return item;
    }

    private void mover(int dias) {
        inicio = inicio.plusDays(dias);
        actualizar();
    }

    @Override
    public void actualizar() {
        SistemaAlojamiento sistema = contexto.getSistema();
        ModeloCalendario modelo = new ModeloCalendario(sistema, sistema.todosApartamentos(),
                inicio, DIAS_VISIBLES);
        tabla.setModel(modelo);
        tabla.setDefaultRenderer(Object.class, new RenderizadorCalendario());

        tabla.getColumnModel().getColumn(0).setPreferredWidth(118);
        for (int columna = 1; columna <= DIAS_VISIBLES; columna++) {
            tabla.getColumnModel().getColumn(columna).setPreferredWidth(74);
        }

        rango.setText(Formato.fecha(inicio) + "  →  "
                + Formato.fecha(inicio.plusDays(DIAS_VISIBLES - 1)));
        detalle.setText("Seleccione una celda para ver el detalle.");
    }

    private void mostrarDetalle(int fila, int columna) {
        if (fila < 0 || columna <= 0 || !(tabla.getModel() instanceof ModeloCalendario)) {
            return;
        }
        ModeloCalendario modelo = (ModeloCalendario) tabla.getModel();
        LocalDate fecha = modelo.fechaDeColumna(columna);
        Apartamento apartamento = modelo.apartamentoDeFila(fila);
        Reserva reserva = modelo.reservaEn(fila, columna);

        if (reserva == null) {
            detalle.setText(apartamento.getCodigo() + " · " + Formato.fecha(fecha)
                    + " · libre · apartamento " + apartamento.getEstado().getDescripcion().toLowerCase());
            return;
        }
        detalle.setText(reserva.getCodigo() + " · " + apartamento.getCodigo() + " · "
                + Formato.fecha(fecha) + " · " + reserva.getEstado().getDescripcion()
                + " · " + reserva.getTitular().getNombre()
                + " · canal " + reserva.getCanal().getDescripcion()
                + " · temporada " + modelo.temporadaDe(fecha));
    }

    // ------------------------------------------------------------------
    // Modelo de tabla del calendario
    // ------------------------------------------------------------------

    private static class ModeloCalendario extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final SistemaAlojamiento sistema;
        private final ListaEnlazada<Apartamento> apartamentos;
        private final LocalDate inicio;
        private final int dias;

        ModeloCalendario(SistemaAlojamiento sistema, ListaEnlazada<Apartamento> apartamentos,
                         LocalDate inicio, int dias) {
            this.sistema = sistema;
            this.apartamentos = apartamentos;
            this.inicio = inicio;
            this.dias = dias;
        }

        LocalDate fechaDeColumna(int columna) {
            return inicio.plusDays(columna - 1);
        }

        Apartamento apartamentoDeFila(int fila) {
            return apartamentos.obtener(fila);
        }

        Temporada temporadaDe(LocalDate fecha) {
            return sistema.getCalendario().temporadaDe(fecha);
        }

        Reserva reservaEn(int fila, int columna) {
            if (columna <= 0 || fila < 0 || fila >= apartamentos.tamano()) {
                return null;
            }
            LocalDate fecha = fechaDeColumna(columna);
            Apartamento apartamento = apartamentos.obtener(fila);
            for (Reserva reserva : apartamento.getReservas()) {
                if (reserva.estaVigente() && reserva.getEstancia().ocupaNoche(fecha)) {
                    return reserva;
                }
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return apartamentos.tamano();
        }

        @Override
        public int getColumnCount() {
            return dias + 1;
        }

        @Override
        public String getColumnName(int columna) {
            if (columna == 0) {
                return "Apartamento";
            }
            LocalDate fecha = fechaDeColumna(columna);
            return "<html><center>" + String.format("%02d/%02d", fecha.getDayOfMonth(),
                    fecha.getMonthValue()) + "<br><small>"
                    + temporadaDe(fecha).getDescripcion() + "</small></center></html>";
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Apartamento apartamento = apartamentos.obtener(fila);
            if (columna == 0) {
                return apartamento.getCodigo();
            }
            Reserva reserva = reservaEn(fila, columna);
            return reserva == null ? "" : reserva.getCodigo();
        }
    }

    // ------------------------------------------------------------------
    // Renderizador de celdas
    // ------------------------------------------------------------------

    private static class RenderizadorCalendario extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                       boolean seleccionado, boolean enfocado,
                                                       int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionado, enfocado, fila, columna);
            setHorizontalAlignment(CENTER);
            setFont(Estilo.pequena());
            setToolTipText(null);

            if (!(tabla.getModel() instanceof ModeloCalendario)) {
                return this;
            }
            ModeloCalendario modelo = (ModeloCalendario) tabla.getModel();
            if (columna == 0) {
                setHorizontalAlignment(LEFT);
                setBackground(Estilo.FONDO);
                setForeground(Estilo.TEXTO);
                setText(String.valueOf(valor));
                return this;
            }

            LocalDate fecha = modelo.fechaDeColumna(columna);
            Reserva reserva = modelo.reservaEn(fila, columna);
            Apartamento apartamento = modelo.apartamentoDeFila(fila);

            if (reserva != null) {
                if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
                    setBackground(new Color(0xFFF8E1));
                    setForeground(new Color(0x8D6E00));
                } else if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
                    setBackground(new Color(0xE3F2FD));
                    setForeground(new Color(0x0D47A1));
                } else {
                    setBackground(new Color(0xE8F5E9));
                    setForeground(new Color(0x1B5E20));
                }
                setToolTipText(reserva.getCodigo() + " · " + reserva.getTitular().getNombre()
                        + " · " + reserva.getEstado().getDescripcion());
            } else if (apartamento.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) {
                setBackground(new Color(0xFFEBEE));
                setForeground(Estilo.PELIGRO);
                setToolTipText("Apartamento fuera de servicio");
            } else {
                setBackground(Color.WHITE);
                setForeground(fecha.isBefore(LocalDate.now())
                        ? new Color(0xB0BEC5) : Estilo.TEXTO_SUAVE);
            }

            if (seleccionado) {
                setBackground(getBackground().darker());
            }
            return this;
        }
    }
}
