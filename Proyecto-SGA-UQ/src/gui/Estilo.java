package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Paleta, tipografías y componentes reutilizables para dar un aspecto uniforme a
 * todos los paneles de la interfaz.
 */
public final class Estilo {

    public static final Color FONDO = new Color(0xF4F6F8);
    public static final Color TARJETA = Color.WHITE;
    public static final Color PRIMARIO = new Color(0x1565C0);
    public static final Color PRIMARIO_OSCURO = new Color(0x0D47A1);
    public static final Color EXITO = new Color(0x2E7D32);
    public static final Color ADVERTENCIA = new Color(0xF9A825);
    public static final Color PELIGRO = new Color(0xC62828);
    public static final Color TEXTO = new Color(0x263238);
    public static final Color TEXTO_SUAVE = new Color(0x607D8B);
    public static final Color BORDE = new Color(0xDDE3E8);

    private static final Font TITULO = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font SUBTITULO = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font PEQUENA = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font MONO = new Font("Consolas", Font.PLAIN, 12);

    private Estilo() {
        // Clase de utilidades: no se instancia.
    }

    public static Font titulo() {
        return TITULO;
    }

    public static Font subtitulo() {
        return SUBTITULO;
    }

    public static Font normal() {
        return NORMAL;
    }

    public static Font pequena() {
        return PEQUENA;
    }

    public static Font mono() {
        return MONO;
    }

    /** Borde con relleno interior uniforme. */
    public static Border relleno(int espacio) {
        return BorderFactory.createEmptyBorder(espacio, espacio, espacio, espacio);
    }

    /** Tarjeta blanca con borde redondeado simulado y sombra ligera. */
    public static JPanel tarjeta() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        return panel;
    }

    /** Panel de sección con un título en la parte superior. */
    public static JPanel seccion(String titulo) {
        JPanel panel = tarjeta();
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(SUBTITULO);
        etiqueta.setForeground(TEXTO);
        panel.add(etiqueta, BorderLayout.NORTH);
        return panel;
    }

    /** Etiqueta de título grande. */
    public static JLabel etiquetaTitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(TITULO);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    /** Etiqueta secundaria. */
    public static JLabel etiquetaSuave(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(PEQUENA);
        etiqueta.setForeground(TEXTO_SUAVE);
        return etiqueta;
    }

    /** Botón con color de fondo y texto legible. */
    public static JButton boton(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(NORMAL);
        boton.setForeground(Formato.textoContraste(color));
        boton.setBackground(color);
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker()),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return boton;
    }

    /** Espaciador horizontal. */
    public static Component espacio(int ancho) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setPreferredSize(new java.awt.Dimension(ancho, 1));
        return panel;
    }

    /** Panel transparente con disposición indicada. */
    public static JPanel transparente(java.awt.LayoutManager disposicion) {
        JPanel panel = new JPanel(disposicion);
        panel.setOpaque(false);
        return panel;
    }
}
