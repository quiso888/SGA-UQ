package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import datastructures.Grafo;
import datastructures.ListaEnlazada;
import model.Apartamento;
import model.EstadoApartamento;
import model.Temporada;
import service.GestorEdificio;
import service.SistemaAlojamiento;

/**
 * Panel del mapa interactivo del edificio
 *
 * Dibuja el grafo de pasillos y permite:
 *  arrastrar los nodos para reubicarlos,
 *  calcular y resaltar la ruta más corta (Dijkstra) desde la recepción hasta un apartamento
 *  resaltar la ruta completa de limpieza cuando hay apartamentos pendientes.
 */
public class PanelMapa extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final LienzoMapa lienzo = new LienzoMapa();
    private final Modelos.ModeloLista<Apartamento> modeloApartamentos = new Modelos.ModeloLista<>();
    private final JComboBox<Apartamento> comboApartamentos = new JComboBox<>(modeloApartamentos);
    private final JLabel info = new JLabel(" ");
    private final JLabel estadisticas = new JLabel(" ");

    private ListaEnlazada<String> caminoResaltado = new ListaEnlazada<>();
    private String descripcionRuta = "";

    public PanelMapa(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 10));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);
        add(lienzo, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        info.setFont(Estilo.pequena());
        info.setForeground(Estilo.TEXTO_SUAVE);
        pie.add(info, BorderLayout.WEST);
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
        textos.add(Estilo.etiquetaTitulo("Mapa interactivo del edificio"));
        textos.add(Estilo.etiquetaSuave(
                "Grafo de pasillos · arrastre los nodos · ruta más corta con Dijkstra"));
        cabecera.add(textos, BorderLayout.WEST);

        estadisticas.setFont(Estilo.pequena());
        estadisticas.setForeground(Estilo.TEXTO_SUAVE);
        cabecera.add(estadisticas, BorderLayout.EAST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controles.setOpaque(false);
        controles.add(new JLabel("Apartamento destino:"));
        comboApartamentos.setFont(Estilo.pequena());
        controles.add(comboApartamentos);

        JButton rutaCorta = Estilo.boton("Ruta más corta", Estilo.PRIMARIO);
        rutaCorta.addActionListener(evento -> calcularRutaApartamento());
        controles.add(rutaCorta);

        JButton rutaLimpieza = Estilo.boton("Ruta de limpieza", Estilo.ADVERTENCIA);
        rutaLimpieza.addActionListener(evento -> calcularRutaLimpieza());
        controles.add(rutaLimpieza);

        JButton quitar = Estilo.boton("Quitar ruta", Estilo.TEXTO_SUAVE);
        quitar.addActionListener(evento -> {
            caminoResaltado = new ListaEnlazada<>();
            descripcionRuta = "";
            lienzo.repaint();
            info.setText("Ruta retirada. Seleccione un destino para calcularla de nuevo.");
        });
        controles.add(quitar);

        JButton analisis = Estilo.boton("Analizar bloques (BFS)", Estilo.PRIMARIO_OSCURO);
        analisis.addActionListener(evento -> mostrarAnalisis());
        controles.add(analisis);

        JButton pasillos = Estilo.boton("Cerrar / abrir pasillo…", new Color(0x6A1B9A));
        pasillos.addActionListener(evento -> administrarPasillo());
        controles.add(pasillos);

        cabecera.add(controles, BorderLayout.SOUTH);
        return cabecera;
    }

    /**
     * Análisis del edificio con recorridos BFS/DFS: agrupación por bloque,
     * componentes conexas y apartamentos que quedaron inalcanzables.
     */
    private void mostrarAnalisis() {
        SistemaAlojamiento sistema = contexto.getSistema();
        StringBuilder texto = new StringBuilder();

        texto.append("AGRUPACIÓN POR BLOQUE (BFS)\n");
        for (GestorEdificio.Bloque bloque : sistema.getGestorEdificio().apartamentosPorBloque()) {
            texto.append("  · Bloque ").append(bloque.getNombre()).append(": ");
            int i = 0;
            for (Apartamento apartamento : bloque.getApartamentos()) {
                texto.append(i++ == 0 ? "" : ", ").append(apartamento.getCodigo());
            }
            texto.append("   (").append(bloque.cantidad()).append(" apartamentos)\n");
        }

        ListaEnlazada<Apartamento> inalcanzables =
                sistema.getGestorEdificio().apartamentosInalcanzables();
        texto.append("\nAPARTAMENTOS INALCANZABLES DESDE LA RECEPCIÓN\n  ");
        if (inalcanzables.estaVacia()) {
            texto.append("Ninguno: todos los apartamentos están conectados.\n");
        } else {
            for (Apartamento apartamento : inalcanzables) {
                texto.append(apartamento.getCodigo()).append(' ');
            }
            texto.append("\n  Cierre un pasillo para simular una zona aislada.\n");
        }

        texto.append("\nCOMPONENTES CONEXAS (DFS)\n  ")
                .append(sistema.getGestorEdificio().componentesConexas().tamano())
                .append(" componente(s) en el mapa.\n");

        JTextArea area = new JTextArea(texto.toString(), 16, 52);
        area.setEditable(false);
        area.setFont(Estilo.mono());
        area.setBackground(new Color(0xFAFBFC));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        JOptionPane.showMessageDialog(this, scroll, "Análisis del edificio",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Permite cerrar o reabrir un pasillo del mapa para probar la conectividad y la
     * detección de apartamentos inalcanzables.
     */
    private void administrarPasillo() {
        Grafo<String> grafo = contexto.getSistema().getGestorEdificio().getGrafo();
        Modelos.ModeloLista<Grafo.Nodo<String>> modeloNodos = new Modelos.ModeloLista<>();
        ListaEnlazada<Grafo.Nodo<String>> nodos = new ListaEnlazada<>();
        for (Grafo.Nodo<String> nodo : grafo) {
            nodos.agregar(nodo);
        }
        modeloNodos.setDatos(nodos);
        if (modeloNodos.getSize() < 2) {
            JOptionPane.showMessageDialog(this, "El mapa necesita al menos dos nodos.",
                    "Mapa vacío", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<Grafo.Nodo<String>> origen = new JComboBox<>(modeloNodos);
        JComboBox<Grafo.Nodo<String>> destino = new JComboBox<>(modeloNodos);
        destino.setSelectedIndex(1);
        JComboBox<String> accion = new JComboBox<>(new String[] {
                "Cerrar pasillo", "Abrir pasillo"
        });

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel("Nodo de origen:"));
        formulario.add(origen);
        formulario.add(new JLabel("Nodo de destino:"));
        formulario.add(destino);
        formulario.add(new JLabel("Acción:"));
        formulario.add(accion);
        formulario.add(new JLabel("Pasillos cerrados:"));
        formulario.add(new JLabel(String.valueOf(contarPasillosCerrados(grafo))));

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                "Administrar pasillo del edificio", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        Grafo.Nodo<String> nodoOrigen = (Grafo.Nodo<String>) origen.getSelectedItem();
        Grafo.Nodo<String> nodoDestino = (Grafo.Nodo<String>) destino.getSelectedItem();
        if (nodoOrigen == null || nodoDestino == null
                || nodoOrigen.getEtiqueta().equals(nodoDestino.getEtiqueta())) {
            JOptionPane.showMessageDialog(this, "Seleccione dos nodos diferentes.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String etiquetaOrigen = nodoOrigen.getEtiqueta();
        String etiquetaDestino = nodoDestino.getEtiqueta();
        boolean exito;
        if (accion.getSelectedIndex() == 0) {
            exito = contexto.getSistema().getGestorEdificio()
                    .cerrarPasillo(etiquetaOrigen, etiquetaDestino);
        } else {
            exito = contexto.getSistema().getGestorEdificio()
                    .abrirPasillo(etiquetaOrigen, etiquetaDestino);
        }
        if (!exito) {
            JOptionPane.showMessageDialog(this,
                    "No existe un pasillo entre " + etiquetaOrigen + " y "
                            + etiquetaDestino + ".",
                    "Sin pasillo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        caminoResaltado = new ListaEnlazada<>();
        descripcionRuta = "";
        actualizar();
        lienzo.repaint();
        info.setText((accion.getSelectedIndex() == 0 ? "Pasillo cerrado: " : "Pasillo abierto: ")
                + etiquetaOrigen + " ↔ " + etiquetaDestino
                + " · apartamentos inalcanzables: "
                + contexto.getSistema().getGestorEdificio().apartamentosInalcanzables().tamano());
    }

    private int contarPasillosCerrados(Grafo<String> grafo) {
        int cerrados = 0;
        for (Grafo.Nodo<String> nodo : grafo) {
            for (Grafo.Arista<String> arista : nodo.getAristas()) {
                if (!arista.isActiva() && arista.getOrigen() == nodo) {
                    cerrados++;
                }
            }
        }
        return cerrados;
    }

    private JPanel construirLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        leyenda.setOpaque(false);
        leyenda.add(item("Preparado", new Color(EstadoApartamento.PREPARADO.getColorRgb())));
        leyenda.add(item("Ocupado", new Color(EstadoApartamento.OCUPADO.getColorRgb())));
        leyenda.add(item("Por preparar", new Color(EstadoApartamento.PENDIENTE_PREPARACION.getColorRgb())));
        leyenda.add(item("Fuera de servicio", new Color(EstadoApartamento.FUERA_DE_SERVICIO.getColorRgb())));
        leyenda.add(item("Ruta", new Color(0xF57C00)));
        return leyenda;
    }

    private JPanel item(String texto, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel muestra = new JLabel("   ");
        muestra.setOpaque(true);
        muestra.setBackground(color);
        muestra.setBorder(javax.swing.BorderFactory.createLineBorder(color.darker()));
        item.add(muestra);
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Estilo.pequena());
        etiqueta.setForeground(Estilo.TEXTO_SUAVE);
        item.add(etiqueta);
        return item;
    }

    @Override
    public void actualizar() {
        SistemaAlojamiento sistema = contexto.getSistema();
        Apartamento seleccionado = (Apartamento) comboApartamentos.getSelectedItem();
        String codigo = seleccionado == null ? null : seleccionado.getCodigo();

        modeloApartamentos.setDatos(sistema.getGestorEdificio().getApartamentos());
        if (codigo != null) {
            for (int i = 0; i < modeloApartamentos.getSize(); i++) {
                if (modeloApartamentos.getElementAt(i).getCodigo().equals(codigo)) {
                    comboApartamentos.setSelectedIndex(i);
                    break;
                }
            }
        }
        estadisticas.setText("Nodos: " + sistema.getGestorEdificio().numeroNodos()
                + "   ·   Pasillos: " + sistema.getGestorEdificio().numeroPasillos()
                + "   ·   Ciclos: " + (sistema.getGestorEdificio().tieneCiclo() ? "sí" : "no"));
        lienzo.repaint();
        if (info.getText().trim().isEmpty()) {
            info.setText("Seleccione un apartamento y calcule la ruta, o arrastre los nodos del mapa.");
        }
    }

    private void calcularRutaApartamento() {
        Apartamento apartamento = (Apartamento) comboApartamentos.getSelectedItem();
        if (apartamento == null) {
            info.setText("No hay apartamentos registrados en el mapa.");
            return;
        }
        Grafo.Ruta<String> ruta = contexto.getSistema().getGestorEdificio()
                .rutaRecepcionApartamento(apartamento.getCodigo());
        if (!ruta.esAlcanzable()) {
            caminoResaltado = new ListaEnlazada<>();
            descripcionRuta = "";
            info.setText("No hay camino desde la recepción hasta " + apartamento.getCodigo()
                    + ": revise los pasillos cerrados.");
        } else {
            caminoResaltado = ruta.getEtiquetas();
            descripcionRuta = "Ruta a " + apartamento.getCodigo();
            info.setText("Ruta más corta hasta " + apartamento.getCodigo() + " · "
                    + ruta.getDistancia() + " m · " + caminoComoTexto(caminoResaltado));
        }
        lienzo.repaint();
    }

    private void calcularRutaLimpieza() {
        SistemaAlojamiento sistema = contexto.getSistema();
        ListaEnlazada<Apartamento> ruta = sistema.rutaDeLimpieza();
        if (ruta.estaVacia()) {
            info.setText("No hay apartamentos pendientes de preparación: la ruta está vacía.");
            caminoResaltado = new ListaEnlazada<>();
            descripcionRuta = "";
            lienzo.repaint();
            return;
        }

        ListaEnlazada<String> etiquetas = new ListaEnlazada<>();
        String actual = GestorEdificio.RECEPCION;
        int total = 0;
        for (Apartamento apartamento : ruta) {
            Grafo.Ruta<String> tramo = sistema.getGestorEdificio().ruta(actual, apartamento.getCodigo());
            if (tramo.esAlcanzable()) {
                total += tramo.getDistancia();
                for (String etiqueta : tramo.getEtiquetas()) {
                    if (etiquetas.estaVacia() || !etiquetas.ultimo().equals(etiqueta)) {
                        etiquetas.agregar(etiqueta);
                    }
                }
            }
            actual = apartamento.getCodigo();
        }
        caminoResaltado = etiquetas;
        descripcionRuta = "Ruta de limpieza";
        info.setText("Ruta de limpieza · " + ruta.tamano() + " apartamento(s) · "
                + total + " m recorridos · " + rutaComoTexto(ruta));
        lienzo.repaint();
    }

    private String rutaComoTexto(ListaEnlazada<Apartamento> ruta) {
        StringBuilder texto = new StringBuilder();
        for (Apartamento apartamento : ruta) {
            if (texto.length() > 0) {
                texto.append(" → ");
            }
            texto.append(apartamento.getCodigo());
        }
        return texto.toString();
    }

    private String caminoComoTexto(ListaEnlazada<String> etiquetas) {
        StringBuilder texto = new StringBuilder();
        for (String etiqueta : etiquetas) {
            if (texto.length() > 0) {
                texto.append(" → ");
            }
            texto.append(etiqueta);
        }
        return texto.toString();
    }

    private void informarNodo(Grafo.Nodo<String> nodo) {
        if (nodo == null) {
            return;
        }
        String etiqueta = nodo.getEtiqueta();
        if (etiqueta == null) {
            return;
        }
        for (Apartamento apartamento : contexto.getSistema().todosApartamentos()) {
            if (apartamento.getCodigo().equals(etiqueta)) {
                info.setText(apartamento.getCodigo() + " · " + apartamento.getNombre()
                        + " · " + apartamento.getEstado().getDescripcion()
                        + " · capacidad " + apartamento.getCapacidadMaxima()
                        + " · " + Formato.moneda(apartamento.precioPorPersona(Temporada.BAJA))
                        + " por persona/noche en temporada baja");
                return;
            }
        }
        info.setText("Instalación del edificio: " + etiqueta
                + " · posición (" + Math.round(nodo.getX()) + ", " + Math.round(nodo.getY()) + ")");
    }

    // ------------------------------------------------------------------
    // Lienzo del mapa
    // ------------------------------------------------------------------

    private class LienzoMapa extends JPanel {

        private static final long serialVersionUID = 1L;

        private static final double RADIO_APARTAMENTO = 23;
        private static final double RADIO_INSTALACION = 17;
        private static final double MARGEN = 70;

        private double escala = 1;
        private double desplazamientoX;
        private double desplazamientoY;

        private Grafo.Nodo<String> nodoArrastrado;
        private Grafo.Nodo<String> nodoPresionado;
        private Point puntoPresion;
        private Consumer<Grafo.Nodo<String>> alSeleccionar;

        LienzoMapa() {
            setBackground(new Color(0xFBFCFD));
            setPreferredSize(new Dimension(900, 560));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent evento) {
                    nodoPresionado = nodoEn(evento.getX(), evento.getY());
                    nodoArrastrado = nodoPresionado;
                    puntoPresion = evento.getPoint();
                    if (nodoPresionado != null && alSeleccionar != null) {
                        alSeleccionar.accept(nodoPresionado);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent evento) {
                    nodoArrastrado = null;
                    nodoPresionado = null;
                    puntoPresion = null;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent evento) {
                    if (nodoArrastrado == null) {
                        return;
                    }
                    int dx = Math.abs(evento.getX() - puntoPresion.x);
                    int dy = Math.abs(evento.getY() - puntoPresion.y);
                    if (dx < 2 && dy < 2) {
                        return;
                    }
                    double x = (evento.getX() - desplazamientoX) / escala;
                    double y = (evento.getY() - desplazamientoY) / escala;
                    contexto.getSistema().getGestorEdificio()
                            .actualizarPosicion(nodoArrastrado.getEtiqueta(), x, y);
                    repaint();
                }
            });
        }

        void setAlSeleccionar(Consumer<Grafo.Nodo<String>> alSeleccionar) {
            this.alSeleccionar = alSeleccionar;
        }

        private Grafo.Nodo<String> nodoEn(int x, int y) {
            Grafo<String> grafo = contexto.getSistema().getGestorEdificio().getGrafo();
            for (Grafo.Nodo<String> nodo : grafo) {
                double nx = aPantallaX(nodo.getX());
                double ny = aPantallaY(nodo.getY());
                double radio = radioDe(nodo);
                double distancia = Math.hypot(x - nx, y - ny);
                if (distancia <= radio + 4) {
                    return nodo;
                }
            }
            return null;
        }

        private double aPantallaX(double x) {
            return x * escala + desplazamientoX;
        }

        private double aPantallaY(double y) {
            return y * escala + desplazamientoY;
        }

        private double radioDe(Grafo.Nodo<String> nodo) {
            return esInstalacion(nodo.getEtiqueta()) ? RADIO_INSTALACION : RADIO_APARTAMENTO;
        }

        private boolean esInstalacion(String etiqueta) {
            return etiqueta == null
                    || etiqueta.equals(GestorEdificio.RECEPCION)
                    || etiqueta.startsWith(GestorEdificio.PREFIJO_INSTALACION);
        }

        @Override
        protected void paintComponent(Graphics grafico) {
            super.paintComponent(grafico);
            Graphics2D g = (Graphics2D) grafico.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Grafo<String> red = contexto.getSistema().getGestorEdificio().getGrafo();
            if (red.estaVacio() || getWidth() <= 0 || getHeight() <= 0) {
                g.setColor(Estilo.TEXTO_SUAVE);
                g.setFont(Estilo.normal());
                g.drawString("El mapa del edificio todavía no tiene nodos.", 24, 32);
                g.dispose();
                return;
            }

            calcularEscala(red);

            // Pasillos.
            for (Grafo.Arista<String> arista : red.getAristas()) {
                Grafo.Nodo<String> origen = arista.getOrigen();
                Grafo.Nodo<String> destino = arista.getDestino();
                double x1 = aPantallaX(origen.getX());
                double y1 = aPantallaY(origen.getY());
                double x2 = aPantallaX(destino.getX());
                double y2 = aPantallaY(destino.getY());

                if (arista.isActiva()) {
                    g.setColor(new Color(0xCFD8DC));
                    g.setStroke(new BasicStroke(2.2f));
                } else {
                    g.setColor(new Color(0xE57373));
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_BEVEL, 0, new float[] {6f, 4f}, 0));
                }
                g.draw(new Line2D.Double(x1, y1, x2, y2));

                double mx = (x1 + x2) / 2;
                double my = (y1 + y2) / 2;
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.setColor(Estilo.TEXTO_SUAVE);
                String peso = arista.getPeso() + " m";
                g.drawString(peso, (float) mx + 3, (float) my - 3);
            }

            // Ruta resaltada (Dijkstra).
            dibujarRuta(g, red);

            // Nodos.
            for (Grafo.Nodo<String> nodo : red) {
                dibujarNodo(g, red, nodo);
            }

            // Rótulo de la ruta.
            if (descripcionRuta != null && !descripcionRuta.isEmpty()) {
                g.setFont(Estilo.subtitulo());
                g.setColor(new Color(0xE65100));
                g.drawString(descripcionRuta, 18, 26);
            }
            g.dispose();
        }

        private void calcularEscala(Grafo<String> red) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (Grafo.Nodo<String> nodo : red) {
                minX = Math.min(minX, nodo.getX());
                minY = Math.min(minY, nodo.getY());
                maxX = Math.max(maxX, nodo.getX());
                maxY = Math.max(maxY, nodo.getY());
            }
            double rangoX = Math.max(maxX - minX, 120);
            double rangoY = Math.max(maxY - minY, 120);
            double disponibleAncho = Math.max(getWidth() - 2 * MARGEN, 100);
            double disponibleAlto = Math.max(getHeight() - 2 * MARGEN, 100);
            escala = Math.min(disponibleAncho / rangoX, disponibleAlto / rangoY);
            desplazamientoX = (getWidth() - rangoX * escala) / 2 - minX * escala;
            desplazamientoY = (getHeight() - rangoY * escala) / 2 - minY * escala;
        }

        private void dibujarRuta(Graphics2D g, Grafo<String> red) {
            if (caminoResaltado == null || caminoResaltado.tamano() < 2) {
                return;
            }
            g.setColor(new Color(0xF57C00, true));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            String anterior = null;
            for (String etiqueta : caminoResaltado) {
                if (anterior != null) {
                    Grafo.Nodo<String> a = red.buscarNodo(anterior);
                    Grafo.Nodo<String> b = red.buscarNodo(etiqueta);
                    if (a != null && b != null) {
                        g.draw(new Line2D.Double(aPantallaX(a.getX()), aPantallaY(a.getY()),
                                aPantallaX(b.getX()), aPantallaY(b.getY())));
                    }
                }
                anterior = etiqueta;
            }
        }

        private void dibujarNodo(Graphics2D g, Grafo<String> red, Grafo.Nodo<String> nodo) {
            double x = aPantallaX(nodo.getX());
            double y = aPantallaY(nodo.getY());
            double radio = radioDe(nodo);

            boolean enRuta = caminoResaltado != null && caminoResaltado.contiene(nodo.getEtiqueta());
            Color relleno = colorDeNodo(nodo.getEtiqueta());

            if (enRuta) {
                g.setColor(new Color(0xFFE0B2));
                g.fill(new Ellipse2D.Double(x - radio - 6, y - radio - 6,
                        (radio + 6) * 2, (radio + 6) * 2));
            }

            g.setColor(relleno);
            g.fill(new Ellipse2D.Double(x - radio, y - radio, radio * 2, radio * 2));
            g.setColor(enRuta ? new Color(0xE65100) : relleno.darker());
            g.setStroke(new BasicStroke(enRuta ? 2.6f : 1.4f));
            g.draw(new Ellipse2D.Double(x - radio, y - radio, radio * 2, radio * 2));

            String corto = etiquetaCorta(nodo.getEtiqueta());
            g.setFont(new Font("Segoe UI", Font.BOLD, esInstalacion(nodo.getEtiqueta()) ? 10 : 11));
            g.setColor(Formato.textoContraste(relleno));
            int ancho = g.getFontMetrics().stringWidth(corto);
            g.drawString(corto, (float) (x - ancho / 2.0), (float) (y + 4));

            if (!esInstalacion(nodo.getEtiqueta())) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.setColor(Estilo.TEXTO_SUAVE);
                String nombre = nombreDeApartamento(nodo.getEtiqueta());
                int anchoNombre = g.getFontMetrics().stringWidth(nombre);
                g.drawString(nombre, (float) (x - anchoNombre / 2.0), (float) (y + radio + 14));
            }
        }

        private Color colorDeNodo(String etiqueta) {
            if (esInstalacion(etiqueta)) {
                return new Color(0x78909C);
            }
            for (Apartamento apartamento : contexto.getSistema().todosApartamentos()) {
                if (apartamento.getCodigo().equals(etiqueta)) {
                    return new Color(apartamento.getEstado().getColorRgb());
                }
            }
            return new Color(0x90A4AE);
        }

        private String etiquetaCorta(String etiqueta) {
            if (etiqueta == null) {
                return "";
            }
            if (etiqueta.equals(GestorEdificio.RECEPCION)) {
                return "REC";
            }
            if (etiqueta.startsWith("SERV-ESC-")) {
                return "ESC " + etiqueta.substring("SERV-ESC-".length());
            }
            if (etiqueta.startsWith(GestorEdificio.PREFIJO_INSTALACION)) {
                return etiqueta.substring(GestorEdificio.PREFIJO_INSTALACION.length());
            }
            return etiqueta;
        }

        private String nombreDeApartamento(String codigo) {
            for (Apartamento apartamento : contexto.getSistema().todosApartamentos()) {
                if (apartamento.getCodigo().equals(codigo)) {
                    return apartamento.getNombre();
                }
            }
            return "";
        }
    }

    {
        lienzo.setAlSeleccionar(this::informarNodo);
    }
}
