package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import model.Huesped;

/**
 * Diálogo de acceso: el usuario elige con qué <b>rol</b> entra al sistema
 * (Huésped, Recepcionista o Administrador). Si entra como huésped, además escoge
 * su perfil o se registra.
 */
public class DialogoAcceso extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final JRadioButton[] opciones = new JRadioButton[Rol.values().length];
    private final Modelos.ModeloLista<Huesped> modeloHuespedes = new Modelos.ModeloLista<>();
    private final JComboBox<Huesped> comboHuesped = new JComboBox<>(modeloHuespedes);
    private final JButton registrar = Estilo.boton("Registrarme", Estilo.EXITO);
    private boolean aceptado;

    private DialogoAcceso(Component padre, Contexto contexto) {
        this.contexto = contexto;
        setTitle("Acceso · SGA Aparta-Suites UQ");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(Estilo.relleno(18));

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.add(Estilo.etiquetaTitulo("¿Cómo desea ingresar?"));
        encabezado.add(Estilo.etiquetaSuave("Cada rol ve las pantallas de su trabajo."));
        contenido.add(encabezado, BorderLayout.NORTH);

        JPanel centro = Estilo.tarjeta();
        JPanel lista = new JPanel();
        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        ButtonGroup grupo = new ButtonGroup();
        for (Rol rol : Rol.values()) {
            JRadioButton boton = new JRadioButton("<html><b>" + rol.getDescripcion() + "</b><br>"
                    + "<span style='color:#607D8B'>" + rol.getResumen() + "</span></html>");
            boton.setOpaque(false);
            boton.setFont(Estilo.normal());
            boton.addActionListener(evento -> actualizarHuesped());
            grupo.add(boton);
            lista.add(boton);
            lista.add(javax.swing.Box.createVerticalStrut(8));
            opciones[rol.ordinal()] = boton;
        }
        Rol actual = contexto.getRol();
        opciones[actual.ordinal()].setSelected(true);
        centro.add(lista, BorderLayout.CENTER);

        JPanel perfil = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        perfil.setOpaque(false);
        perfil.add(new JLabel("Huésped:"));
        comboHuesped.setFont(Estilo.pequena());
        perfil.add(comboHuesped);
        registrar.addActionListener(evento -> registrarHuesped());
        perfil.add(registrar);
        centro.add(perfil, BorderLayout.SOUTH);
        contenido.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        JButton entrar = Estilo.boton("Ingresar", Estilo.PRIMARIO);
        entrar.addActionListener(evento -> ingresar());
        botones.add(entrar);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        recargarHuespedes(contexto.getDocumentoHuesped());
        actualizarHuesped();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(padre);
        getRootPane().setDefaultButton(entrar);
    }

    /**
     * Muestra el diálogo y deja la sesión elegida en el contexto.
     *
     * @return {@code true} si el usuario ingresó; {@code false} si cerró el diálogo
     */
    public static boolean mostrar(Component padre, Contexto contexto) {
        DialogoAcceso dialogo = new DialogoAcceso(padre, contexto);
        dialogo.setVisible(true);
        return dialogo.aceptado;
    }

    private Rol rolElegido() {
        for (Rol rol : Rol.values()) {
            if (opciones[rol.ordinal()].isSelected()) {
                return rol;
            }
        }
        return Rol.RECEPCIONISTA;
    }

    private void actualizarHuesped() {
        boolean esHuesped = rolElegido() == Rol.HUESPED;
        comboHuesped.setEnabled(esHuesped);
        registrar.setEnabled(esHuesped);
    }

    private void recargarHuespedes(String documentoASeleccionar) {
        modeloHuespedes.setDatos(contexto.getSistema().todosHuespedes());
        if (documentoASeleccionar != null) {
            Huesped huesped = contexto.getSistema().buscarHuespedOpcional(documentoASeleccionar);
            if (huesped != null) {
                comboHuesped.setSelectedItem(huesped);
            }
        }
    }

    private void registrarHuesped() {
        if (DialogoHuesped.mostrar(this, contexto)) {
            recargarHuespedes(null);
            contexto.notificar();
        }
    }

    private void ingresar() {
        Rol rol = rolElegido();
        String documento = null;
        if (rol == Rol.HUESPED) {
            Huesped huesped = (Huesped) comboHuesped.getSelectedItem();
            if (huesped == null) {
                JOptionPane.showMessageDialog(this,
                        "Seleccione su perfil o regístrese como huésped.",
                        "Falta el huésped", JOptionPane.WARNING_MESSAGE);
                return;
            }
            documento = huesped.getDocumento();
        }
        contexto.iniciarSesion(rol, documento);
        aceptado = true;
        dispose();
    }
}
