package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;

import datastructures.ListaEnlazada;
import exceptions.EstructuraVaciaException;
import exceptions.FechasInvalidasException;
import exceptions.SgaException;
import exceptions.SinAlojamientoDisponibleException;
import exceptions.TransicionEstadoInvalidaException;
import model.Apartamento;
import model.EstadoReserva;
import model.Estancia;
import model.Movimiento;
import model.Reserva;
import model.SolicitudExterna;
import service.SistemaAlojamiento;

/**
 * Panel de reservas: consulta el listado completo, permite ordenarlo por
 * distintos criterios y ejecuta el ciclo de vida de la reserva (anticipo, check-in,
 * check-out, cancelación y no-show).
 *
 * A la derecha se muestra el canal externo: su cola FIFO de solicitudes, los
 * botones para procesarlas y la bitácora de solicitudes aceptadas, rechazadas y
 * conflictos de canal.
 */
public class PanelReservas extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final JTable tabla = new JTable();
    private final Modelos.ModeloTabla<Reserva> modelo =
            new Modelos.ModeloTabla<>(new String[] {
                    "Código", "Titular", "Apartamento", "Canal", "Entrada", "Salida",
                    "Noches", "Personas", "Estado", "Total", "Anticipo"
            }, new ExtractorReserva(), new ListaEnlazada<Reserva>());
    private final JTextArea detalle = new JTextArea(14, 40);
    private final JComboBox<String> orden = new JComboBox<>(new String[] {
            "Fecha de entrada", "Titular (A-Z)", "Estado", "Total (mayor a menor)"
    });
    private final Modelos.ModeloLista<SolicitudExterna> modeloExternas = new Modelos.ModeloLista<>();
    private final JList<SolicitudExterna> listaExternas = new JList<>(modeloExternas);
    private final JLabel conflictos = new JLabel(" ");
    private final JTextArea bitacora = new JTextArea(9, 30);

    public PanelReservas(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);

        contexto.registrar(this);
        actualizar();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout(0, 8));
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Reservas"));
        textos.add(Estilo.etiquetaSuave(
                "Pendiente → Confirmada → En curso → Finalizada · con salidas a Cancelada o No-show"));
        cabecera.add(textos, BorderLayout.WEST);

        JPanel ordenPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ordenPanel.setOpaque(false);
        ordenPanel.add(new JLabel("Ordenar por:"));
        orden.setFont(Estilo.pequena());
        orden.addActionListener(evento -> actualizar());
        ordenPanel.add(orden);
        cabecera.add(ordenPanel, BorderLayout.EAST);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barra.setOpaque(false);

        JButton nueva = Estilo.boton("Nueva reserva", Estilo.EXITO);
        nueva.addActionListener(evento -> nuevaReserva());
        barra.add(nueva);

        JButton anticipo = Estilo.boton("Registrar anticipo", Estilo.PRIMARIO);
        anticipo.addActionListener(evento -> registrarAnticipo());
        barra.add(anticipo);

        JButton cargo = Estilo.boton("Registrar cargo", new Color(0x6A1B9A));
        cargo.addActionListener(evento -> registrarMovimiento(true));
        barra.add(cargo);

        JButton pago = Estilo.boton("Registrar pago", Estilo.EXITO);
        pago.addActionListener(evento -> registrarMovimiento(false));
        barra.add(pago);

        JButton checkIn = Estilo.boton("Check-in", Estilo.PRIMARIO_OSCURO);
        checkIn.addActionListener(evento -> checkIn());
        barra.add(checkIn);

        JButton checkOut = Estilo.boton("Check-out", Estilo.ADVERTENCIA);
        checkOut.addActionListener(evento -> checkOut());
        barra.add(checkOut);

        JButton cancelar = Estilo.boton("Cancelar", Estilo.PELIGRO);
        cancelar.addActionListener(evento -> cancelarReserva());
        barra.add(cancelar);

        JButton noShow = Estilo.boton("No-show", new Color(0x8E244D));
        noShow.addActionListener(evento -> registrarNoShow());
        barra.add(noShow);

        JButton vencimientos = Estilo.boton("Procesar vencimientos", Estilo.TEXTO_SUAVE);
        vencimientos.addActionListener(evento -> procesarVencimientos());
        barra.add(vencimientos);

        JButton deshacer = Estilo.boton("Deshacer folio", Estilo.TEXTO_SUAVE);
        deshacer.addActionListener(evento -> deshacerFolio());
        barra.add(deshacer);

        JButton grupo = Estilo.boton("Asignar grupo grande", Estilo.PRIMARIO);
        grupo.addActionListener(evento -> asignarGrupo());
        barra.add(grupo);

        cabecera.add(barra, BorderLayout.SOUTH);
        return cabecera;
    }

    private JSplitPane construirCentro() {
        tabla.setModel(modelo);
        tabla.setRowHeight(24);
        tabla.setFont(Estilo.pequena());
        tabla.getTableHeader().setFont(Estilo.pequena());
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(false);
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));

        detalle.setEditable(false);
        detalle.setFont(Estilo.mono());
        detalle.setBackground(new Color(0xFAFBFC));
        detalle.setBorder(Estilo.relleno(8));
        JScrollPane scrollDetalle = new JScrollPane(detalle);
        scrollDetalle.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), "Detalle de la reserva y folio"));

        JSplitPane izquierda = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, scrollDetalle);
        izquierda.setResizeWeight(0.55);
        izquierda.setDividerLocation(300);
        izquierda.setBorder(null);

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda,
                construirPanelExterno());
        centro.setResizeWeight(0.70);
        centro.setDividerLocation(760);
        centro.setBorder(null);
        return centro;
    }

    private JPanel construirPanelExterno() {
        JPanel panel = Estilo.seccion("Canal externo · cola FIFO");
        panel.setPreferredSize(new java.awt.Dimension(330, 500));

        listaExternas.setFont(Estilo.pequena());
        listaExternas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaExternas.setCellRenderer(new RenderizadorSolicitud());
        JScrollPane scrollLista = new JScrollPane(listaExternas);
        scrollLista.setPreferredSize(new java.awt.Dimension(300, 150));
        panel.add(scrollLista, BorderLayout.CENTER);

        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.setOpaque(false);
        JButton siguiente = Estilo.boton("Procesar siguiente", Estilo.PRIMARIO);
        siguiente.addActionListener(evento -> procesarExterna(false));
        botones.add(siguiente);
        JButton todas = Estilo.boton("Procesar todas", Estilo.PRIMARIO_OSCURO);
        todas.addActionListener(evento -> procesarExterna(true));
        botones.add(todas);
        sur.add(botones, BorderLayout.NORTH);

        conflictos.setFont(Estilo.pequena());
        conflictos.setForeground(Estilo.PELIGRO);
        sur.add(conflictos, BorderLayout.CENTER);

        bitacora.setEditable(false);
        bitacora.setFont(Estilo.mono());
        bitacora.setBackground(new Color(0xFAFBFC));
        JScrollPane scrollBitacora = new JScrollPane(bitacora);
        scrollBitacora.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), "Bitácora del canal"));
        sur.add(scrollBitacora, BorderLayout.SOUTH);

        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------------
    // Refresco de datos
    // ------------------------------------------------------------------

    @Override
    public void actualizar() {
        SistemaAlojamiento sistema = contexto.getSistema();

        String codigoSeleccionado = null;
        int fila = tabla.getSelectedRow();
        if (fila >= 0 && fila < modelo.getRowCount()) {
            codigoSeleccionado = modelo.getFila(fila).getCodigo();
        }

        ListaEnlazada<Reserva> reservas = sistema.todasReservas();
        reservas.ordenar(comparadorDeOrden());
        modelo.setFilas(reservas);

        if (tabla.getColumnCount() > 8) {
            tabla.getColumnModel().getColumn(8).setCellRenderer(new RenderizadorEstado());
            tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
            tabla.getColumnModel().getColumn(1).setPreferredWidth(170);
            tabla.getColumnModel().getColumn(2).setPreferredWidth(90);
            tabla.getColumnModel().getColumn(8).setPreferredWidth(95);
            tabla.getColumnModel().getColumn(9).setPreferredWidth(110);
            tabla.getColumnModel().getColumn(10).setPreferredWidth(100);
        }

        if (codigoSeleccionado != null) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (modelo.getFila(i).getCodigo().equals(codigoSeleccionado)) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        if (tabla.getSelectedRow() < 0) {
            detalle.setText("Seleccione una reserva para ver su detalle, sus ocupantes y el folio.");
        } else {
            mostrarDetalle();
        }

        modeloExternas.setDatos(sistema.getProcesadorCanalExterno().solicitudesPendientes());
        conflictos.setText("Conflictos de canal registrados: "
                + sistema.getProcesadorCanalExterno().numeroConflictos()
                + "   ·   Solicitudes sin procesar: "
                + sistema.getProcesadorCanalExterno().numeroSolicitudesPendientes());
        actualizarBitacora(sistema);
    }

    private void actualizarBitacora(SistemaAlojamiento sistema) {
        StringBuilder texto = new StringBuilder();
        for (SolicitudExterna solicitud : sistema.getProcesadorCanalExterno().getProcesadas()) {
            texto.append("• ").append(solicitud.getIdentificador()).append(" · ")
                    .append(solicitud.getCodigoApartamento()).append(" · ")
                    .append(solicitud.getEstado().getDescripcion());
            if (solicitud.getMotivo() != null) {
                texto.append(" → ").append(solicitud.getMotivo());
            }
            texto.append('\n');
        }
        for (model.ConflictoCanal conflicto : sistema.getProcesadorCanalExterno().getConflictos()) {
            texto.append("✖ Conflicto ").append(conflicto.getIdentificadorSolicitud())
                    .append(" (").append(conflicto.getCodigoApartamento()).append("): ")
                    .append(conflicto.getMotivo()).append('\n');
        }
        if (texto.length() == 0) {
            texto.append("Sin solicitudes procesadas todavía.");
        }
        bitacora.setText(texto.toString());
        bitacora.setCaretPosition(0);
    }

    private java.util.Comparator<Reserva> comparadorDeOrden() {
        int indice = orden.getSelectedIndex();
        if (indice == 1) {
            return (a, b) -> a.getTitular().getNombre().compareToIgnoreCase(b.getTitular().getNombre());
        }
        if (indice == 2) {
            return (a, b) -> {
                int estado = Integer.compare(a.getEstado().ordinal(), b.getEstado().ordinal());
                return estado != 0 ? estado : a.getEstancia().getFechaEntrada()
                        .compareTo(b.getEstancia().getFechaEntrada());
            };
        }
        if (indice == 3) {
            return (a, b) -> {
                int total = Double.compare(b.getTotalEstimado(), a.getTotalEstimado());
                return total != 0 ? total : a.getCodigo().compareTo(b.getCodigo());
            };
        }
        return (a, b) -> a.compareTo(b);
    }

    private void mostrarDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= modelo.getRowCount()) {
            return;
        }
        Reserva reserva = modelo.getFila(fila);
        StringBuilder texto = new StringBuilder();
        texto.append("Reserva ").append(reserva.getCodigo()).append('\n');
        texto.append("Titular: ").append(reserva.getTitular().getNombre())
                .append("  ·  Documento: ").append(reserva.getTitular().getDocumento()).append('\n');
        texto.append("Apartamento: ").append(reserva.getApartamento().getCodigo())
                .append(" · ").append(reserva.getApartamento().getNombre()).append('\n');
        texto.append("Canal: ").append(reserva.getCanal().getDescripcion())
                .append("  ·  Estado: ").append(reserva.getEstado().getDescripcion()).append('\n');
        texto.append("Entrada: ").append(Formato.fecha(reserva.getEstancia().getFechaEntrada()))
                .append("  ·  Salida: ").append(Formato.fecha(reserva.getEstancia().getFechaSalida()))
                .append("  ·  ").append(reserva.getEstancia().numeroNoches()).append(" noches\n");
        texto.append("Hora estimada de llegada: ").append(reserva.getHoraEstimadaLlegada())
                .append("  ·  Personas: ").append(reserva.numeroPersonas()).append('\n');
        texto.append("Total estimado: ").append(Formato.moneda(reserva.getTotalEstimado()))
                .append("  ·  Anticipo: ").append(Formato.moneda(reserva.getAnticipoPagado()))
                .append(" de ").append(Formato.moneda(reserva.anticipoRequerido())).append('\n');
        if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
            texto.append("Vence: ").append(Formato.fechaHora(reserva.getFechaVencimiento()))
                    .append(" (24 horas para pagar el anticipo)\n");
        }
        if (reserva.getMotivoCierre() != null) {
            texto.append("Motivo de cierre: ").append(reserva.getMotivoCierre()).append('\n');
        }

        texto.append("\nOcupantes:\n");
        for (model.Ocupante ocupante : reserva.getOcupantes()) {
            texto.append("  - ").append(ocupante.getNombre());
            if (ocupante.esTitular()) {
                texto.append(" (titular)");
            }
            if (!ocupante.pagaEn(reserva.getEstancia().getFechaEntrada())) {
                texto.append(" · menor de 6 años: ocupa cupo sin pagar");
            }
            texto.append('\n');
        }

        texto.append("\nFolio ").append(reserva.getFolio().getCodigo())
                .append("  ·  saldo: ").append(Formato.moneda(reserva.getFolio().saldo())).append('\n');
        for (Movimiento movimiento : reserva.getFolio().getMovimientos()) {
            texto.append("  ").append(movimiento).append('\n');
        }
        texto.append("  Operaciones deshacibles en la pila: ")
                .append(reserva.getFolio().getPilaOperaciones().tamano()).append('\n');

        detalle.setText(texto.toString());
        detalle.setCaretPosition(0);
    }

    // ------------------------------------------------------------------
    // Acciones del ciclo de vida
    // ------------------------------------------------------------------

    private Reserva seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= modelo.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Seleccione una reserva de la tabla.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return modelo.getFila(fila);
    }

    private void nuevaReserva() {
        if (DialogoReserva.mostrar(this, contexto)) {
            contexto.notificar();
        }
    }

    private void registrarAnticipo() {
        Reserva reserva = seleccionada();
        if (reserva != null) {
            AccionesReserva.registrarAnticipo(this, contexto, reserva);
        }
    }

    /**
     * Registra un cargo adicional o un pago en el folio de la reserva seleccionada.
     * Los movimientos nunca se borran; las correcciones se hacen con el botón de
     * deshacer, que agrega el movimiento contrario.
     */
    private void registrarMovimiento(boolean esCargo) {
        Reserva reserva = seleccionada();
        if (reserva == null) {
            return;
        }
        String conceptoInicial = esCargo
                ? "Cargo adicional de la estancia"
                : "Pago de la estancia";
        double saldo = reserva.getFolio().saldo();
        String valorInicial = String.format(java.util.Locale.ROOT, "%.0f",
                !esCargo && saldo > 0 ? saldo : reserva.getTotalEstimado());

        JTextField concepto = new JTextField(conceptoInicial, 22);
        JTextField valor = new JTextField(valorInicial, 12);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel(esCargo ? "Concepto del cargo:" : "Concepto del pago:"));
        formulario.add(concepto);
        formulario.add(new JLabel("Valor (COP):"));
        formulario.add(valor);
        formulario.add(new JLabel("Saldo actual del folio:"));
        formulario.add(new JLabel(Formato.moneda(saldo)));

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                esCargo ? "Registrar cargo en el folio" : "Registrar pago en el folio",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        double monto;
        try {
            monto = leerMonto(valor.getText());
        } catch (NumberFormatException excepcion) {
            mensajeError("Monto inválido", "Escriba un valor numérico, por ejemplo 150000.");
            return;
        }
        if (monto <= 0) {
            mensajeError("Monto inválido", "El valor debe ser mayor que cero.");
            return;
        }
        String textoConcepto = concepto.getText() == null || concepto.getText().trim().isEmpty()
                ? conceptoInicial : concepto.getText().trim();

        Movimiento movimiento = esCargo
                ? reserva.getFolio().registrarCargo(textoConcepto, monto)
                : reserva.getFolio().registrarPago(textoConcepto, monto);
        contexto.notificar();
        JOptionPane.showMessageDialog(this,
                "Movimiento registrado:\n" + movimiento
                        + "\n\nNuevo saldo del folio: "
                        + Formato.moneda(reserva.getFolio().saldo()),
                "Folio actualizado", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Convierte un texto de monto en pesos a número, aceptando puntos y comas. */
    private double leerMonto(String texto) {
        return AccionesReserva.leerMonto(texto);
    }

    private void checkIn() {        Reserva reserva = seleccionada();
        if (reserva == null) {
            return;
        }
        try {
            contexto.getSistema().registrarCheckIn(reserva.getCodigo());
            contexto.notificar();
        } catch (TransicionEstadoInvalidaException excepcion) {
            mensajeError("Check-in no permitido",
                    excepcion.getMessage()
                            + "\n\nRecuerde: el apartamento debe estar Preparado y la reserva Confirmada.");
        } catch (SgaException excepcion) {
            mensajeError("No se pudo registrar el check-in", excepcion.getMessage());
        }
    }

    private void checkOut() {
        Reserva reserva = seleccionada();
        if (reserva == null) {
            return;
        }
        try {
            contexto.getSistema().registrarCheckOut(reserva.getCodigo());
            contexto.notificar();
            JOptionPane.showMessageDialog(this,
                    "Check-out registrado. El apartamento " + reserva.getApartamento().getCodigo()
                            + " quedó pendiente de preparación y entró en la cola de prioridad.",
                    "Check-out", JOptionPane.INFORMATION_MESSAGE);
        } catch (TransicionEstadoInvalidaException excepcion) {
            mensajeError("Check-out no permitido", excepcion.getMessage());
        } catch (SgaException excepcion) {
            mensajeError("No se pudo registrar el check-out", excepcion.getMessage());
        }
    }

    private void cancelarReserva() {
        Reserva reserva = seleccionada();
        if (reserva != null) {
            AccionesReserva.cancelar(this, contexto, reserva);
        }
    }

    private void registrarNoShow() {
        Reserva reserva = seleccionada();
        if (reserva == null) {
            return;
        }
        String motivo = JOptionPane.showInputDialog(this,
                "Motivo del no-show de " + reserva.getCodigo() + ":",
                "Registro de no-show", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        try {
            contexto.getSistema().registrarNoShow(reserva.getCodigo(), motivo);
            contexto.notificar();
        } catch (TransicionEstadoInvalidaException excepcion) {
            mensajeError("No-show no permitido", excepcion.getMessage());
        } catch (SgaException excepcion) {
            mensajeError("No se pudo registrar el no-show", excepcion.getMessage());
        }
    }

    private void procesarVencimientos() {
        ListaEnlazada<Reserva> canceladas =
                contexto.getSistema().procesarVencimientos(LocalDateTime.now());
        contexto.notificar();
        JOptionPane.showMessageDialog(this,
                canceladas.estaVacia()
                        ? "No hay reservas pendientes vencidas en este momento."
                        : "Se cancelaron automáticamente " + canceladas.tamano()
                        + " reserva(s) por superar las 24 horas sin anticipo.",
                "Vencimientos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deshacerFolio() {
        Reserva reserva = seleccionada();
        if (reserva == null) {
            return;
        }
        try {
            Movimiento inverso = contexto.getSistema().getGestorFolios()
                    .deshacerUltimaOperacion(reserva.getCodigo());
            contexto.notificar();
            JOptionPane.showMessageDialog(this,
                    "Se registró el contra-movimiento:\n" + inverso,
                    "Folio corregido", JOptionPane.INFORMATION_MESSAGE);
        } catch (EstructuraVaciaException excepcion) {
            mensajeError("No hay operaciones por deshacer", excepcion.getMessage());
        } catch (SgaException excepcion) {
            mensajeError("No se pudo deshacer la operación", excepcion.getMessage());
        }
    }

    /**
     * Diálogo para buscar la combinación mínima de apartamentos de un grupo grande
     * (backtracking).
     */
    private void asignarGrupo() {
        JTextField entrada = new JTextField(LocalDate.now().plusDays(30).toString(), 12);
        JTextField salida = new JTextField(LocalDate.now().plusDays(33).toString(), 12);
        JSpinner personas = new JSpinner(new SpinnerNumberModel(10, 1, 60, 1));

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel("Entrada (aaaa-mm-dd):"));
        formulario.add(entrada);
        formulario.add(new JLabel("Salida (aaaa-mm-dd):"));
        formulario.add(salida);
        formulario.add(new JLabel("Personas del grupo:"));
        formulario.add(personas);

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                "Asignar grupo grande (backtracking)", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Estancia estancia = new Estancia(LocalDate.parse(entrada.getText().trim()),
                    LocalDate.parse(salida.getText().trim()));
            int totalPersonas = (Integer) personas.getValue();
            ListaEnlazada<Apartamento> asignados =
                    contexto.getSistema().asignarGrupo(estancia, totalPersonas);

            StringBuilder mensaje = new StringBuilder("Combinación mínima encontrada ("
                    + asignados.tamano() + " apartamento(s)):\n");
            int capacidad = 0;
            for (Apartamento apartamento : asignados) {
                mensaje.append("  · ").append(apartamento.getCodigo()).append(" · ")
                        .append(apartamento.getNombre()).append(" · capacidad ")
                        .append(apartamento.getCapacidadMaxima()).append('\n');
                capacidad += apartamento.getCapacidadMaxima();
            }
            mensaje.append("\nCapacidad total: ").append(capacidad)
                    .append(" para ").append(totalPersonas).append(" personas.");
            JOptionPane.showMessageDialog(this, mensaje.toString(),
                    "Asignación de grupo", JOptionPane.INFORMATION_MESSAGE);
        } catch (DateTimeParseException excepcion) {
            mensajeError("Fecha inválida", "Use el formato aaaa-mm-dd, por ejemplo "
                    + LocalDate.now().plusDays(30) + ".");
        } catch (FechasInvalidasException excepcion) {
            mensajeError("Fechas inválidas", excepcion.getMessage());
        } catch (SinAlojamientoDisponibleException excepcion) {
            mensajeError("Sin alojamiento disponible", excepcion.getMessage());
        }
    }

    private void procesarExterna(boolean todas) {
        try {
            ListaEnlazada<Reserva> creadas;
            if (todas) {
                creadas = contexto.getSistema().procesarCanalExterno();
            } else {
                creadas = new ListaEnlazada<>();
                Reserva creada = contexto.getSistema().getProcesadorCanalExterno().procesarSiguiente();
                if (creada != null) {
                    creadas.agregar(creada);
                }
            }
            contexto.notificar();
            JOptionPane.showMessageDialog(this,
                    "Solicitudes procesadas. Reservas creadas: " + creadas.tamano()
                            + "\nConflictos de canal acumulados: "
                            + contexto.getSistema().getProcesadorCanalExterno().numeroConflictos(),
                    "Canal externo", JOptionPane.INFORMATION_MESSAGE);
        } catch (SgaException excepcion) {
            mensajeError("No se pudo procesar el canal externo", excepcion.getMessage());
        }
    }

    private void mensajeError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Extractores y renderizadores
    // ------------------------------------------------------------------

    private static class ExtractorReserva implements Modelos.ModeloTabla.Extractor<Reserva> {

        @Override
        public Object valor(Reserva reserva, int columna) {
            switch (columna) {
                case 0:
                    return reserva.getCodigo();
                case 1:
                    return reserva.getTitular().getNombre();
                case 2:
                    return reserva.getApartamento().getCodigo();
                case 3:
                    return reserva.getCanal().getDescripcion();
                case 4:
                    return Formato.fecha(reserva.getEstancia().getFechaEntrada());
                case 5:
                    return Formato.fecha(reserva.getEstancia().getFechaSalida());
                case 6:
                    return reserva.getEstancia().numeroNoches();
                case 7:
                    return reserva.numeroPersonas();
                case 8:
                    return reserva.getEstado();
                case 9:
                    return Formato.moneda(reserva.getTotalEstimado());
                case 10:
                    return Formato.moneda(reserva.getAnticipoPagado());
                default:
                    return "";
            }
        }
    }

    private static class RenderizadorEstado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                                                       boolean seleccionado, boolean enfocado,
                                                       int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(
                    tabla, valor, seleccionado, enfocado, fila, columna);
            if (seleccionado || !(valor instanceof EstadoReserva)) {
                return componente;
            }
            EstadoReserva estado = (EstadoReserva) valor;
            setHorizontalAlignment(CENTER);
            if (estado == EstadoReserva.PENDIENTE) {
                setBackground(new Color(0xFFF8E1));
                setForeground(new Color(0x8D6E00));
            } else if (estado == EstadoReserva.CONFIRMADA) {
                setBackground(new Color(0xE3F2FD));
                setForeground(new Color(0x0D47A1));
            } else if (estado == EstadoReserva.EN_CURSO) {
                setBackground(new Color(0xE8F5E9));
                setForeground(new Color(0x1B5E20));
            } else if (estado == EstadoReserva.FINALIZADA) {
                setBackground(new Color(0xECEFF1));
                setForeground(Estilo.TEXTO_SUAVE);
            } else {
                setBackground(new Color(0xFFEBEE));
                setForeground(Estilo.PELIGRO);
            }
            return componente;
        }
    }

    private static class RenderizadorSolicitud extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean seleccionado, boolean enfocado) {
            super.getListCellRendererComponent(lista, valor, indice, seleccionado, enfocado);
            if (valor instanceof SolicitudExterna) {
                SolicitudExterna solicitud = (SolicitudExterna) valor;
                setText("<html><b>" + solicitud.getIdentificador() + "</b> · "
                        + solicitud.getCodigoApartamento() + "<br>"
                        + Formato.fecha(solicitud.getFechaEntrada()) + " → "
                        + Formato.fecha(solicitud.getFechaSalida()) + " · "
                        + solicitud.getNumeroPersonas() + " personas<br>"
                        + solicitud.getEstado().getDescripcion() + "</html>");
            }
            return this;
        }
    }
}
