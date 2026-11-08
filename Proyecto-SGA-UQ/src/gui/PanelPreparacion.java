package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import datastructures.Grafo;
import datastructures.ListaEnlazada;
import exceptions.SgaException;
import exceptions.TransicionEstadoInvalidaException;
import model.Apartamento;
import model.TareaPreparacion;
import service.GestorEdificio;
import service.GestorPreparacion;
import service.SistemaAlojamiento;

/**
 * Panel de preparación: muestra la cola de prioridad de limpieza y la ruta que
 * debe seguir el personal, calculada con Dijkstra sobre el grafo del edificio.
 *
 * Las tareas con entrada programada para hoy tienen prioridad 1; las demás,
 * prioridad 2. Dentro de cada grupo se ordenan por la hora estimada de llegada del
 * siguiente grupo y por antigüedad.
 */
public class PanelPreparacion extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final Modelos.ModeloLista<TareaPreparacion> modelo = new Modelos.ModeloLista<>();
    private final JList<TareaPreparacion> lista = new JList<>(modelo);
    private final JTextArea ruta = new JTextArea();
    private final JLabel resumen = new JLabel(" ");
    private final JLabel distancia = new JLabel(" ");

    public PanelPreparacion(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                construirCola(), construirRuta());
        split.setResizeWeight(0.40);
        split.setDividerLocation(430);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        resumen.setFont(Estilo.pequena());
        resumen.setForeground(Estilo.TEXTO_SUAVE);
        pie.add(resumen, BorderLayout.WEST);
        add(pie, BorderLayout.SOUTH);

        contexto.registrar(this);
        actualizar();
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Preparación de apartamentos"));
        textos.add(Estilo.etiquetaSuave(
                "Prioridad 1: entrada hoy · Prioridad 2: sin entrada hoy · ruta óptima por pasillos"));
        cabecera.add(textos, BorderLayout.WEST);

        JButton refrescar = Estilo.boton("Actualizar", Estilo.PRIMARIO);
        refrescar.addActionListener(evento -> contexto.notificar());
        cabecera.add(refrescar, BorderLayout.EAST);
        return cabecera;
    }

    private JPanel construirCola() {
        JPanel panel = Estilo.seccion("Cola de prioridad de limpieza");

        lista.setFont(Estilo.normal());
        lista.setFixedCellHeight(54);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RenderizadorTarea());
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        botones.setOpaque(false);

        JButton iniciar = Estilo.boton("Iniciar preparación", Estilo.PRIMARIO);
        iniciar.addActionListener(evento -> iniciar());
        botones.add(iniciar);

        JButton completar = Estilo.boton("Completar", Estilo.EXITO);
        completar.addActionListener(evento -> completar());
        botones.add(completar);

        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel construirRuta() {
        JPanel panel = Estilo.seccion("Ruta de limpieza · Dijkstra");

        ruta.setEditable(false);
        ruta.setFont(Estilo.mono());
        ruta.setBackground(new Color(0xFAFBFC));
        ruta.setBorder(Estilo.relleno(8));
        JScrollPane scroll = new JScrollPane(ruta);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        panel.add(scroll, BorderLayout.CENTER);

        distancia.setFont(Estilo.normal());
        distancia.setForeground(Estilo.PRIMARIO_OSCURO);
        distancia.setBorder(Estilo.relleno(6));
        panel.add(distancia, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------------
    // Datos
    // ------------------------------------------------------------------

    @Override
    public void actualizar() {
        SistemaAlojamiento sistema = contexto.getSistema();
        GestorPreparacion gestor = sistema.getGestorPreparacion();

        String codigoSeleccionado = null;
        TareaPreparacion seleccionada = lista.getSelectedValue();
        if (seleccionada != null) {
            codigoSeleccionado = seleccionada.getApartamento().getCodigo();
        }

        ListaEnlazada<TareaPreparacion> tareas = gestor.tareasPendientes();
        modelo.setDatos(tareas);

        if (codigoSeleccionado != null) {
            for (int i = 0; i < modelo.getSize(); i++) {
                if (modelo.getElementAt(i).getApartamento().getCodigo().equals(codigoSeleccionado)) {
                    lista.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (lista.getSelectedIndex() < 0 && modelo.getSize() > 0) {
            lista.setSelectedIndex(0);
        }

        resumen.setText("Tareas pendientes: " + gestor.numeroTareasPendientes()
                + "   ·   Con entrada hoy: " + gestor.tareasConEntradaHoy().tamano()
                + "   ·   Completadas: " + gestor.historial().tamano()
                + "   ·   Tiempo medio: "
                + String.format(java.util.Locale.ROOT, "%.1f min",
                gestor.tiempoPromedioPreparacionMinutos()));
        actualizarRuta(sistema, gestor);
    }

    private void actualizarRuta(SistemaAlojamiento sistema, GestorPreparacion gestor) {
        ListaEnlazada<Apartamento> rutaLimpieza = gestor.rutaDeLimpieza();
        if (rutaLimpieza.estaVacia()) {
            ruta.setText("No hay apartamentos pendientes de preparación.\n\n"
                    + "Cuando se registre un check-out, el apartamento pasará a\n"
                    + "\"Pendiente de preparación\" y aparecerá aquí con su prioridad.");
            distancia.setText("Distancia total de la ruta: 0 m");
            return;
        }

        StringBuilder texto = new StringBuilder();
        texto.append("Punto de partida: ").append(GestorEdificio.RECEPCION).append("\n\n");
        String actual = GestorEdificio.RECEPCION;
        int paso = 1;
        for (Apartamento apartamento : rutaLimpieza) {
            Grafo.Ruta<String> tramo = sistema.getGestorEdificio()
                    .ruta(actual, apartamento.getCodigo());
            TareaPreparacion tarea = gestor.buscarTarea(apartamento);

            texto.append(paso++).append(". ").append(apartamento.getCodigo())
                    .append(" · ").append(apartamento.getNombre())
                    .append("  [prioridad ").append(tarea.getPrioridad()).append("]\n");
            if (tramo.esAlcanzable()) {
                texto.append("   ").append(tramo.getDistancia()).append(" m: ")
                        .append(camino(tramo)).append('\n');
            } else {
                texto.append("   Inalcanzable desde ").append(actual)
                        .append(": revise los pasillos cerrados.\n");
            }
            texto.append("   ").append(tarea.isEntradaHoy()
                    ? "Entrada hoy a las " + tarea.getHoraEstimadaLlegada()
                    : "Sin entrada programada para hoy").append('\n');
            if (tarea.getInicioPreparacion() != null) {
                texto.append("   Preparación iniciada: ")
                        .append(Formato.fechaHora(tarea.getInicioPreparacion())).append('\n');
            }
            texto.append('\n');
            actual = apartamento.getCodigo();
        }
        texto.append("Distancia total: ").append(gestor.distanciaRutaDeLimpieza()).append(" m");
        ruta.setText(texto.toString());
        ruta.setCaretPosition(0);
        distancia.setText("Distancia total de la ruta: "
                + gestor.distanciaRutaDeLimpieza() + " m  ·  "
                + rutaLimpieza.tamano() + " apartamento(s) por atender");
    }

    private String camino(Grafo.Ruta<String> tramo) {
        StringBuilder texto = new StringBuilder();
        for (String etiqueta : tramo.getEtiquetas()) {
            if (texto.length() > 0) {
                texto.append(" → ");
            }
            texto.append(etiqueta);
        }
        return texto.toString();
    }

    private TareaPreparacion seleccionada() {
        TareaPreparacion tarea = lista.getSelectedValue();
        if (tarea == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione una tarea de la cola de prioridad.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
        }
        return tarea;
    }

    private void iniciar() {
        TareaPreparacion tarea = seleccionada();
        if (tarea == null) {
            return;
        }
        try {
            contexto.getSistema().getGestorPreparacion().iniciar(tarea.getApartamento());
            contexto.notificar();
        } catch (TransicionEstadoInvalidaException excepcion) {
            JOptionPane.showMessageDialog(this,
                    "No se puede iniciar la preparación del apartamento "
                            + tarea.getApartamento().getCodigo() + ":\n" + excepcion.getMessage(),
                    "Preparación no permitida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void completar() {
        TareaPreparacion tarea = seleccionada();
        if (tarea == null) {
            return;
        }
        try {
            long segundos = contexto.getSistema().getGestorPreparacion()
                    .completar(tarea.getApartamento());
            contexto.notificar();
            JOptionPane.showMessageDialog(this,
                    "El apartamento " + tarea.getApartamento().getCodigo()
                            + " quedó Preparado.\nDuración de la limpieza: " + segundos + " segundos.",
                    "Preparación completada", JOptionPane.INFORMATION_MESSAGE);
        } catch (TransicionEstadoInvalidaException excepcion) {
            JOptionPane.showMessageDialog(this,
                    "Primero debe iniciar la preparación del apartamento "
                            + tarea.getApartamento().getCodigo() + ".\n" + excepcion.getMessage(),
                    "Preparación no permitida", JOptionPane.WARNING_MESSAGE);
        } catch (SgaException excepcion) {
            JOptionPane.showMessageDialog(this, excepcion.getMessage(),
                    "Error de preparación", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Renderizador de tareas
    // ------------------------------------------------------------------

    private static class RenderizadorTarea extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean seleccionado, boolean enfocado) {
            super.getListCellRendererComponent(lista, valor, indice, seleccionado, enfocado);
            if (!(valor instanceof TareaPreparacion)) {
                return this;
            }
            TareaPreparacion tarea = (TareaPreparacion) valor;
            String prioridad = tarea.getPrioridad() == 1
                    ? "<font color='#C62828'><b>Prioridad 1 · entrada hoy</b></font>"
                    : "<font color='#1565C0'><b>Prioridad 2 · sin entrada hoy</b></font>";
            String detalle = tarea.isEntradaHoy()
                    ? "Llegada estimada " + tarea.getHoraEstimadaLlegada()
                    : "Sin llegada programada para hoy";
            setText("<html>" + prioridad + " · <b>" + tarea.getApartamento().getCodigo()
                    + "</b> " + tarea.getApartamento().getNombre()
                    + "<br><font size='2'>" + detalle + " · en cola desde "
                    + Formato.fechaHora(tarea.getDesdeCuandoPendiente())
                    + (tarea.getReservaEntrante() == null ? "" : " · entra " + tarea.getReservaEntrante())
                    + "</font></html>");
            setBorder(Estilo.relleno(4));
            return this;
        }
    }
}
