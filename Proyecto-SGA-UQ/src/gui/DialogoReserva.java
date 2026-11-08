package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import datastructures.ListaEnlazada;
import exceptions.CapacidadExcedidaException;
import exceptions.FechasInvalidasException;
import exceptions.ReservaSolapadaException;
import exceptions.TransicionEstadoInvalidaException;
import model.Apartamento;
import model.CanalReserva;
import model.Comparadores;
import model.Estancia;
import model.Huesped;
import model.Ocupante;
import model.Reserva;
import model.Temporada;
import service.SistemaAlojamiento;

/**
 * Diálogo para crear una reserva.
 *
 * Primero se eligen las fechas y el número de personas; el botón de búsqueda llena
 * la lista de apartamentos disponibles aplicando el criterio de ordenamiento
 * seleccionado (precio de la temporada predominante, capacidad o número de reservas).
 * Después se elige el apartamento y se crea la reserva por el canal Portal (queda
 * Pendiente hasta el anticipo) o por recepción directa (queda Confirmada).
 */
public class DialogoReserva extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Contexto contexto;
    private final SistemaAlojamiento sistema;

    private final JComboBox<Huesped> titular;
    private final JComboBox<CanalReserva> canal;
    private final JTextField entrada = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
    private final JTextField salida = new JTextField(LocalDate.now().plusDays(3).toString(), 12);
    private final JSpinner personas = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
    private final JCheckBox menor = new JCheckBox("Incluir un menor de 6 años (ocupa cupo sin pagar)");
    private final JTextField hora = new JTextField("15:00", 6);
    private final JComboBox<String> orden = new JComboBox<>(new String[] {
            "Precio (temporada predominante)", "Capacidad (mayor primero)",
            "Número de reservas (mayor primero)"
    });
    private final Modelos.ModeloLista<Apartamento> modeloApartamentos = new Modelos.ModeloLista<>();
    private final JComboBox<Apartamento> apartamento = new JComboBox<>(modeloApartamentos);
    private final JLabel total = new JLabel(" ");
    private final JTextArea desglose = new JTextArea(6, 42);

    private boolean creada;

    private DialogoReserva(Component padre, Contexto contexto, Huesped titularFijo) {
        this.contexto = contexto;
        this.sistema = contexto.getSistema();

        setTitle("Nueva reserva · SGA");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.titular = new JComboBox<>(new Modelos.ModeloLista<>(sistema.todosHuespedes()));
        this.canal = new JComboBox<>(new CanalReserva[] {CanalReserva.PORTAL, CanalReserva.DIRECTA});
        if (titularFijo != null) {
            // Un huésped solo reserva para sí mismo y por el portal.
            this.titular.setSelectedItem(titularFijo);
            this.titular.setEnabled(false);
            this.canal.setSelectedItem(CanalReserva.PORTAL);
            this.canal.setEnabled(false);
        }

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(Estilo.relleno(16));

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new javax.swing.BoxLayout(encabezado, javax.swing.BoxLayout.Y_AXIS));
        encabezado.add(Estilo.etiquetaTitulo("Nueva reserva"));
        encabezado.add(Estilo.etiquetaSuave(
                "La noche de salida no se cobra ni se ocupa. El portal exige anticipo del 30 %."));
        contenido.add(encabezado, BorderLayout.NORTH);

        contenido.add(construirCentro(), BorderLayout.CENTER);
        contenido.add(construirBotones(), BorderLayout.SOUTH);

        setContentPane(contenido);
        setSize(660, 720);
        setLocationRelativeTo(padre);

        apartamento.addActionListener(evento -> actualizarTotal());
        personas.addChangeListener(evento -> actualizarTotal());
        menor.addActionListener(evento -> actualizarTotal());

        buscarDisponibilidad();
    }

    /**
     * Abre el diálogo y devuelve {@code true} si se creó una reserva.
     */
    public static boolean mostrar(Component padre, Contexto contexto) {
        return mostrar(padre, contexto, null);
    }

    /**
     * Abre el diálogo para que un huésped reserve por el portal: el titular queda
     * fijo y el canal es siempre Portal.
     */
    public static boolean mostrar(Component padre, Contexto contexto, Huesped titularFijo) {
        DialogoReserva dialogo = new DialogoReserva(padre, contexto, titularFijo);
        dialogo.setVisible(true);
        return dialogo.creada;
    }

    // ------------------------------------------------------------------
    // Interfaz
    // ------------------------------------------------------------------

    private JPanel construirFormulario() {
        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridBagLayout());

        int fila = 0;
        agregarFila(formulario, fila++, "Huésped titular:", titular);
        agregarFila(formulario, fila++, "Canal de la reserva:", canal);
        agregarFila(formulario, fila++, "Fecha de entrada (aaaa-mm-dd):", entrada);
        agregarFila(formulario, fila++, "Fecha de salida (aaaa-mm-dd):", salida);
        agregarFila(formulario, fila++, "Personas del grupo:", personas);
        agregarFila(formulario, fila++, "Hora estimada de llegada (hh:mm):", hora);
        agregarFila(formulario, fila++, "Menor de 6 años:", menor);
        agregarFila(formulario, fila++, "Ordenar disponibilidad por:", orden);

        JButton buscar = Estilo.boton("Buscar apartamentos disponibles", Estilo.PRIMARIO);
        buscar.addActionListener(evento -> buscarDisponibilidad());
        GridBagConstraints restricciones = restricciones(0, fila++, 2);
        formulario.add(buscar, restricciones);

        agregarFila(formulario, fila++, "Apartamento disponible:", apartamento);

        total.setFont(Estilo.normal());
        total.setForeground(Estilo.PRIMARIO_OSCURO);
        restricciones = restricciones(0, fila, 2);
        formulario.add(total, restricciones);
        return formulario;
    }

    /** Formulario y cotización noche por noche, apilados en el centro. */
    private JPanel construirCentro() {
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(construirFormulario(), BorderLayout.NORTH);

        desglose.setEditable(false);
        desglose.setFont(Estilo.mono());
        desglose.setBackground(new Color(0xFAFBFC));
        desglose.setBorder(Estilo.relleno(6));
        JScrollPane scroll = new JScrollPane(desglose);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Estilo.BORDE),
                        "Cotización noche por noche (la noche de salida no se cobra)"),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        scroll.setPreferredSize(new Dimension(600, 150));
        centro.add(scroll, BorderLayout.CENTER);
        return centro;
    }

    private void agregarFila(JPanel panel, int fila, String texto, Component campo) {
        GridBagConstraints etiqueta = restricciones(0, fila, 1);
        etiqueta.weightx = 0;
        etiqueta.fill = GridBagConstraints.NONE;
        etiqueta.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel(texto);
        label.setFont(Estilo.normal());
        panel.add(label, etiqueta);

        GridBagConstraints valor = restricciones(1, fila, 1);
        valor.weightx = 1;
        valor.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, valor);
    }

    private GridBagConstraints restricciones(int x, int y, int ancho) {
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridx = x;
        restricciones.gridy = y;
        restricciones.gridwidth = ancho;
        restricciones.insets = new Insets(4, 4, 4, 4);
        restricciones.anchor = GridBagConstraints.WEST;
        return restricciones;
    }

    private JPanel construirBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);

        JButton cancelar = Estilo.boton("Cancelar", Estilo.TEXTO_SUAVE);
        cancelar.addActionListener(evento -> dispose());
        botones.add(cancelar);

        JButton crear = Estilo.boton("Crear reserva", Estilo.EXITO);
        crear.addActionListener(evento -> crearReserva());
        botones.add(crear);
        return botones;
    }

    // ------------------------------------------------------------------
    // Lógica del diálogo
    // ------------------------------------------------------------------

    private Estancia leerEstancia() {
        LocalDate fechaEntrada = LocalDate.parse(entrada.getText().trim());
        LocalDate fechaSalida = LocalDate.parse(salida.getText().trim());
        return new Estancia(fechaEntrada, fechaSalida);
    }

    private int numeroPersonas() {
        return (Integer) personas.getValue();
    }

    private void buscarDisponibilidad() {
        try {
            Estancia estancia = leerEstancia();
            int personasGrupo = numeroPersonas();
            Temporada temporada = sistema.getServicioTemporadas().temporadaPredominante(estancia);
            Comparator<Apartamento> criterio;
            if (orden.getSelectedIndex() == 1) {
                criterio = new Comparadores.PorCapacidad();
            } else if (orden.getSelectedIndex() == 2) {
                criterio = new Comparadores.PorNumeroReservas();
            } else {
                criterio = new Comparadores.PorPrecio(temporada);
            }
            ListaEnlazada<Apartamento> disponibles =
                    sistema.apartamentosDisponibles(estancia, personasGrupo, criterio);
            modeloApartamentos.setDatos(disponibles);

            if (disponibles.estaVacia()) {
                total.setText("No hay apartamentos disponibles para esas fechas y ese grupo.");
                total.setForeground(Estilo.PELIGRO);
            } else {
                total.setForeground(Estilo.PRIMARIO_OSCURO);
                actualizarTotal();
            }
        } catch (DateTimeParseException excepcion) {
            mensajeError("Fecha inválida",
                    "Escriba las fechas en formato aaaa-mm-dd, por ejemplo " + LocalDate.now() + ".");
        } catch (FechasInvalidasException excepcion) {
            mensajeError("Fechas inválidas", excepcion.getMessage());
        }
    }

    private void actualizarTotal() {
        Apartamento seleccionado = (Apartamento) apartamento.getSelectedItem();
        if (seleccionado == null) {
            total.setText("Seleccione un apartamento para ver el total estimado.");
            desglose.setText("");
            return;
        }
        try {
            Estancia estancia = leerEstancia();
            int personasGrupo = numeroPersonas();
            int menores = menor.isSelected() ? 1 : 0;
            int pagadoras = Math.max(0, personasGrupo - menores);

            StringBuilder detalle = new StringBuilder();
            double valor = 0;
            for (LocalDate noche : estancia) {
                Temporada temporada = sistema.getCalendario().temporadaDe(noche);
                double tarifa = seleccionado.precioPorPersona(temporada);
                double valorNoche = tarifa * pagadoras;
                valor += valorNoche;
                detalle.append(Formato.fecha(noche))
                        .append("  ").append(String.format("%-7s", temporada.getDescripcion()))
                        .append("  ").append(pagadoras).append(" × ")
                        .append(Formato.moneda(tarifa)).append("  =  ")
                        .append(Formato.moneda(valorNoche)).append('\n');
            }
            detalle.append("\nTotal de la estancia: ").append(Formato.moneda(valor));
            desglose.setText(detalle.toString());
            desglose.setCaretPosition(0);

            String detalleMenores = menores == 0 ? ""
                    : " · " + menores + " menor(es) de 6 años sin cargo";
            total.setText("Total estimado: " + Formato.moneda(valor) + " · "
                    + estancia.numeroNoches() + " noches · " + pagadoras
                    + " persona(s) que pagan" + detalleMenores);
        } catch (DateTimeParseException excepcion) {
            total.setText("Fechas inválidas: use aaaa-mm-dd.");
            desglose.setText("");
        } catch (FechasInvalidasException excepcion) {
            total.setText("Fechas inválidas: " + excepcion.getMessage());
            desglose.setText("");
        }
    }

    private ListaEnlazada<Ocupante> construirOcupantes(Huesped huespedTitular, int personasGrupo) {
        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        LocalDate nacimientoTitular = huespedTitular.getFechaNacimiento() == null
                ? LocalDate.now().minusYears(30) : huespedTitular.getFechaNacimiento();
        ocupantes.agregar(new Ocupante(huespedTitular.getNombre(), nacimientoTitular, true));
        for (int i = 2; i <= personasGrupo; i++) {
            LocalDate nacimiento = LocalDate.now().minusYears(30);
            String nombre = "Acompañante " + (i - 1);
            if (menor.isSelected() && i == personasGrupo) {
                nacimiento = LocalDate.now().minusYears(4);
                nombre = "Menor " + (i - 1);
            }
            ocupantes.agregar(new Ocupante(nombre, nacimiento));
        }
        return ocupantes;
    }

    private void crearReserva() {
        Huesped huespedTitular = (Huesped) titular.getSelectedItem();
        if (huespedTitular == null) {
            mensajeError("Falta el titular", "Seleccione el huésped titular de la reserva.");
            return;
        }
        Apartamento apartamentoSeleccionado = (Apartamento) apartamento.getSelectedItem();
        if (apartamentoSeleccionado == null) {
            mensajeError("Falta el apartamento",
                    "Primero busque la disponibilidad y seleccione un apartamento.");
            return;
        }
        try {
            Estancia estancia = leerEstancia();
            LocalTime horaLlegada = LocalTime.parse(hora.getText().trim());
            ListaEnlazada<Ocupante> ocupantes = construirOcupantes(huespedTitular, numeroPersonas());
            CanalReserva canalSeleccionado = (CanalReserva) canal.getSelectedItem();

            Reserva reserva;
            if (canalSeleccionado == CanalReserva.DIRECTA) {
                reserva = sistema.crearReservaDirecta(huespedTitular, apartamentoSeleccionado,
                        estancia, ocupantes, horaLlegada);
            } else {
                reserva = sistema.crearReservaPortal(huespedTitular, apartamentoSeleccionado,
                        estancia, ocupantes, horaLlegada);
            }
            creada = true;

            String mensaje = "Reserva " + reserva.getCodigo() + " creada para "
                    + huespedTitular.getNombre() + " en " + apartamentoSeleccionado.getCodigo()
                    + ".\nEstado: " + reserva.getEstado().getDescripcion()
                    + "\nTotal estimado: " + Formato.moneda(reserva.getTotalEstimado());
            if (reserva.getEstado() == model.EstadoReserva.PENDIENTE) {
                mensaje += "\nAnticipo requerido: " + Formato.moneda(reserva.anticipoRequerido())
                        + "\nVence: " + Formato.fechaHora(reserva.getFechaVencimiento());
            }
            JOptionPane.showMessageDialog(this, mensaje, "Reserva creada",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (DateTimeParseException excepcion) {
            mensajeError("Dato inválido", "Revise las fechas (aaaa-mm-dd) y la hora (hh:mm).");
        } catch (FechasInvalidasException excepcion) {
            mensajeError("Fechas inválidas", excepcion.getMessage());
        } catch (CapacidadExcedidaException excepcion) {
            mensajeError("Capacidad excedida", excepcion.getMessage());
        } catch (ReservaSolapadaException excepcion) {
            mensajeError("Reserva solapada", excepcion.getMessage());
        } catch (TransicionEstadoInvalidaException excepcion) {
            mensajeError("Apartamento no disponible", excepcion.getMessage());
        } catch (IllegalArgumentException excepcion) {
            mensajeError("Datos incompletos", excepcion.getMessage());
        }
    }

    private void mensajeError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }
}
