package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import datastructures.ArbolBinarioBusqueda;
import datastructures.Grafo;
import datastructures.ListaEnlazada;
import gui.Formularios.Campo;
import model.Apartamento;
import model.CalendarioTemporadas;
import model.Empleado;
import model.Huesped;
import model.Temporada;
import service.GestorEdificio;
import service.SistemaAlojamiento;

/**
 * Panel del administrador: gestión jerárquica de empleados, apartamentos y
 * huéspedes (cada catálogo es un árbol binario de búsqueda) y definición de las
 * temporadas y de las tarifas por apartamento.
 *
 * Cada catálogo permite cambiar el recorrido del árbol (in-orden, pre-orden,
 * post-orden y por niveles) para ver cómo está organizado.
 */
public class PanelAdministracion extends JPanel implements Contexto.Actualizable {

    private static final long serialVersionUID = 1L;

    private static final String[] RECORRIDOS = {"In-orden", "Pre-orden", "Post-orden", "Por niveles"};

    private final Contexto contexto;
    private final ListaEnlazada<Contexto.Actualizable> pestanas = new ListaEnlazada<>();

    public PanelAdministracion(Contexto contexto) {
        this.contexto = contexto;
        setLayout(new BorderLayout(0, 12));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.relleno(16));

        JPanel cabecera = new JPanel();
        cabecera.setOpaque(false);
        cabecera.setLayout(new javax.swing.BoxLayout(cabecera, javax.swing.BoxLayout.Y_AXIS));
        cabecera.add(Estilo.etiquetaTitulo("Administración"));
        cabecera.add(Estilo.etiquetaSuave("Empleados, apartamentos y huéspedes en árboles de "
                + "búsqueda · temporadas y tarifas por apartamento"));
        add(cabecera, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Estilo.normal());
        agregar(tabs, "Empleados", new PestanaEmpleados());
        agregar(tabs, "Apartamentos", new PestanaApartamentos());
        agregar(tabs, "Huéspedes", new PestanaHuespedes());
        agregar(tabs, "Temporadas y tarifas", new PestanaTemporadas());
        add(tabs, BorderLayout.CENTER);

