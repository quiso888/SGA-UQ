package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import datastructures.ListaEnlazada;

/**
 * Gráfico de barras horizontales dibujado con {Graphics2D}.
 *
 * Se usa en el panel de estadísticas para comparar conteos e ingresos sin
 * depender de bibliotecas externas. Cada barra tiene su etiqueta, su valor y su
 * color; el ancho se escala con respecto al mayor valor de la serie.
 */
public class GraficoBarras extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int RELLENO = 8;
    private static final int ALTO_MINIMO_FILA = 26;
    private static final int ALTO_MAXIMO_FILA = 40;

    /** Formato aplicado al valor numérico de cada barra. */
    public interface Formateador {
        String formatear(double valor);
    }

    /** Una barra del gráfico. */
    public static final class Barra {

        private final String etiqueta;
        private final double valor;
        private final Color color;

        public Barra(String etiqueta, double valor, Color color) {
            this.etiqueta = etiqueta;
            this.valor = valor;
            this.color = color;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public double getValor() {
            return valor;
        }

        public Color getColor() {
            return color;
        }
    }

    private final ListaEnlazada<Barra> barras = new ListaEnlazada<>();
    private Formateador formateador;
    private String mensajeVacio = "Sin datos para mostrar";

    public GraficoBarras() {
        this(null);
    }

    public GraficoBarras(Formateador formateador) {
        this.formateador = formateador;
        setOpaque(false);
    }

    public void setFormateador(Formateador formateador) {
        this.formateador = formateador;
    }

    public void setMensajeVacio(String mensajeVacio) {
        this.mensajeVacio = mensajeVacio;
    }

    /** Reemplaza la serie de barras y solicita volver a dibujar. */
    public void setBarras(ListaEnlazada<Barra> nuevas) {
        barras.limpiar();
        if (nuevas != null) {
            for (Barra barra : nuevas) {
                barras.agregar(barra);
            }
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int filas = Math.max(barras.tamano(), 1);
        return new Dimension(340, RELLENO * 2 + filas * 32);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(180, 80);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();

            if (barras.estaVacia()) {
                g.setColor(Estilo.TEXTO_SUAVE);
                g.setFont(Estilo.pequena());
                FontMetrics fm = g.getFontMetrics();
                int x = Math.max(RELLENO, (ancho - fm.stringWidth(mensajeVacio)) / 2);
                int y = alto / 2 + fm.getAscent() / 2;
                g.drawString(mensajeVacio, x, y);
                return;
            }

            int filas = barras.tamano();
            double maximo = 0;
            for (Barra barra : barras) {
                maximo = Math.max(maximo, barra.getValor());
            }
            if (maximo <= 0) {
                maximo = 1;
            }

            int anchoEtiqueta = Math.min(150, Math.max(80, (int) (ancho * 0.32)));
            int anchoValor = Math.min(120, Math.max(64, (int) (ancho * 0.22)));
            int anchoBarras = ancho - RELLENO * 2 - anchoEtiqueta - anchoValor - 10;
            if (anchoBarras < 20) {
                anchoBarras = 20;
            }

            int altoFila = (alto - RELLENO * 2) / filas;
            altoFila = Math.max(ALTO_MINIMO_FILA, Math.min(ALTO_MAXIMO_FILA, altoFila));
            int altoBarra = Math.max(10, altoFila - 14);

            g.setFont(Estilo.pequena());
            FontMetrics fm = g.getFontMetrics();

            int indice = 0;
            for (Barra barra : barras) {
                int yFila = RELLENO + indice * altoFila;
                int centro = yFila + altoFila / 2;

                // Etiqueta, recortada con puntos suspensivos si no cabe.
                String etiqueta = recortar(barra.getEtiqueta(), fm, anchoEtiqueta - 6);
                int anchoTexto = fm.stringWidth(etiqueta);
                g.setColor(Estilo.TEXTO);
                g.drawString(etiqueta, RELLENO + anchoEtiqueta - 6 - anchoTexto,
                        centro + fm.getAscent() / 2 - 1);

                // Barra proporcional al máximo de la serie.
                int xBarra = RELLENO + anchoEtiqueta;
                int anchoPintado = (int) Math.round(anchoBarras * (barra.getValor() / maximo));
                if (barra.getValor() > 0 && anchoPintado < 3) {
                    anchoPintado = 3;
                }
                int yBarra = centro - altoBarra / 2;

                g.setColor(barra.getColor() == null ? Estilo.PRIMARIO : barra.getColor());
                g.fillRoundRect(xBarra, yBarra, Math.max(anchoPintado, 0), altoBarra,
                        altoBarra, altoBarra);

                // Valor numérico a la derecha de la pista de barras.
                String valor = formatear(barra.getValor());
                int anchoValorTexto = fm.stringWidth(valor);
                int xValor = xBarra + anchoBarras + 8;
                if (xValor + anchoValorTexto > ancho - RELLENO) {
                    xValor = Math.max(xBarra + 4, ancho - RELLENO - anchoValorTexto);
                }
                g.setColor(Estilo.TEXTO_SUAVE);
                g.drawString(valor, xValor, centro + fm.getAscent() / 2 - 1);
                indice++;
            }
        } finally {
            g.dispose();
        }
    }

    private String formatear(double valor) {
        if (formateador != null) {
            return formateador.formatear(valor);
        }
        if (valor == Math.rint(valor)) {
            return String.valueOf((long) valor);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", valor);
    }

    /** Recorta el texto con puntos suspensivos si no cabe en el ancho dado. */
    private String recortar(String texto, FontMetrics fm, int anchoMaximo) {
        if (texto == null) {
            return "";
        }
        if (fm.stringWidth(texto) <= anchoMaximo) {
            return texto;
        }
        String puntos = "…";
        int limite = texto.length();
        while (limite > 0
                && fm.stringWidth(texto.substring(0, limite) + puntos) > anchoMaximo) {
            limite--;
        }
        return texto.substring(0, limite) + puntos;
    }
}
