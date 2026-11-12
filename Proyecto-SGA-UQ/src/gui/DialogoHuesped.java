package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import model.Huesped;
import service.SistemaAlojamiento;

/**
 * Diálogo para registrar un huésped nuevo en el catálogo.
 */
public class DialogoHuesped extends JDialog {

    private static final long serialVersionUID = 1L;

    private final SistemaAlojamiento sistema;
    private final JTextField documento = new JTextField();
    private final JTextField nombre = new JTextField();
    private final JTextField nacimiento = new JTextField();
    private final JTextField telefono = new JTextField();
    private final JTextField correo = new JTextField();

    private boolean creado;

    private DialogoHuesped(Component padre, Contexto contexto) {
        this.sistema = contexto.getSistema();
        setTitle("Nuevo huésped · SGA");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(Estilo.relleno(16));

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new javax.swing.BoxLayout(encabezado, javax.swing.BoxLayout.Y_AXIS));
        encabezado.add(Estilo.etiquetaTitulo("Nuevo huésped"));
        encabezado.add(Estilo.etiquetaSuave(
                "El documento identifica al huésped en el árbol binario de búsqueda."));
        contenido.add(encabezado, BorderLayout.NORTH);

        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridLayout(0, 2, 8, 8));
        agregar(formulario, "Documento:", documento);
        agregar(formulario, "Nombre completo:", nombre);
        agregar(formulario, "Nacimiento (aaaa-mm-dd):", nacimiento);
        agregar(formulario, "Teléfono:", telefono);
        agregar(formulario, "Correo:", correo);
        contenido.add(formulario, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton cancelar = Estilo.boton("Cancelar", Estilo.TEXTO_SUAVE);
        cancelar.addActionListener(evento -> dispose());
        botones.add(cancelar);
        JButton guardar = Estilo.boton("Guardar huésped", Estilo.EXITO);
        guardar.addActionListener(evento -> guardar());
        botones.add(guardar);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        setSize(480, 360);
        setLocationRelativeTo(padre);
    }

    /**
     * Abre el diálogo y devuelve {@code true} si se registró un huésped.
     */
    public static boolean mostrar(Component padre, Contexto contexto) {
        DialogoHuesped dialogo = new DialogoHuesped(padre, contexto);
        dialogo.setVisible(true);
        return dialogo.creado;
    }

    private void agregar(JPanel panel, String etiqueta, JTextField campo) {
        JLabel titulo = new JLabel(etiqueta);
        titulo.setFont(Estilo.normal());
        panel.add(titulo);
        campo.setFont(Estilo.normal());
        panel.add(campo);
    }

    private void guardar() {
        String documentoTexto = documento.getText() == null ? "" : documento.getText().trim();
        if (documentoTexto.isEmpty()) {
            mensajeError("Documento obligatorio", "Escriba el documento del huésped.");
            return;
        }
        if (sistema.buscarHuespedOpcional(documentoTexto) != null) {
            mensajeError("Huésped duplicado",
                    "Ya existe un huésped con el documento " + documentoTexto + ".");
            return;
        }
        try {
            LocalDate fechaNacimiento = nacimiento.getText() == null
                    || nacimiento.getText().trim().isEmpty()
                    ? null : LocalDate.parse(nacimiento.getText().trim());
            Huesped huesped = new Huesped(documentoTexto, nombre.getText(),
                    fechaNacimiento, textoOpcional(telefono), textoOpcional(correo));
            sistema.registrarHuesped(huesped);
            creado = true;
            JOptionPane.showMessageDialog(this,
                    "Huésped registrado:\n" + huesped,
                    "Huésped creado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (DateTimeParseException excepcion) {
            mensajeError("Fecha inválida",
                    "Use el formato aaaa-mm-dd para la fecha de nacimiento.");
        } catch (IllegalArgumentException excepcion) {
            mensajeError("Datos inválidos", excepcion.getMessage());
        }
    }

    private String textoOpcional(JTextField campo) {
        String texto = campo.getText();
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }

    private void mensajeError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }
}
