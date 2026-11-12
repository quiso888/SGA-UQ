package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import model.Apartamento;
import model.EstadoApartamento;
import model.Reserva;
import model.Temporada;
import service.SistemaAlojamiento;

/**
 * Panel del tablero de estados.
 *
 * Muestra una tarjeta por cada apartamento con el código de colores exigido por
 * el enunciado:
 *   verde: preparado,
 *   azul: ocupado,
 *   amarillo: pendiente de preparación o en preparación,
 *   rojo: fuera de servicio.
 */
public class PanelTablero extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final JPanel rejilla = new JPanel(new GridLayout(0, 4, 14, 14));
    private final JLabel resumen = new JLabel(" ");
    private final JLabel fecha = new JLabel(" ");
    private final JLabel turno = new JLabel(" ");

    public PanelTablero(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));
        add(construirCabecera(), BorderLayout.NORTH);

        rejilla.setOpaque(false);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(rejilla, BorderLayout.NORTH);

        JScrollPane desplazamiento = new JScrollPane(contenedor);
        desplazamiento.setBorder(null);
        desplazamiento.getViewport().setBackground(Estilo.FONDO);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(18);
        add(desplazamiento, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        resumen.setFont(Estilo.pequena());
        resumen.setForeground(Estilo.TEXTO_SUAVE);
        pie.add(resumen, BorderLayout.WEST);
        pie.add(construirLeyenda(), BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        contexto.registrar(this);
        actualizar();
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Tablero de apartamentos"));
        textos.add(Estilo.etiquetaSuave(
                "Aparta-Suites UQ · Salento, Quindío · estado operativo en tiempo real"));
        cabecera.add(textos, BorderLayout.WEST);

        fecha.setFont(Estilo.normal());
        fecha.setForeground(Estilo.TEXTO_SUAVE);

        turno.setFont(Estilo.pequena());
        turno.setForeground(Estilo.PRIMARIO_OSCURO);

        JPanel panelTurno = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panelTurno.setOpaque(false);
        panelTurno.add(turno);
        JButton rotar = Estilo.boton("Rotar turno", Estilo.PRIMARIO);
        rotar.addActionListener(evento -> {
            contexto.getSistema().rotarTurnoRecepcion();
            actualizar();
        });
        panelTurno.add(rotar);

        JPanel derecha = new JPanel();
        derecha.setOpaque(false);
        derecha.setLayout(new BoxLayout(derecha, BoxLayout.Y_AXIS));
        JPanel filaFecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        filaFecha.setOpaque(false);
        filaFecha.add(fecha);
        derecha.add(filaFecha);
        derecha.add(panelTurno);
        cabecera.add(derecha, BorderLayout.EAST);
        return cabecera;
    }

    /**
     * Leyenda del código de colores del tablero.
     */
    private JPanel construirLeyenda() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        leyenda.setOpaque(false);
        leyenda.add(itemLeyenda("Preparado", EstadoApartamento.PREPARADO.getColorRgb()));
        leyenda.add(itemLeyenda("Ocupado", EstadoApartamento.OCUPADO.getColorRgb()));
        leyenda.add(itemLeyenda("Por preparar", EstadoApartamento.PENDIENTE_PREPARACION.getColorRgb()));
        leyenda.add(itemLeyenda("Fuera de servicio", EstadoApartamento.FUERA_DE_SERVICIO.getColorRgb()));
        return leyenda;
    }

    private JPanel itemLeyenda(String texto, int rgb) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JLabel muestra = new JLabel("   ");
        muestra.setOpaque(true);
        muestra.setBackground(new Color(rgb));
        muestra.setBorder(BorderFactory.createLineBorder(new Color(rgb).darker()));
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
        rejilla.removeAll();

        for (Apartamento apartamento : sistema.todosApartamentos()) {
            rejilla.add(tarjeta(apartamento));
        }
        if (sistema.todosApartamentos().estaVacia()) {
            JLabel vacio = new JLabel("Todavía no hay apartamentos registrados.");
            vacio.setFont(Estilo.normal());
            vacio.setForeground(Estilo.TEXTO_SUAVE);
            rejilla.add(vacio);
        }

        int[] conteo = sistema.getServicioEstadisticas().apartamentosPorEstado();
        resumen.setText("Total: " + sistema.todosApartamentos().tamano()
                + "   ·   Preparados: " + conteo[EstadoApartamento.PREPARADO.ordinal()]
                + "   ·   Ocupados: " + conteo[EstadoApartamento.OCUPADO.ordinal()]
                + "   ·   Por preparar: " + (conteo[EstadoApartamento.PENDIENTE_PREPARACION.ordinal()]
                + conteo[EstadoApartamento.EN_PREPARACION.ordinal()])
                + "   ·   Fuera de servicio: " + conteo[EstadoApartamento.FUERA_DE_SERVICIO.ordinal()]);
        fecha.setText("Hoy: " + Formato.fecha(LocalDate.now()));
        model.Empleado recepcionista = sistema.recepcionistaDeTurno();
        turno.setText(recepcionista == null
                ? "Sin recepcionista de turno"
                : "Turno de recepción: " + recepcionista.getNombre());

        rejilla.revalidate();
        rejilla.repaint();
    }

    /**
     * Construye la tarjeta visual de un apartamento.
     */
    private JPanel tarjeta(Apartamento apartamento) {
        Color color = new Color(apartamento.getEstado().getColorRgb());

        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(Estilo.TARJETA);
        tarjeta.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        tarjeta.setPreferredSize(new Dimension(240, 198));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(color);
        cabecera.setBorder(Estilo.relleno(8));
        JLabel codigo = new JLabel(apartamento.getCodigo());
        codigo.setFont(Estilo.subtitulo());
        codigo.setForeground(Formato.textoContraste(color));
        cabecera.add(codigo, BorderLayout.WEST);

        JLabel estado = new JLabel(apartamento.getEstado().getDescripcion());
        estado.setFont(Estilo.pequena());
        estado.setForeground(Formato.textoContraste(color));
        cabecera.add(estado, BorderLayout.EAST);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel();
        cuerpo.setBackground(Estilo.TARJETA);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(Estilo.relleno(10));

        cuerpo.add(linea(apartamento.getNombre(), Estilo.normal(), Estilo.TEXTO));
        cuerpo.add(linea("Bloque " + apartamento.getBloque() + " · Piso " + apartamento.getPiso()
                        + " · " + apartamento.getNumeroDormitorios() + " dormitorios",
                Estilo.pequena(), Estilo.TEXTO_SUAVE));
        cuerpo.add(linea("Capacidad máxima: " + apartamento.getCapacidadMaxima() + " personas",
                Estilo.pequena(), Estilo.TEXTO_SUAVE));
        cuerpo.add(linea("Tarifa baja: " + Formato.moneda(apartamento.precioPorPersona(Temporada.BAJA))
                + " por persona/noche", Estilo.pequena(), Estilo.TEXTO_SUAVE));

        Reserva proxima = proximaReserva(apartamento, LocalDate.now());
        if (proxima != null) {
            JLabel reserva = new JLabel("<html><b>" + proxima.getCodigo() + "</b> · "
                    + proxima.getEstado().getDescripcion()
                    + "<br>del " + Formato.fecha(proxima.getEstancia().getFechaEntrada())
                    + " al " + Formato.fecha(proxima.getEstancia().getFechaSalida()) + "</html>");
            reserva.setFont(Estilo.pequena());
            reserva.setForeground(Estilo.PRIMARIO_OSCURO);
            cuerpo.add(javax.swing.Box.createVerticalStrut(6));
            cuerpo.add(reserva);
        } else {
            cuerpo.add(javax.swing.Box.createVerticalStrut(6));
            cuerpo.add(linea("Sin reservas próximas", Estilo.pequena(), Estilo.TEXTO_SUAVE));
        }

        if (apartamento.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO
                && apartamento.getMotivoFueraDeServicio() != null) {
            cuerpo.add(javax.swing.Box.createVerticalStrut(6));
            cuerpo.add(linea("Motivo: " + apartamento.getMotivoFueraDeServicio(),
                    Estilo.pequena(), Estilo.PELIGRO));
        }
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    private JLabel linea(String texto, java.awt.Font fuente, Color color) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente);
        etiqueta.setForeground(color);
        return etiqueta;
    }

    /**
     * Reserva vigente más próxima del apartamento (la que está en curso o la
     * siguiente por entrar).
     */
    private Reserva proximaReserva(Apartamento apartamento, LocalDate hoy) {
        Reserva elegida = null;
        for (Reserva reserva : apartamento.getReservas()) {
            if (!reserva.estaVigente()) {
                continue;
            }
            if (!reserva.getEstancia().getFechaSalida().isAfter(hoy)) {
                continue;
            }
            if (elegida == null
                    || reserva.getEstancia().getFechaEntrada()
                    .isBefore(elegida.getEstancia().getFechaEntrada())) {
                elegida = reserva;
            }
        }
        return elegida;
    }
}