        contexto.registrar(this);
        actualizar();
    }

    private void agregar(JTabbedPane tabs, String titulo, JPanel pestana) {
        tabs.addTab(titulo, pestana);
        pestanas.agregar((Contexto.Actualizable) pestana);
    }

    @Override
    public void actualizar() {
        for (Contexto.Actualizable pestana : pestanas) {
            pestana.actualizar();
        }
    }

    private void error(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    private SistemaAlojamiento sistema() {
        return contexto.getSistema();
    }

    private static LocalDate fechaOpcional(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : LocalDate.parse(texto.trim());
    }

    // ==================================================================
    // Pestaña genérica para un catálogo guardado en un árbol de búsqueda
    // ==================================================================

    private abstract class PestanaCatalogo<T extends Comparable<T>> extends JPanel
            implements Contexto.Actualizable {

        private static final long serialVersionUID = 1L;

        private final Modelos.ModeloTabla<T> modelo;
        private final JTable tabla;
        private final JComboBox<String> recorrido = new JComboBox<>(RECORRIDOS);
        private final JLabel pie = Estilo.etiquetaSuave(" ");

        PestanaCatalogo(String[] columnas, String etiquetaNuevo) {
            super(new BorderLayout(0, 10));
            setOpaque(false);
            setBorder(Estilo.relleno(10));
            modelo = new Modelos.ModeloTabla<>(columnas, (fila, columna) -> valor(fila, columna), null);
            tabla = new JTable(modelo);
            tabla.setRowHeight(26);
            tabla.setFont(Estilo.pequena());
            tabla.getTableHeader().setFont(Estilo.pequena());
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            barra.setOpaque(false);
            JButton nuevo = Estilo.boton(etiquetaNuevo, Estilo.EXITO);
            nuevo.addActionListener(evento -> {
                nuevo();
                contexto.notificar();
            });
            barra.add(nuevo);
            JButton editar = Estilo.boton("Editar", Estilo.PRIMARIO);
            editar.addActionListener(evento -> {
                T elemento = seleccionado();
                if (elemento != null) {
                    editar(elemento);
                    contexto.notificar();
                }
            });
            barra.add(editar);
            JButton eliminar = Estilo.boton("Eliminar", Estilo.PELIGRO);
            eliminar.addActionListener(evento -> {
                T elemento = seleccionado();
                if (elemento != null && confirmar(elemento)) {
                    eliminar(elemento);
                    contexto.notificar();
                }
            });
            barra.add(eliminar);
            barra.add(Estilo.espacio(16));
            barra.add(new JLabel("Recorrido del árbol:"));
            recorrido.setFont(Estilo.pequena());
            recorrido.addActionListener(evento -> actualizar());
            barra.add(recorrido);
            add(barra, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
            add(scroll, BorderLayout.CENTER);
            pie.setFont(Estilo.pequena());
            add(pie, BorderLayout.SOUTH);
        }

        abstract ArbolBinarioBusqueda<T> arbol();

        abstract Object valor(T fila, int columna);

        abstract void nuevo();

        abstract void editar(T elemento);

        abstract void eliminar(T elemento);

        abstract String descripcion(T elemento);

        private boolean confirmar(T elemento) {
            return JOptionPane.showConfirmDialog(PanelAdministracion.this,
                    "¿Eliminar " + descripcion(elemento) + "?", "Confirmar baja",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                    == JOptionPane.YES_OPTION;
        }

        private T seleccionado() {
            int fila = tabla.getSelectedRow();
            if (fila < 0 || fila >= modelo.getRowCount()) {
                JOptionPane.showMessageDialog(PanelAdministracion.this,
                        "Seleccione una fila de la tabla.", "Sin selección",
                        JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return modelo.getFila(fila);
        }

        @Override
        public void actualizar() {
            ArbolBinarioBusqueda<T> arbol = arbol();
            ListaEnlazada<T> filas;
            switch (recorrido.getSelectedIndex()) {
                case 1:
                    filas = arbol.recorridoPreOrden();
                    break;
                case 2:
                    filas = arbol.recorridoPostOrden();
                    break;
                case 3:
                    filas = arbol.recorridoPorNivel();
                    break;
                default:
                    filas = arbol.recorridoInOrden();
                    break;
            }
            modelo.setFilas(filas);
            pie.setText("Árbol binario de búsqueda: " + arbol.tamano() + " nodo(s) · altura "
                    + arbol.altura() + " · recorrido " + recorrido.getSelectedItem());
        }

        void ejecutar(Runnable accion) {
            try {
                accion.run();
            } catch (DateTimeParseException excepcion) {
                error("Fecha inválida", "Use el formato aaaa-mm-dd.");
            } catch (NumberFormatException excepcion) {
                error("Número inválido", "Revise los campos numéricos.");
            } catch (RuntimeException excepcion) {
                error("No se pudo completar la operación", excepcion.getMessage());
            }
        }
    }

    // ==================================================================
    // Empleados
    // ==================================================================

    private class PestanaEmpleados extends PestanaCatalogo<Empleado> {

        private static final long serialVersionUID = 1L;

        PestanaEmpleados() {
            super(new String[] {"Documento", "Nombre", "Cargo", "Turno de recepción"},
                    "Nuevo empleado");
        }

        @Override
        ArbolBinarioBusqueda<Empleado> arbol() {
            return sistema().getArbolEmpleados();
        }

        @Override
        Object valor(Empleado empleado, int columna) {
            switch (columna) {
                case 0:
                    return empleado.getDocumento();
                case 1:
                    return empleado.getNombre();
                case 2:
                    return empleado.getCargo().getDescripcion();
                case 3:
                    return empleado.equals(sistema().recepcionistaDeTurno()) ? "Turno actual" : "";
                default:
                    return "";
            }
        }

        private String[] cargos() {
            Empleado.Cargo[] valores = Empleado.Cargo.values();
            String[] nombres = new String[valores.length];
            for (int i = 0; i < valores.length; i++) {
                nombres[i] = valores[i].getDescripcion();
            }
            return nombres;
        }

        private Empleado.Cargo cargo(String texto) {
            for (Empleado.Cargo cargo : Empleado.Cargo.values()) {
                if (cargo.getDescripcion().equalsIgnoreCase(texto)) {
                    return cargo;
                }
            }
            throw new IllegalArgumentException("Cargo no válido: " + texto);
        }

        @Override
        void nuevo() {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Nuevo empleado",
                    Formularios.campos(Campo.texto("Documento", ""), Campo.texto("Nombre", ""),
                            Campo.opciones("Cargo", Empleado.Cargo.RECEPCIONISTA.getDescripcion(),
                                    cargos())));
            if (datos == null) {
                return;
            }
            ejecutar(() -> {
                if (sistema().getArbolEmpleados()
                        .contiene(Empleado.claveDeBusqueda(datos[0]))) {
                    throw new IllegalArgumentException(
                            "Ya existe un empleado con el documento " + datos[0] + ".");
                }
                sistema().registrarEmpleado(new Empleado(datos[0], datos[1], cargo(datos[2])));
            });
        }

        @Override
        void editar(Empleado empleado) {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Editar empleado",
                    Formularios.campos(Campo.fijo("Documento", empleado.getDocumento()),
                            Campo.texto("Nombre", empleado.getNombre()),
                            Campo.opciones("Cargo", empleado.getCargo().getDescripcion(), cargos())));
            if (datos == null) {
                return;
            }
            ejecutar(() -> sistema().actualizarEmpleado(empleado.getDocumento(), datos[1],
                    cargo(datos[2])));
        }

        @Override
        void eliminar(Empleado empleado) {
            ejecutar(() -> sistema().eliminarEmpleado(empleado.getDocumento()));
        }

        @Override
        String descripcion(Empleado empleado) {
            return "al empleado " + empleado.getNombre();
        }
    }

    // ==================================================================
    // Apartamentos
    // ==================================================================

    private class PestanaApartamentos extends PestanaCatalogo<Apartamento> {

        private static final long serialVersionUID = 1L;

        PestanaApartamentos() {
            super(new String[] {"Código", "Nombre", "Bloque", "Piso", "Dorm.", "Capacidad",
                    "Estado", "Reservas"}, "Nuevo apartamento");
        }

        @Override
        ArbolBinarioBusqueda<Apartamento> arbol() {
            return sistema().getGestorReservas().getCatalogoApartamentos();
        }

        @Override
        Object valor(Apartamento a, int columna) {
            switch (columna) {
                case 0:
                    return a.getCodigo();
                case 1:
                    return a.getNombre();
                case 2:
                    return a.getBloque();
                case 3:
                    return a.getPiso();
                case 4:
                    return a.getNumeroDormitorios();
                case 5:
                    return a.getCapacidadMaxima();
                case 6:
                    return a.getEstado().getDescripcion();
                case 7:
                    return a.numeroReservas();
                default:
                    return "";
            }
        }

        private String[] nodosDelMapa() {
            Grafo<String> grafo = sistema().getGestorEdificio().getGrafo();
            ListaEnlazada<String> etiquetas = new ListaEnlazada<>();
            for (Grafo.Nodo<String> nodo : grafo) {
                etiquetas.agregar(nodo.getEtiqueta());
            }
            String[] nombres = new String[etiquetas.tamano()];
            int i = 0;
            for (String etiqueta : etiquetas) {
                nombres[i++] = etiqueta;
            }
            return nombres;
        }

        @Override
        void nuevo() {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Nuevo apartamento",
                    Formularios.campos(Campo.texto("Código", ""), Campo.texto("Nombre", ""),
                            Campo.texto("Bloque", "A"), Campo.texto("Piso", "1"),
                            Campo.texto("Dormitorios", "1"), Campo.texto("Capacidad máxima", "2"),
                            Campo.texto("Tarifa Baja (por persona/noche)", "120000"),
                            Campo.texto("Tarifa Media", "160000"),
                            Campo.texto("Tarifa Alta", "210000"),
                            Campo.opciones("Pasillo hacia (nodo del mapa)",
                                    GestorEdificio.RECEPCION, nodosDelMapa()),
                            Campo.texto("Distancia del pasillo (m)", "10")));
            if (datos == null) {
                return;
            }
            ejecutar(() -> {
                int piso = Integer.parseInt(datos[3]);
                int ocupadosEnPiso = 0;
                for (Apartamento existente : sistema().todosApartamentos()) {
                    if (existente.getPiso() == piso) {
                        ocupadosEnPiso++;
                    }
                }
                Apartamento nuevo = new Apartamento(datos[0], datos[1], datos[2], piso,
                        Integer.parseInt(datos[4]), Integer.parseInt(datos[5]),
                        110 + 140.0 * ocupadosEnPiso, 120 + 130.0 * Math.max(0, piso - 1));
                nuevo.definirTarifa(Temporada.BAJA, Double.parseDouble(datos[6]));
                nuevo.definirTarifa(Temporada.MEDIA, Double.parseDouble(datos[7]));
                nuevo.definirTarifa(Temporada.ALTA, Double.parseDouble(datos[8]));
                if (sistema().getGestorReservas().buscarApartamentoOpcional(datos[0]) != null) {
                    throw new IllegalArgumentException(
                            "Ya existe un apartamento con el código " + datos[0] + ".");
                }
                sistema().registrarApartamento(nuevo);
                if (!datos[9].isEmpty()) {
                    sistema().getGestorEdificio().conectar(nuevo.getCodigo(), datos[9],
                            Integer.parseInt(datos[10]));
                }
            });
        }

        @Override
        void editar(Apartamento a) {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Editar apartamento",
                    Formularios.campos(Campo.fijo("Código", a.getCodigo()),
                            Campo.texto("Nombre", a.getNombre()),
                            Campo.texto("Bloque", a.getBloque()),
                            Campo.texto("Piso", String.valueOf(a.getPiso())),
                            Campo.texto("Dormitorios", String.valueOf(a.getNumeroDormitorios())),
                            Campo.texto("Capacidad máxima", String.valueOf(a.getCapacidadMaxima()))));
            if (datos == null) {
                return;
            }
            ejecutar(() -> a.actualizarDatos(datos[1], datos[2], Integer.parseInt(datos[3]),
                    Integer.parseInt(datos[4]), Integer.parseInt(datos[5])));
        }

        @Override
        void eliminar(Apartamento a) {
            ejecutar(() -> sistema().eliminarApartamento(a.getCodigo()));
        }

        @Override
        String descripcion(Apartamento a) {
            return "el apartamento " + a.getCodigo() + " (se retira también del mapa)";
        }
    }

    // ==================================================================
    // Huéspedes
    // ==================================================================

    private class PestanaHuespedes extends PestanaCatalogo<Huesped> {

        private static final long serialVersionUID = 1L;

        PestanaHuespedes() {
            super(new String[] {"Documento", "Nombre", "Nacimiento", "Teléfono", "Correo",
                    "Reservas"}, "Nuevo huésped");
        }

        @Override
        ArbolBinarioBusqueda<Huesped> arbol() {
            return sistema().getGestorReservas().getCatalogoHuespedes();
        }

        @Override
        Object valor(Huesped h, int columna) {
            switch (columna) {
                case 0:
                    return h.getDocumento();
                case 1:
                    return h.getNombre();
                case 2:
                    return Formato.fecha(h.getFechaNacimiento());
                case 3:
                    return h.getTelefono() == null ? "" : h.getTelefono();
                case 4:
                    return h.getCorreo() == null ? "" : h.getCorreo();
                case 5:
                    return h.getHistorial().tamano();
                default:
                    return "";
            }
        }

        @Override
        void nuevo() {
            DialogoHuesped.mostrar(PanelAdministracion.this, contexto);
        }

        @Override
        void editar(Huesped h) {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Editar huésped",
                    Formularios.campos(Campo.fijo("Documento", h.getDocumento()),
                            Campo.texto("Nombre", h.getNombre()),
                            Campo.texto("Nacimiento (aaaa-mm-dd)",
                                    h.getFechaNacimiento() == null ? ""
                                            : h.getFechaNacimiento().toString()),
                            Campo.texto("Teléfono", h.getTelefono()),
                            Campo.texto("Correo", h.getCorreo())));
            if (datos == null) {
                return;
            }
            ejecutar(() -> {
                if (datos[1].isEmpty()) {
                    throw new IllegalArgumentException("El nombre del huésped es obligatorio.");
                }
                LocalDate nacimiento = fechaOpcional(datos[2]);
                h.setNombre(datos[1]);
                h.setFechaNacimiento(nacimiento);
                h.setTelefono(datos[3].isEmpty() ? null : datos[3]);
                h.setCorreo(datos[4].isEmpty() ? null : datos[4]);
            });
        }

        @Override
        void eliminar(Huesped h) {
            ejecutar(() -> sistema().eliminarHuesped(h.getDocumento()));
        }

        @Override
        String descripcion(Huesped h) {
            return "al huésped " + h.getNombre();
        }
    }

    // ==================================================================
    // Temporadas y tarifas
    // ==================================================================

    private class PestanaTemporadas extends JPanel implements Contexto.Actualizable {

        private static final long serialVersionUID = 1L;

        private final Modelos.ModeloTabla<CalendarioTemporadas.Periodo> modeloPeriodos =
                new Modelos.ModeloTabla<>(new String[] {"Temporada", "Desde", "Hasta", "Noches"},
                        (periodo, columna) -> valorPeriodo(periodo, columna), null);
        private final JTable tablaPeriodos = new JTable(modeloPeriodos);
        private final JLabel porDefecto = Estilo.etiquetaSuave(" ");

        private final Modelos.ModeloTabla<Apartamento> modeloTarifas = new Modelos.ModeloTabla<>(
                new String[] {"Apartamento", "Baja", "Media", "Alta"},
                (apartamento, columna) -> valorTarifa(apartamento, columna), null);
        private final JTable tablaTarifas = new JTable(modeloTarifas);

        PestanaTemporadas() {
            super(new GridLayout(1, 2, 14, 0));
            setOpaque(false);
            setBorder(Estilo.relleno(10));
            add(construirPeriodos());
            add(construirTarifas());
        }

        private JPanel construirPeriodos() {
            JPanel panel = Estilo.seccion("Fechas de cada temporada");
            tablaPeriodos.setRowHeight(26);
            tablaPeriodos.setFont(Estilo.pequena());
            tablaPeriodos.getTableHeader().setFont(Estilo.pequena());
            tablaPeriodos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            panel.add(new JScrollPane(tablaPeriodos), BorderLayout.CENTER);

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            barra.setOpaque(false);
            JButton agregar = Estilo.boton("Agregar periodo", Estilo.EXITO);
            agregar.addActionListener(evento -> agregarPeriodo());
            barra.add(agregar);
            JButton quitar = Estilo.boton("Quitar periodo", Estilo.PELIGRO);
            quitar.addActionListener(evento -> quitarPeriodo());
            barra.add(quitar);
            porDefecto.setFont(Estilo.pequena());
            JPanel sur = new JPanel(new BorderLayout());
            sur.setOpaque(false);
            sur.add(barra, BorderLayout.NORTH);
            sur.add(porDefecto, BorderLayout.SOUTH);
            panel.add(sur, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel construirTarifas() {
            JPanel panel = Estilo.seccion("Tarifa por persona y noche de cada apartamento");
            tablaTarifas.setRowHeight(26);
            tablaTarifas.setFont(Estilo.pequena());
            tablaTarifas.getTableHeader().setFont(Estilo.pequena());
            tablaTarifas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            panel.add(new JScrollPane(tablaTarifas), BorderLayout.CENTER);

            JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            barra.setOpaque(false);
            JButton editar = Estilo.boton("Editar tarifas", Estilo.PRIMARIO);
            editar.addActionListener(evento -> editarTarifas());
            barra.add(editar);
            panel.add(barra, BorderLayout.SOUTH);
            return panel;
        }

        private Object valorPeriodo(CalendarioTemporadas.Periodo periodo, int columna) {
            switch (columna) {
                case 0:
                    return periodo.getTemporada().getDescripcion();
                case 1:
                    return Formato.fecha(periodo.getInicio());
                case 2:
                    return Formato.fecha(periodo.getFin());
                case 3:
                    return java.time.temporal.ChronoUnit.DAYS.between(
                            periodo.getInicio(), periodo.getFin()) + 1;
                default:
                    return "";
            }
        }

        private Object valorTarifa(Apartamento apartamento, int columna) {
            switch (columna) {
                case 0:
                    return apartamento.getCodigo() + " · " + apartamento.getNombre();
                case 1:
                    return Formato.moneda(apartamento.precioPorPersona(Temporada.BAJA));
                case 2:
                    return Formato.moneda(apartamento.precioPorPersona(Temporada.MEDIA));
                case 3:
                    return Formato.moneda(apartamento.precioPorPersona(Temporada.ALTA));
                default:
                    return "";
            }
        }

        private void agregarPeriodo() {
            String[] datos = Formularios.pedir(PanelAdministracion.this, "Nuevo periodo de temporada",
                    Formularios.campos(
                            Campo.opciones("Temporada", Temporada.ALTA.getDescripcion(),
                                    Temporada.MEDIA.getDescripcion(), Temporada.ALTA.getDescripcion()),
                            Campo.texto("Desde (aaaa-mm-dd)", LocalDate.now().toString()),
                            Campo.texto("Hasta (aaaa-mm-dd)", LocalDate.now().plusDays(14).toString())));
            if (datos == null) {
                return;
            }
            try {
                Temporada temporada = null;
                for (Temporada candidata : Temporada.values()) {
                    if (candidata.getDescripcion().equalsIgnoreCase(datos[0])) {
                        temporada = candidata;
                    }
                }
                if (temporada == null) {
                    throw new IllegalArgumentException("Temporada no válida: " + datos[0]);
                }
                sistema().getServicioTemporadas().registrar(temporada,
                        LocalDate.parse(datos[1]), LocalDate.parse(datos[2]));
                contexto.notificar();
            } catch (DateTimeParseException excepcion) {
                error("Fecha inválida", "Use el formato aaaa-mm-dd.");
            } catch (IllegalArgumentException excepcion) {
                error("Periodo inválido", excepcion.getMessage());
            }
        }

        private void quitarPeriodo() {
            int fila = tablaPeriodos.getSelectedRow();
            if (fila < 0 || fila >= modeloPeriodos.getRowCount()) {
                JOptionPane.showMessageDialog(PanelAdministracion.this,
                        "Seleccione un periodo.", "Sin selección", JOptionPane.WARNING_MESSAGE);
                return;
            }
            sistema().getServicioTemporadas().eliminar(modeloPeriodos.getFila(fila));
            contexto.notificar();
        }

        private void editarTarifas() {
            int fila = tablaTarifas.getSelectedRow();
            if (fila < 0 || fila >= modeloTarifas.getRowCount()) {
                JOptionPane.showMessageDialog(PanelAdministracion.this,
                        "Seleccione un apartamento.", "Sin selección", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Apartamento a = modeloTarifas.getFila(fila);
            String[] datos = Formularios.pedir(PanelAdministracion.this,
                    "Tarifas de " + a.getCodigo(),
                    Formularios.campos(
                            Campo.texto("Baja", String.valueOf((long) a.precioPorPersona(Temporada.BAJA))),
                            Campo.texto("Media", String.valueOf((long) a.precioPorPersona(Temporada.MEDIA))),
                            Campo.texto("Alta", String.valueOf((long) a.precioPorPersona(Temporada.ALTA)))));
            if (datos == null) {
                return;
            }
            try {
                double baja = Double.parseDouble(datos[0]);
                double media = Double.parseDouble(datos[1]);
                double alta = Double.parseDouble(datos[2]);
                a.definirTarifa(Temporada.BAJA, baja);
                a.definirTarifa(Temporada.MEDIA, media);
                a.definirTarifa(Temporada.ALTA, alta);
                contexto.notificar();
            } catch (NumberFormatException excepcion) {
                error("Tarifa inválida", "Escriba valores numéricos, por ejemplo 150000.");
            } catch (IllegalArgumentException excepcion) {
                error("Tarifa inválida", excepcion.getMessage());
            }
        }

        @Override
        public void actualizar() {
            modeloPeriodos.setFilas(sistema().getServicioTemporadas().periodos());
            modeloTarifas.setFilas(sistema().todosApartamentos());
            porDefecto.setText("Las fechas fuera de estos periodos se liquidan en temporada "
                    + sistema().getCalendario().getTemporadaPorDefecto().getDescripcion() + ".");
        }
    }
}
