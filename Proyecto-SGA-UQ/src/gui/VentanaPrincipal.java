package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import service.SistemaAlojamiento;
import util.CargadorDatos;
import util.EscenarioPrueba;

/**
 * Ventana principal del Sistema de Gestión de Alojamiento de Aparta-Suites UQ.
 *
 * Al abrir pide el rol (Huésped, Recepcionista o Administrador) y muestra
 * solo las pestañas de ese rol. El botón Atrás vuelve a la pantalla anterior
 * usando una {datastructures.Pila}. Las pestañas disponibles son:
 *
 *   Mi cuenta: reservas, saldo de folios, favoritos e historial del huésped.
 *   Administración: empleados, apartamentos, huéspedes, temporadas y tarifas.
 *   Tablero:estado de todos los apartamentos con el código de colores.
 *   Reservas: consulta, creación y ciclo de vida de las reservas.
 *   Calendario: ocupación día a día por apartamento.
 *   Huéspedes:ficha del huésped, favoritos e historial.
 *   Preparación: cola de prioridad y ruta de limpieza.
 *   Mapa: grafo interactivo del edificio con ruta más corta.
 *   Estadísticas: ocupación, ingresos, conflictos y tiempos.
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final JLabel etiquetaEstado = new JLabel(" ");
    private final JTabbedPane pestanas = new JTabbedPane();
    private final HistorialNavegacion historial = new HistorialNavegacion();
    private final JButton botonAtras = new JButton("← Atrás");
    private final JLabel etiquetaSesion = new JLabel(" ");
    private boolean navegandoAtras;

    public VentanaPrincipal() {
        this(EscenarioPrueba.construir());
    }

    /**
     * Crea la ventana principal con el sistema indicado como estado inicial.
     *
     * @param sistemaInicial sistema que rellenará los paneles al abrir.
     */
    public VentanaPrincipal(SistemaAlojamiento sistemaInicial) {
        super("SGA · Aparta-Suites UQ · Salento, Quindío");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);

        // El escenario de prueba se carga al arrancar, para que la aplicación no
        // aparezca vacía la primera vez.
        this.contexto = new Contexto(sistemaInicial);

        construirMenu();
        construirContenido();
        actualizarBarraEstado();
        cambiarDeRol();
    }

    // ------------------------------------------------------------------
    // Menú
    // ------------------------------------------------------------------

    private void construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu menuDatos = new JMenu("Datos");
        JMenuItem itemEscenario = new JMenuItem("Cargar escenario de prueba");
        itemEscenario.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                contexto.setSistema(EscenarioPrueba.construir());
                actualizarBarraEstado();
                mostrarInformacion("Se cargó el escenario de prueba del alojamiento.");
            }
        });
        menuDatos.add(itemEscenario);

        JMenuItem itemArchivo = new JMenuItem("Cargar desde archivo…");
        itemArchivo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                cargarDesdeArchivo();
            }
        });
        menuDatos.add(itemArchivo);

        JMenuItem itemVacio = new JMenuItem("Empezar con un sistema vacío");
        itemVacio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                contexto.setSistema(new SistemaAlojamiento());
                actualizarBarraEstado();
                mostrarInformacion("Sistema vacío. Registre apartamentos y huéspedes.");
            }
        });
        menuDatos.add(itemVacio);

        menuDatos.addSeparator();

        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                dispose();
                System.exit(0);
            }
        });
        menuDatos.add(itemSalir);

        barra.add(menuDatos);

        JMenu menuSesion = new JMenu("Sesión");
        JMenuItem itemRol = new JMenuItem("Cambiar de rol…");
        itemRol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                cambiarDeRol();
            }
        });
        menuSesion.add(itemRol);
        barra.add(menuSesion);

        JMenu menuAyuda = new JMenu("Ayuda");
        JMenuItem itemAcerca = new JMenuItem("Acerca de…");
        itemAcerca.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                JOptionPane.showMessageDialog(VentanaPrincipal.this,
                        "Sistema de Gestión de Alojamiento (SGA)\n"
                                + "Aparta-Suites UQ · Salento, Quindío\n\n"
                                + "Proyecto académico de Estructuras de Datos.\n"
                                + "Todas las estructuras de datos del núcleo son propias:\n"
                                + "árboles, listas, pilas, colas, colas de prioridad,\n"
                                + "grafos y conjuntos, sin usar colecciones de java.util.",
                        "Acerca de SGA", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        menuAyuda.add(itemAcerca);
        barra.add(menuAyuda);

        setJMenuBar(barra);
    }

    // ------------------------------------------------------------------
    // Contenido
    // ------------------------------------------------------------------

    private void construirContenido() {
        JPanel raiz = new JPanel(new BorderLayout());

        JPanel barraNavegacion = new JPanel(new BorderLayout());
        barraNavegacion.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botonAtras.setFont(Estilo.normal());
        botonAtras.setFocusPainted(false);
        botonAtras.setEnabled(false);
        botonAtras.setToolTipText("Volver a la pantalla anterior");
        botonAtras.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                volverAtras();
            }
        });
        izquierda.add(botonAtras);
        barraNavegacion.add(izquierda, BorderLayout.WEST);
        etiquetaSesion.setFont(Estilo.pequena());
        etiquetaSesion.setForeground(Estilo.TEXTO_SUAVE);
        barraNavegacion.add(etiquetaSesion, BorderLayout.EAST);
        raiz.add(barraNavegacion, BorderLayout.NORTH);

        pestanas.addChangeListener(evento -> registrarNavegacion());
        raiz.add(pestanas, BorderLayout.CENTER);

        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        barraEstado.add(etiquetaEstado, BorderLayout.WEST);
        raiz.add(barraEstado, BorderLayout.SOUTH);

        setContentPane(raiz);
    }

    /**
     * Pide el rol y reconstruye las pestañas según lo que ese rol puede hacer.
     */
    private void cambiarDeRol() {
        DialogoAcceso.mostrar(this, contexto);
        armarPestanas();
        actualizarBarraEstado();
    }

    private void armarPestanas() {
        navegandoAtras = true;
        pestanas.removeAll();
        contexto.limpiarPaneles();
        contexto.registrar(this::actualizarBarraEstado);
        historial.reiniciar();

        switch (contexto.getRol()) {
            case HUESPED:
                pestanas.addTab("Mi cuenta", new PanelUsuario(contexto));
                pestanas.addTab("Calendario", new PanelCalendario(contexto));
                break;
            case ADMINISTRADOR:
                pestanas.addTab("Tablero", new PanelTablero(contexto));
                pestanas.addTab("Calendario", new PanelCalendario(contexto));
                pestanas.addTab("Mapa", new PanelMapa(contexto));
                pestanas.addTab("Administración", new PanelAdministracion(contexto));
                pestanas.addTab("Estadísticas", new PanelEstadisticas(contexto));
                break;
            default:
                pestanas.addTab("Tablero", new PanelTablero(contexto));
                pestanas.addTab("Reservas", new PanelReservas(contexto));
                pestanas.addTab("Calendario", new PanelCalendario(contexto));
                pestanas.addTab("Huéspedes", new PanelHuespedes(contexto));
                pestanas.addTab("Preparación", new PanelPreparacion(contexto));
                pestanas.addTab("Mapa", new PanelMapa(contexto));
                break;
        }
        navegandoAtras = false;

        pestanas.setSelectedIndex(0);
        historial.visitar(pestanas.getTitleAt(0));
        botonAtras.setEnabled(false);
        actualizarEtiquetaSesion();
    }

    private void actualizarEtiquetaSesion() {
        String texto = "Rol: " + contexto.getRol().getDescripcion();
        if (contexto.getRol() == Rol.HUESPED && contexto.getHuespedActual() != null) {
            texto += " · " + contexto.getHuespedActual().getNombre();
        }
        etiquetaSesion.setText(texto);
    }

    /** Apila la pantalla anterior cada vez que el usuario abre otra pestaña. */
    private void registrarNavegacion() {
        if (navegandoAtras || pestanas.getSelectedIndex() < 0) {
            return;
        }
        historial.visitar(pestanas.getTitleAt(pestanas.getSelectedIndex()));
        botonAtras.setEnabled(historial.puedeVolver());
    }

    /** Vuelve a la pantalla anterior desapilando el historial de navegación. */
    private void volverAtras() {
        String pantalla = historial.atras();
        if (pantalla != null) {
            navegandoAtras = true;
            for (int i = 0; i < pestanas.getTabCount(); i++) {
                if (pestanas.getTitleAt(i).equals(pantalla)) {
                    pestanas.setSelectedIndex(i);
                    break;
                }
            }
            navegandoAtras = false;
        }
        botonAtras.setEnabled(historial.puedeVolver());
    }

    /** Actualiza el texto de la barra de estado con los totales del sistema. */
    private void actualizarBarraEstado() {
        SistemaAlojamiento sistema = contexto.getSistema();
        etiquetaEstado.setText(String.format(
                "Apartamentos: %d   ·   Huéspedes: %d   ·   Reservas: %d   ·   "
                        + "Pendientes de preparación: %d",
                sistema.todosApartamentos().tamano(),
                sistema.todosHuespedes().tamano(),
                sistema.todasReservas().tamano(),
                sistema.getGestorPreparacion().numeroTareasPendientes())
                + (sistema.recepcionistaDeTurno() == null ? ""
                : "   ·   Turno de recepción: " + sistema.recepcionistaDeTurno().getNombre()));
    }

    /**
     * Abre un diálogo para cargar un escenario desde un archivo de texto.
     */
    private void cargarDesdeArchivo() {
        JFileChooser selector = new JFileChooser(new File("."));
        selector.setDialogTitle("Seleccione el archivo de datos del alojamiento");
        int opcion = selector.showOpenDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        try {
            SistemaAlojamiento sistema = CargadorDatos.cargar(archivo);
            contexto.setSistema(sistema);
            actualizarBarraEstado();
            mostrarInformacion("Datos cargados desde:\n" + archivo.getAbsolutePath());
        } catch (RuntimeException excepcion) {
            JOptionPane.showMessageDialog(this,
                    "No fue posible cargar el archivo:\n" + excepcion.getMessage(),
                    "Error de carga", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarInformacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "SGA",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Punto de entrada alternativo (útil para pruebas).
     */
    public static void abrir() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VentanaPrincipal().setVisible(true);
            }
        });
    }
}
