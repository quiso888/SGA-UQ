package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import datastructures.ListaEnlazada;
import model.CanalReserva;
import model.EstadoApartamento;
import model.EstadoReserva;
import model.Temporada;
import service.ServicioEstadisticas;
import service.SistemaAlojamiento;

/**
 * Panel de estadísticas del alojamiento.
 *
 * Resume los indicadores del negocio en seis tarjetas y cuatro gráficos de
 * barras: reservas por estado, ingresos por canal, ingresos por temporada y
 * apartamentos por estado operativo. Todos los datos provienen de
 * {ServicioEstadisticas}.
 */
public class PanelEstadisticas extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;

    private final JLabel valorIngresos = valorIndicador();
    private final JLabel valorOcupacion = valorIndicador();
    private final JLabel valorReservas = valorIndicador();
    private final JLabel valorCancelacion = valorIndicador();
    private final JLabel valorConflictos = valorIndicador();
    private final JLabel valorPreparacion = valorIndicador();

    private final GraficoBarras graficoReservas =
            new GraficoBarras(valor -> String.valueOf(Math.round(valor)));
    private final GraficoBarras graficoReservasCanal =
            new GraficoBarras(valor -> String.valueOf(Math.round(valor)));
    private final GraficoBarras graficoCanales = new GraficoBarras(Formato::moneda);
    private final GraficoBarras graficoTemporadas = new GraficoBarras(Formato::moneda);
    private final GraficoBarras graficoApartamentos =
            new GraficoBarras(valor -> String.valueOf(Math.round(valor)));
    private final GraficoBarras graficoNoches =
            new GraficoBarras(valor -> String.valueOf(Math.round(valor)));

    private final GraficoBarras graficoOcupacionTemporada =
            new GraficoBarras(valor -> String.format(java.util.Locale.ROOT, "%.0f %%", valor));
    private final GraficoBarras graficoIngresosMes = new GraficoBarras(Formato::moneda);

    private final JLabel pie = new JLabel(" ");

    public PanelEstadisticas(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        add(construirCabecera(), BorderLayout.NORTH);

        JPanel centro = Estilo.transparente(new BorderLayout(0, 14));
        centro.add(construirIndicadores(), BorderLayout.NORTH);
        centro.add(construirGraficos(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        pie.setFont(Estilo.pequena());
        pie.setForeground(Estilo.TEXTO_SUAVE);
        add(pie, BorderLayout.SOUTH);

        contexto.registrar(this);
        actualizar();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(Estilo.etiquetaTitulo("Estadísticas del alojamiento"));
        textos.add(Estilo.etiquetaSuave(
                "Ocupación, ingresos, cancelaciones, conflictos de canal y tiempos de preparación"));
        cabecera.add(textos, BorderLayout.WEST);

        JButton actualizar = Estilo.boton("Actualizar", Estilo.PRIMARIO);
        actualizar.addActionListener(evento -> contexto.notificar());
        cabecera.add(actualizar, BorderLayout.EAST);
        return cabecera;
    }

    private JPanel construirIndicadores() {
        JPanel rejilla = Estilo.transparente(new GridLayout(1, 6, 12, 12));

        rejilla.add(tarjetaIndicador("Ingresos totales", valorIngresos, Estilo.EXITO));
        rejilla.add(tarjetaIndicador("Ocupación de hoy", valorOcupacion, Estilo.PRIMARIO));
        rejilla.add(tarjetaIndicador("Reservas registradas", valorReservas, new Color(0x00838F)));
        rejilla.add(tarjetaIndicador("Tasa de cancelación", valorCancelacion, Estilo.PELIGRO));
        rejilla.add(tarjetaIndicador("Conflictos de canal", valorConflictos, new Color(0xEF6C00)));
        rejilla.add(tarjetaIndicador("Preparación promedio", valorPreparacion, new Color(0x6A1B9A)));
        return rejilla;
    }

    private JPanel tarjetaIndicador(String titulo, JLabel valor, Color acento) {
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(Estilo.TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, acento),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Estilo.BORDE),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12))));

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(Estilo.pequena());
        etiqueta.setForeground(Estilo.TEXTO_SUAVE);
        tarjeta.add(etiqueta, BorderLayout.NORTH);

        valor.setForeground(acento.darker());
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel construirGraficos() {
        JPanel rejilla = Estilo.transparente(new GridLayout(2, 4, 14, 14));

        graficoReservas.setMensajeVacio("Todavía no hay reservas registradas.");
        graficoReservasCanal.setMensajeVacio("Todavía no hay reservas registradas.");
        graficoCanales.setMensajeVacio("Todavía no hay ingresos registrados.");
        graficoTemporadas.setMensajeVacio("Todavía no hay noches liquidadas.");
        graficoApartamentos.setMensajeVacio("Todavía no hay apartamentos registrados.");
        graficoNoches.setMensajeVacio("Todavía no hay noches reservadas.");
        graficoOcupacionTemporada.setMensajeVacio("Todavía no hay noches ocupadas.");
        graficoIngresosMes.setMensajeVacio("Todavía no hay ingresos registrados.");

        rejilla.add(panelGrafico("Reservas por estado",
                "Ciclo de vida completo de las reservas", graficoReservas));
        rejilla.add(panelGrafico("Reservas por canal",
                "Portal, directa y externa · incluye canceladas y no-show",
                graficoReservasCanal));
        rejilla.add(panelGrafico("Ingresos por canal",
                "Aportación económica de cada canal", graficoCanales));
        rejilla.add(panelGrafico("Ingresos por temporada",
                "Liquidación noche por noche según el calendario", graficoTemporadas));
        rejilla.add(panelGrafico("Apartamentos por estado",
                "Situación operativa del edificio", graficoApartamentos));
        rejilla.add(panelGrafico("Ocupación por apartamento",
                "Noches reservadas acumuladas por unidad", graficoNoches));
        rejilla.add(panelGrafico("Ocupación por temporada",
                "Noches ocupadas sobre las noches disponibles", graficoOcupacionTemporada));
        rejilla.add(panelGrafico("Ingresos por periodo",
                "Ingreso de las noches de cada mes", graficoIngresosMes));
        return rejilla;
    }

    private JPanel panelGrafico(String titulo, String descripcion, GraficoBarras grafico) {
        JPanel panel = Estilo.tarjeta();

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));

        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setFont(Estilo.subtitulo());
        etiquetaTitulo.setForeground(Estilo.TEXTO);
        encabezado.add(etiquetaTitulo);

        JLabel etiquetaDescripcion = new JLabel(descripcion);
        etiquetaDescripcion.setFont(Estilo.pequena());
        etiquetaDescripcion.setForeground(Estilo.TEXTO_SUAVE);
        encabezado.add(etiquetaDescripcion);
        encabezado.add(Box.createVerticalStrut(6));

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(grafico, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------
    // Datos
    // ------------------------------------------------------------------

    @Override
    public void actualizar() {
        SistemaAlojamiento sistema = contexto.getSistema();
        ServicioEstadisticas estadisticas = sistema.getServicioEstadisticas();

        valorIngresos.setText(Formato.moneda(estadisticas.ingresoTotal()));
        valorOcupacion.setText(Formato.porcentaje(
                estadisticas.porcentajeOcupacion(LocalDate.now())));
        valorReservas.setText(String.valueOf(estadisticas.totalReservas()));
        valorCancelacion.setText(Formato.porcentaje(estadisticas.tasaCancelacion()));
        valorConflictos.setText(String.valueOf(estadisticas.numeroConflictosCanal()));
        valorPreparacion.setText(String.format(java.util.Locale.ROOT, "%.0f min",
                estadisticas.tiempoMedioPreparacionMinutos()));

        graficoReservas.setBarras(barrasReservasPorEstado(estadisticas.reservasPorEstado()));
        graficoReservasCanal.setBarras(
                barrasReservasPorCanal(estadisticas.reservasPorCanal()));
        graficoCanales.setBarras(barrasIngresosPorCanal(estadisticas.ingresosPorCanal()));
        graficoTemporadas.setBarras(
                barrasIngresosPorTemporada(estadisticas.ingresosPorTemporada()));
        graficoApartamentos.setBarras(
                barrasApartamentosPorEstado(estadisticas.apartamentosPorEstado()));
        graficoNoches.setBarras(
                barrasNochesPorApartamento(estadisticas.nochesReservadasPorApartamento()));

        graficoOcupacionTemporada.setBarras(barrasOcupacionPorTemporada(
                estadisticas.porcentajeOcupacionPorTemporada()));
        graficoIngresosMes.setBarras(barrasIngresosPorMes(estadisticas.ingresosPorMes()));

        pie.setText(String.format(java.util.Locale.ROOT,
                "Ingreso promedio por reserva: %s   ·   No-show: %s   ·   "
                        + "Apartamentos por preparar: %d   ·   Actualizado: %s",
                Formato.moneda(estadisticas.ingresoPromedio()),
                Formato.porcentaje(estadisticas.tasaNoShow()),
                estadisticas.apartamentosPorPreparar(),
                Formato.fechaHora(LocalDateTime.now())));

        revalidate();
        repaint();
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasReservasPorEstado(int[] conteo) {
        Color[] colores = {
                Estilo.ADVERTENCIA,
                Estilo.PRIMARIO,
                new Color(0x00838F),
                Estilo.EXITO,
                Estilo.PELIGRO,
                new Color(0x78909C)
        };

        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (EstadoReserva estado : EstadoReserva.values()) {
            barras.agregar(new GraficoBarras.Barra(estado.getDescripcion(),
                    conteo[estado.ordinal()], colores[estado.ordinal() % colores.length]));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasIngresosPorCanal(double[] ingresos) {
        Color[] colores = {
                Estilo.PRIMARIO,
                Estilo.EXITO,
                new Color(0xEF6C00)
        };

        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (CanalReserva canal : CanalReserva.values()) {
            barras.agregar(new GraficoBarras.Barra(canal.getDescripcion(),
                    ingresos[canal.ordinal()], colores[canal.ordinal() % colores.length]));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasReservasPorCanal(int[] conteo) {
        Color[] colores = {
                Estilo.PRIMARIO,
                Estilo.EXITO,
                new Color(0xEF6C00)
        };

        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (CanalReserva canal : CanalReserva.values()) {
            barras.agregar(new GraficoBarras.Barra(canal.getDescripcion(),
                    conteo[canal.ordinal()], colores[canal.ordinal() % colores.length]));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasNochesPorApartamento(int[] noches) {
        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        ListaEnlazada<model.Apartamento> apartamentos =
                contexto.getSistema().todosApartamentos();
        int indice = 0;
        for (model.Apartamento apartamento : apartamentos) {
            if (indice >= noches.length) {
                break;
            }
            barras.agregar(new GraficoBarras.Barra(apartamento.getCodigo(),
                    noches[indice], Estilo.PRIMARIO));
            indice++;
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasOcupacionPorTemporada(double[] porcentaje) {
        Color[] colores = {
                new Color(0x66BB6A),
                Estilo.ADVERTENCIA,
                Estilo.PELIGRO
        };
        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (Temporada temporada : Temporada.values()) {
            barras.agregar(new GraficoBarras.Barra(temporada.getDescripcion(),
                    porcentaje[temporada.ordinal()], colores[temporada.ordinal() % colores.length]));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasIngresosPorMes(
            ListaEnlazada<ServicioEstadisticas.IngresoPeriodo> meses) {
        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (ServicioEstadisticas.IngresoPeriodo mes : meses) {
            barras.agregar(new GraficoBarras.Barra(mes.getPeriodo().toString(),
                    mes.getIngreso(), Estilo.PRIMARIO));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasIngresosPorTemporada(double[] ingresos) {
        Color[] colores = {
                new Color(0x66BB6A),
                Estilo.ADVERTENCIA,
                Estilo.PELIGRO
        };

        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (Temporada temporada : Temporada.values()) {
            barras.agregar(new GraficoBarras.Barra("Temporada " + temporada.getDescripcion(),
                    ingresos[temporada.ordinal()], colores[temporada.ordinal() % colores.length]));
        }
        return barras;
    }

    private ListaEnlazada<GraficoBarras.Barra> barrasApartamentosPorEstado(int[] conteo) {
        ListaEnlazada<GraficoBarras.Barra> barras = new ListaEnlazada<>();
        for (EstadoApartamento estado : EstadoApartamento.values()) {
            barras.agregar(new GraficoBarras.Barra(estado.getDescripcion(),
                    conteo[estado.ordinal()], new Color(estado.getColorRgb())));
        }
        return barras;
    }

    private static JLabel valorIndicador() {
        JLabel etiqueta = new JLabel("—");
        etiqueta.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        return etiqueta;
    }
}
