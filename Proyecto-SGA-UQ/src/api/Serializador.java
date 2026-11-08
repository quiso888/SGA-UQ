package api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import datastructures.Grafo;
import datastructures.ListaEnlazada;
import model.Apartamento;
import model.CalendarioTemporadas;
import model.CanalReserva;
import model.ConflictoCanal;
import model.Empleado;
import model.EstadoApartamento;
import model.EstadoReserva;
import model.Huesped;
import model.Liquidacion;
import model.Movimiento;
import model.Ocupante;
import model.Reserva;
import model.SolicitudExterna;
import model.TareaPreparacion;
import model.Temporada;
import service.GestorEdificio;
import service.GestorPreparacion;
import service.ServicioEstadisticas;
import service.ServicioLiquidacion;
import service.SistemaAlojamiento;

/**
 * Convierte el modelo de dominio en objetos JSON listos para la interfaz React.
 *
 * <p>No se usan bibliotecas de serialización: cada método arma explícitamente el
 * contrato que consume el frontend.</p>
 */
public final class Serializador {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private Serializador() {
    }

    // ------------------------------------------------------------------
    // Catálogos y estados
    // ------------------------------------------------------------------

    public static Json.Objeto estadoApartamento(EstadoApartamento estado) {
        return Json.objeto()
                .poner("clave", estado.name())
                .poner("descripcion", estado.getDescripcion())
                .poner("color", String.format("#%06X", estado.getColorRgb()))
                .poner("admiteHuespedes", estado.admiteHuespedes())
                .poner("requierePreparacion", estado.requierePreparacion());
    }

    public static Json.Objeto estadoReserva(EstadoReserva estado) {
        return Json.objeto()
                .poner("clave", estado.name())
                .poner("descripcion", estado.getDescripcion())
                .poner("activa", estado.estaActiva())
                .poner("cerrada", estado.estaCerrada());
    }

    public static Json.Objeto canal(CanalReserva canal) {
        return Json.objeto()
                .poner("clave", canal.name())
                .poner("descripcion", canal.getDescripcion());
    }

    public static Json.Objeto temporada(Temporada temporada) {
        return Json.objeto()
                .poner("clave", temporada.name())
                .poner("descripcion", temporada.getDescripcion());
    }

    // ------------------------------------------------------------------
    // Apartamentos
    // ------------------------------------------------------------------

    public static Json.Objeto apartamento(Apartamento apartamento) {
        Json.Objeto objeto = Json.objeto()
                .poner("codigo", apartamento.getCodigo())
                .poner("nombre", apartamento.getNombre())
                .poner("bloque", apartamento.getBloque())
                .poner("piso", apartamento.getPiso())
                .poner("dormitorios", apartamento.getNumeroDormitorios())
                .poner("capacidad", apartamento.getCapacidadMaxima())
                .poner("estado", estadoApartamento(apartamento.getEstado()))
                .poner("motivo", apartamento.getMotivoFueraDeServicio())
                .poner("posicion", Json.objeto()
                        .poner("x", apartamento.getPosicionX())
                        .poner("y", apartamento.getPosicionY()))
                .poner("reservasRegistradas", apartamento.numeroReservas());

        Json.Arreglo caracteristicas = Json.arreglo();
        for (String caracteristica : apartamento.getCaracteristicas()) {
            caracteristicas.agregar(caracteristica);
        }
        objeto.poner("caracteristicas", caracteristicas);

        Json.Objeto tarifas = Json.objeto();
        for (Temporada temporada : Temporada.values()) {
            tarifas.poner(temporada.name(), apartamento.precioPorPersona(temporada));
        }
        objeto.poner("tarifas", tarifas);
        return objeto;
    }

    // ------------------------------------------------------------------
    // Reservas
    // ------------------------------------------------------------------

    public static Json.Objeto resumenReserva(Reserva reserva) {
        return Json.objeto()
                .poner("codigo", reserva.getCodigo())
                .poner("titular", reserva.getTitular().getNombre())
                .poner("documento", reserva.getTitular().getDocumento())
                .poner("apartamento", reserva.getApartamento().getCodigo())
                .poner("canal", canal(reserva.getCanal()))
                .poner("estado", estadoReserva(reserva.getEstado()))
                .poner("entrada", fecha(reserva.getEstancia().getFechaEntrada()))
                .poner("salida", fecha(reserva.getEstancia().getFechaSalida()))
                .poner("noches", reserva.getEstancia().numeroNoches())
                .poner("personas", reserva.numeroPersonas())
                .poner("total", reserva.getTotalEstimado());
    }

    public static Json.Objeto reserva(Reserva reserva, SistemaAlojamiento sistema) {
        Json.Objeto objeto = Json.objeto()
                .poner("codigo", reserva.getCodigo())
                .poner("titular", Json.objeto()
                        .poner("documento", reserva.getTitular().getDocumento())
                        .poner("nombre", reserva.getTitular().getNombre())
                        .poner("telefono", reserva.getTitular().getTelefono())
                        .poner("correo", reserva.getTitular().getCorreo()))
                .poner("apartamento", Json.objeto()
                        .poner("codigo", reserva.getApartamento().getCodigo())
                        .poner("nombre", reserva.getApartamento().getNombre())
                        .poner("estado", estadoApartamento(reserva.getApartamento().getEstado())))
                .poner("canal", canal(reserva.getCanal()))
                .poner("estado", estadoReserva(reserva.getEstado()))
                .poner("entrada", fecha(reserva.getEstancia().getFechaEntrada()))
                .poner("salida", fecha(reserva.getEstancia().getFechaSalida()))
                .poner("noches", reserva.getEstancia().numeroNoches())
                .poner("hora", reserva.getHoraEstimadaLlegada().toString())
                .poner("personas", reserva.numeroPersonas())
                .poner("total", reserva.getTotalEstimado())
                .poner("anticipoPagado", reserva.getAnticipoPagado())
                .poner("anticipoRequerido", reserva.anticipoRequerido())
                .poner("saldo", reserva.getFolio().saldo())
                .poner("fechaCreacion", reserva.getFechaCreacion().toString())
                .poner("fechaVencimiento", reserva.getFechaVencimiento().toString())
                .poner("motivoCierre", reserva.getMotivoCierre())
                .poner("devolucionEstimada", devolucionEstimada(reserva));

        Json.Arreglo ocupantes = Json.arreglo();
        for (Ocupante ocupante : reserva.getOcupantes()) {
            ocupantes.agregar(Json.objeto()
                    .poner("nombre", ocupante.getNombre())
                    .poner("fechaNacimiento", ocupante.getFechaNacimiento() == null
                            ? null : fecha(ocupante.getFechaNacimiento()))
                    .poner("titular", ocupante.esTitular())
                    .poner("edad", ocupante.getFechaNacimiento() == null
                            ? 0 : ocupante.edadEn(LocalDate.now()))
                    .poner("pagaHoy", ocupante.pagaEn(LocalDate.now())));
        }
        objeto.poner("ocupantes", ocupantes);
        objeto.poner("folio", folio(reserva));
        objeto.poner("liquidacion", liquidacion(reserva, sistema));
        objeto.poner("acciones", acciones(reserva));
        return objeto;
    }

    private static Json.Objeto folio(Reserva reserva) {
        Json.Arreglo movimientos = Json.arreglo();
        for (Movimiento movimiento : reserva.getFolio().getMovimientos()) {
            movimientos.agregar(movimiento(movimiento));
        }
        return Json.objeto()
                .poner("codigo", reserva.getFolio().getCodigo())
                .poner("saldo", reserva.getFolio().saldo())
                .poner("totalCargos", reserva.getFolio().totalCargos())
                .poner("totalPagos", reserva.getFolio().totalPagos())
                .poner("saldado", reserva.getFolio().estaSaldado())
                .poner("operacionesPorDeshacer", reserva.getFolio().getPilaOperaciones().tamano())
                .poner("movimientos", movimientos);
    }

    public static Json.Objeto movimiento(Movimiento movimiento) {
        return Json.objeto()
                .poner("tipo", movimiento.getTipo().name())
                .poner("tipoDescripcion", movimiento.getTipo().getDescripcion())
                .poner("concepto", movimiento.getConcepto())
                .poner("valor", movimiento.getValor())
                .poner("fecha", movimiento.getFecha().toString())
                .poner("referencia", movimiento.getReferencia())
                .poner("efecto", movimiento.efectoEnSaldo());
    }

    private static Json.Objeto liquidacion(Reserva reserva, SistemaAlojamiento sistema) {
        ServicioLiquidacion servicio = new ServicioLiquidacion(sistema.getCalendario());
        Liquidacion liquidacion = servicio.liquidar(reserva.getApartamento(),
                reserva.getEstancia(), reserva.getOcupantes());
        Json.Arreglo noches = Json.arreglo();
        for (Liquidacion.DetalleNoche noche : liquidacion.getNoches()) {
            noches.agregar(Json.objeto()
                    .poner("fecha", fecha(noche.getFecha()))
                    .poner("temporada", temporada(noche.getTemporada()))
                    .poner("personas", noche.getPersonasQuePagan())
                    .poner("tarifa", noche.getTarifaPorPersona())
                    .poner("valor", noche.getValor()));
        }
        return Json.objeto()
                .poner("codigo", liquidacion.getCodigoReserva())
                .poner("total", liquidacion.getTotal())
                .poner("noches", noches);
    }

    private static Json.Objeto acciones(Reserva reserva) {
        boolean pendiente = reserva.getEstado() == EstadoReserva.PENDIENTE;
        boolean enCurso = reserva.getEstado() == EstadoReserva.EN_CURSO;
        boolean cerrada = reserva.getEstado().estaCerrada();
        return Json.objeto()
                .poner("anticipo", pendiente)
                .poner("checkIn", reserva.getEstado() == EstadoReserva.CONFIRMADA
                        && reserva.getApartamento().getEstado() == EstadoApartamento.PREPARADO)
                .poner("checkOut", enCurso)
                .poner("cancelar", !cerrada)
                .poner("noShow", reserva.getEstado() == EstadoReserva.CONFIRMADA
                        || pendiente)
                .poner("movimientos", !reserva.getEstado().estaCerrada()
                        || reserva.getFolio().getMovimientos().tamano() > 0)
                .poner("deshacer", !reserva.getFolio().getPilaOperaciones().estaVacia())
                .poner("procesarVencimientos", pendiente);
    }

    public static double devolucionEstimada(Reserva reserva) {
        if (reserva.getAnticipoPagado() <= 0) {
            return 0;
        }
        return LocalDate.now().isBefore(reserva.getEstancia().getFechaEntrada())
                ? reserva.getAnticipoPagado() : 0;
    }

    // ------------------------------------------------------------------
    // Huéspedes
    // ------------------------------------------------------------------

    public static Json.Objeto huesped(Huesped huesped) {
        Json.Arreglo favoritos = Json.arreglo();
        for (String codigo : huesped.getFavoritos()) {
            favoritos.agregar(codigo);
        }

        Json.Arreglo historial = Json.arreglo();
        int noches = 0;
        double gasto = 0;
        for (Reserva reserva : huesped.getHistorial()) {
            historial.agregar(resumenReserva(reserva));
            noches += reserva.getEstancia().numeroNoches();
            if (reserva.getEstado() != EstadoReserva.CANCELADA
                    && reserva.getEstado() != EstadoReserva.NO_SHOW) {
                gasto += reserva.getTotalEstimado();
            }
        }

        return Json.objeto()
                .poner("documento", huesped.getDocumento())
                .poner("nombre", huesped.getNombre())
                .poner("fechaNacimiento", huesped.getFechaNacimiento() == null
                        ? null : fecha(huesped.getFechaNacimiento()))
                .poner("telefono", huesped.getTelefono())
                .poner("correo", huesped.getCorreo())
                .poner("favoritos", favoritos)
                .poner("historial", historial)
                .poner("totalReservas", huesped.getHistorial().tamano())
                .poner("totalNoches", noches)
                .poner("totalGastado", gasto);
    }

    // ------------------------------------------------------------------
    // Tablero
    // ------------------------------------------------------------------

    public static Json.Objeto tablero(SistemaAlojamiento sistema) {
        Json.Arreglo apartamentos = Json.arreglo();
        for (Apartamento apartamento : sistema.todosApartamentos()) {
            Json.Objeto item = apartamento(apartamento);
            Reserva proxima = proximaReserva(apartamento, LocalDate.now());
            item.poner("proximaReserva", proxima == null ? null : resumenReserva(proxima));
            apartamentos.agregar(item);
        }

        int[] conteo = sistema.getServicioEstadisticas().apartamentosPorEstado();
        Json.Objeto resumenEstados = Json.objeto();
        for (EstadoApartamento estado : EstadoApartamento.values()) {
            resumenEstados.poner(estado.name(), conteo[estado.ordinal()]);
        }

        Empleado turno = sistema.recepcionistaDeTurno();
        Json.Objeto hoy = Json.objeto()
                .poner("fecha", fecha(LocalDate.now()))
                .poner("temporada", temporada(sistema.getServicioTemporadas().temporadaDe(LocalDate.now())));

        return Json.objeto()
                .poner("hoy", hoy)
                .poner("turno", turno == null ? null : empleado(turno))
                .poner("apartamentos", apartamentos)
                .poner("conteoEstados", resumenEstados)
                .poner("totales", totales(sistema))
                .poner("leyenda", leyendaApartamentos());
    }

    private static Reserva proximaReserva(Apartamento apartamento, LocalDate hoy) {
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

    private static Json.Arreglo leyendaApartamentos() {
        Json.Arreglo leyenda = Json.arreglo();
        for (EstadoApartamento estado : EstadoApartamento.values()) {
            if (estado == EstadoApartamento.EN_PREPARACION) {
                continue;
            }
            leyenda.agregar(estadoApartamento(estado));
        }
        return leyenda;
    }

    public static Json.Objeto empleado(Empleado empleado) {
        return Json.objeto()
                .poner("documento", empleado.getDocumento())
                .poner("nombre", empleado.getNombre())
                .poner("cargo", empleado.getCargo().name())
                .poner("cargoDescripcion", empleado.getCargo().getDescripcion())
                .poner("activo", empleado.isActivo());
    }

    public static Json.Objeto totales(SistemaAlojamiento sistema) {
        return Json.objeto()
                .poner("apartamentos", sistema.todosApartamentos().tamano())
                .poner("huespedes", sistema.todosHuespedes().tamano())
                .poner("reservas", sistema.todasReservas().tamano())
                .poner("preparacionesPendientes",
                        sistema.getGestorPreparacion().numeroTareasPendientes())
                .poner("solicitudesPendientes",
                        sistema.getProcesadorCanalExterno().numeroSolicitudesPendientes())
                .poner("conflictosCanal",
                        sistema.getProcesadorCanalExterno().numeroConflictos());
    }

    // ------------------------------------------------------------------
    // Calendario
    // ------------------------------------------------------------------

    public static Json.Objeto calendario(SistemaAlojamiento sistema, LocalDate inicio, int dias) {
        Json.Arreglo fechas = Json.arreglo();
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = inicio.plusDays(i);
            fechas.agregar(Json.objeto()
                    .poner("fecha", fecha(fecha))
                    .poner("temporada", temporada(sistema.getServicioTemporadas().temporadaDe(fecha))));
        }

        Json.Arreglo apartamentos = Json.arreglo();
        for (Apartamento apartamento : sistema.todosApartamentos()) {
            Json.Arreglo celdas = Json.arreglo();
            for (int i = 0; i < dias; i++) {
                LocalDate fecha = inicio.plusDays(i);
                Reserva ocupante = reservaQueOcupa(apartamento, fecha);
                celdas.agregar(Json.objeto()
                        .poner("fecha", fecha(fecha))
                        .poner("reserva", ocupante == null ? null : resumenReserva(ocupante))
                        .poner("fueraServicio",
                                apartamento.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO));
            }
            apartamentos.agregar(Json.objeto()
                    .poner("codigo", apartamento.getCodigo())
                    .poner("nombre", apartamento.getNombre())
                    .poner("bloque", apartamento.getBloque())
                    .poner("estado", estadoApartamento(apartamento.getEstado()))
                    .poner("celdas", celdas));
        }

        return Json.objeto()
                .poner("inicio", fecha(inicio))
                .poner("dias", dias)
                .poner("fin", fecha(inicio.plusDays(dias - 1)))
                .poner("fechas", fechas)
                .poner("apartamentos", apartamentos);
    }

    private static Reserva reservaQueOcupa(Apartamento apartamento, LocalDate fecha) {
        Reserva elegida = null;
        for (Reserva reserva : apartamento.getReservas()) {
            if (!reserva.estaVigente() || !reserva.getEstancia().ocupaNoche(fecha)) {
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

    // ------------------------------------------------------------------
    // Preparación
    // ------------------------------------------------------------------

    public static Json.Objeto preparacion(SistemaAlojamiento sistema) {
        GestorPreparacion gestor = sistema.getGestorPreparacion();
        GestorEdificio edificio = sistema.getGestorEdificio();

        Json.Arreglo tareas = Json.arreglo();
        for (TareaPreparacion tarea : gestor.tareasPendientes()) {
            tareas.agregar(tarea(tarea));
        }

        Json.Arreglo ruta = Json.arreglo();
        String actual = GestorEdificio.RECEPCION;
        int distancia = 0;
        int orden = 1;
        for (Apartamento apartamento : gestor.rutaDeLimpieza()) {
            TareaPreparacion tarea = buscarTarea(gestor, apartamento);
            Grafo.Ruta<String> tramo = edificio.ruta(actual, apartamento.getCodigo());
            Json.Objeto item = Json.objeto()
                    .poner("orden", orden++)
                    .poner("codigo", apartamento.getCodigo())
                    .poner("nombre", apartamento.getNombre())
                    .poner("estado", estadoApartamento(apartamento.getEstado()))
                    .poner("prioridad", tarea == null ? 2 : tarea.getPrioridad())
                    .poner("entradaHoy", tarea != null && tarea.isEntradaHoy())
                    .poner("hora", tarea == null ? null
                            : tarea.getHoraEstimadaLlegada().toString())
                    .poner("reservaEntrante", tarea == null ? null : tarea.getReservaEntrante())
                    .poner("desde", actual)
                    .poner("distancia", tramo.esAlcanzable() ? tramo.getDistancia() : null)
                    .poner("alcanzable", tramo.esAlcanzable())
                    .poner("tramo", etiquetas(tramo));
            ruta.agregar(item);
            if (tramo.esAlcanzable()) {
                distancia += tramo.getDistancia();
                actual = apartamento.getCodigo();
            }
        }

        Json.Arreglo historial = Json.arreglo();
        for (TareaPreparacion tarea : gestor.historial()) {
            historial.agregar(tarea(tarea)
                    .poner("duracionSegundos", tarea.duracionSegundos()));
        }

        Json.Objeto resumen = Json.objeto()
                .poner("pendientes", gestor.numeroTareasPendientes())
                .poner("conEntradaHoy", gestor.tareasConEntradaHoy().tamano())
                .poner("completadas", gestor.historial().tamano())
                .poner("promedioMinutos", gestor.tiempoPromedioPreparacionMinutos())
                .poner("distanciaRuta", distancia)
                .poner("apartamentosFueraDeServicio",
                        sistema.getServicioEstadisticas().apartamentosPorEstado()
                                [EstadoApartamento.FUERA_DE_SERVICIO.ordinal()]);

        Json.Arreglo instalaciones = Json.arreglo();
        instalaciones.agregar(Json.objeto().poner("codigo", GestorEdificio.RECEPCION)
                .poner("nombre", "Recepción"));
        instalaciones.agregar(Json.objeto().poner("codigo", "SERV-ASEO")
                .poner("nombre", "Cuarto de aseo"));
        for (String bloque : new String[] {"A", "B", "C"}) {
            instalaciones.agregar(Json.objeto().poner("codigo", "SERV-ESC-" + bloque)
                    .poner("nombre", "Escaleras bloque " + bloque));
        }

        return Json.objeto()
                .poner("tareas", tareas)
                .poner("ruta", ruta)
                .poner("historial", historial)
                .poner("resumen", resumen)
                .poner("puntosPartida", instalaciones)
                .poner("prioridad", Json.objeto()
                        .poner("uno", "Entrada hoy · ordenadas por hora de llegada")
                        .poner("dos", "Resto de apartamentos pendientes"));
    }

    private static TareaPreparacion buscarTarea(GestorPreparacion gestor, Apartamento apartamento) {
        try {
            return gestor.buscarTarea(apartamento);
        } catch (RuntimeException excepcion) {
            return null;
        }
    }

    public static Json.Objeto tarea(TareaPreparacion tarea) {
        return Json.objeto()
                .poner("codigo", tarea.getApartamento().getCodigo())
                .poner("nombre", tarea.getApartamento().getNombre())
                .poner("estado", estadoApartamento(tarea.getApartamento().getEstado()))
                .poner("prioridad", tarea.getPrioridad())
                .poner("entradaHoy", tarea.isEntradaHoy())
                .poner("hora", tarea.getHoraEstimadaLlegada().toString())
                .poner("reservaEntrante", tarea.getReservaEntrante())
                .poner("desde", tarea.getDesdeCuandoPendiente().toString())
                .poner("inicio", tarea.getInicioPreparacion() == null
                        ? null : tarea.getInicioPreparacion().toString())
                .poner("fin", tarea.getFinPreparacion() == null
                        ? null : tarea.getFinPreparacion().toString())
                .poner("duracionSegundos", tarea.duracionSegundos());
    }

    // ------------------------------------------------------------------
    // Mapa
    // ------------------------------------------------------------------

    public static Json.Objeto mapa(SistemaAlojamiento sistema) {
        GestorEdificio edificio = sistema.getGestorEdificio();
        Grafo<String> grafo = edificio.getGrafo();

        Json.Arreglo nodos = Json.arreglo();
        for (Grafo.Nodo<String> nodo : grafo) {
            String etiqueta = nodo.getEtiqueta();
            Json.Objeto item = Json.objeto()
                    .poner("id", etiqueta)
                    .poner("x", nodo.getX())
                    .poner("y", nodo.getY())
                    .poner("tipo", tipoNodo(etiqueta));

            Apartamento apartamento = buscarApartamentoOpcional(sistema, etiqueta);
            if (apartamento != null) {
                item.poner("nombre", apartamento.getNombre())
                        .poner("bloque", apartamento.getBloque())
                        .poner("piso", apartamento.getPiso())
                        .poner("capacidad", apartamento.getCapacidadMaxima())
                        .poner("estado", estadoApartamento(apartamento.getEstado()));
            } else {
                item.poner("nombre", nombreInstalacion(etiqueta));
            }
            nodos.agregar(item);
        }

        Json.Arreglo aristas = Json.arreglo();
        for (Grafo.Arista<String> arista : grafo.getAristas()) {
            aristas.agregar(Json.objeto()
                    .poner("origen", arista.getOrigen().getEtiqueta())
                    .poner("destino", arista.getDestino().getEtiqueta())
                    .poner("peso", arista.getPeso())
                    .poner("activa", arista.isActiva()));
        }

        return Json.objeto()
                .poner("nodos", nodos)
                .poner("aristas", aristas)
                .poner("analisis", analisis(sistema))
                .poner("recepcion", GestorEdificio.RECEPCION);
    }

    private static Json.Objeto analisis(SistemaAlojamiento sistema) {
        GestorEdificio edificio = sistema.getGestorEdificio();
        Grafo<String> grafo = edificio.getGrafo();

        Json.Arreglo bloques = Json.arreglo();
        for (GestorEdificio.Bloque bloque : edificio.apartamentosPorBloque()) {
            Json.Arreglo codigos = Json.arreglo();
            for (Apartamento apartamento : bloque.getApartamentos()) {
                codigos.agregar(apartamento.getCodigo());
            }
            bloques.agregar(Json.objeto()
                    .poner("nombre", bloque.getNombre())
                    .poner("cantidad", bloque.cantidad())
                    .poner("apartamentos", codigos));
        }

        Json.Arreglo inalcanzables = Json.arreglo();
        for (Apartamento apartamento : edificio.apartamentosInalcanzables()) {
            inalcanzables.agregar(apartamento.getCodigo());
        }

        Json.Arreglo componentes = Json.arreglo();
        for (ListaEnlazada<Grafo.Nodo<String>> componente : edificio.componentesConexas()) {
            Json.Arreglo etiquetas = Json.arreglo();
            for (Grafo.Nodo<String> nodo : componente) {
                etiquetas.agregar(nodo.getEtiqueta());
            }
            componentes.agregar(etiquetas);
        }

        return Json.objeto()
                .poner("nodos", edificio.numeroNodos())
                .poner("pasillos", edificio.numeroPasillos())
                .poner("tieneCiclo", edificio.tieneCiclo())
                .poner("componentes", componentes)
                .poner("bloques", bloques)
                .poner("inalcanzables", inalcanzables);
    }

    private static Apartamento buscarApartamentoOpcional(SistemaAlojamiento sistema, String codigo) {
        try {
            return sistema.buscarApartamento(codigo);
        } catch (RuntimeException excepcion) {
            return null;
        }
    }

    private static String tipoNodo(String etiqueta) {
        if (GestorEdificio.RECEPCION.equals(etiqueta)) {
            return "recepcion";
        }
        if (etiqueta.startsWith("SERV-ESC")) {
            return "escalera";
        }
        if (etiqueta.startsWith("SERV-ASEO")) {
            return "aseo";
        }
        if (etiqueta.startsWith(GestorEdificio.PREFIJO_INSTALACION)) {
            return "instalacion";
        }
        return "apartamento";
    }

    private static String nombreInstalacion(String etiqueta) {
        if (GestorEdificio.RECEPCION.equals(etiqueta)) {
            return "Recepción";
        }
        if (etiqueta.startsWith("SERV-ESC")) {
            return "Escaleras " + etiqueta.substring(etiqueta.length() - 1);
        }
        if (etiqueta.startsWith("SERV-ASEO")) {
            return "Cuarto de aseo";
        }
        return etiqueta;
    }

    public static Json.Objeto ruta(Grafo.Ruta<String> ruta) {
        return Json.objeto()
                .poner("alcanzable", ruta.esAlcanzable())
                .poner("distancia", ruta.esAlcanzable() ? ruta.getDistancia() : null)
                .poner("camino", etiquetas(ruta));
    }

    private static Json.Arreglo etiquetas(Grafo.Ruta<String> ruta) {
        Json.Arreglo etiquetas = Json.arreglo();
        for (String etiqueta : ruta.getEtiquetas()) {
            etiquetas.agregar(etiqueta);
        }
        return etiquetas;
    }

    // ------------------------------------------------------------------
    // Estadísticas
    // ------------------------------------------------------------------

    public static Json.Objeto estadisticas(SistemaAlojamiento sistema) {
        ServicioEstadisticas servicio = sistema.getServicioEstadisticas();

        Json.Objeto indicadores = Json.objeto()
                .poner("ingresoTotal", servicio.ingresoTotal())
                .poner("ingresoPromedio", servicio.ingresoPromedio())
                .poner("ocupacionHoy", servicio.porcentajeOcupacion(LocalDate.now()))
                .poner("totalReservas", servicio.totalReservas())
                .poner("tasaCancelacion", servicio.tasaCancelacion())
                .poner("tasaNoShow", servicio.tasaNoShow())
                .poner("conflictosCanal", servicio.numeroConflictosCanal())
                .poner("preparacionPromedio", servicio.tiempoMedioPreparacionMinutos())
                .poner("apartamentosPorPreparar", servicio.apartamentosPorPreparar());

        int[] reservasEstado = servicio.reservasPorEstado();
        Json.Arreglo reservasPorEstado = Json.arreglo();
        for (EstadoReserva estado : EstadoReserva.values()) {
            reservasPorEstado.agregar(barra(estado.getDescripcion(),
                    reservasEstado[estado.ordinal()]));
        }

        int[] reservasCanal = servicio.reservasPorCanal();
        Json.Arreglo reservasPorCanal = Json.arreglo();
        for (CanalReserva canal : CanalReserva.values()) {
            reservasPorCanal.agregar(barra(canal.getDescripcion(),
                    reservasCanal[canal.ordinal()]));
        }

        double[] ingresosCanal = servicio.ingresosPorCanal();
        Json.Arreglo ingresosPorCanal = Json.arreglo();
        for (CanalReserva canal : CanalReserva.values()) {
            ingresosPorCanal.agregar(barra(canal.getDescripcion(),
                    ingresosCanal[canal.ordinal()]));
        }

        double[] ingresosTemporada = servicio.ingresosPorTemporada();
        Json.Arreglo ingresosPorTemporada = Json.arreglo();
        for (Temporada temporada : Temporada.values()) {
            ingresosPorTemporada.agregar(barra(temporada.getDescripcion(),
                    ingresosTemporada[temporada.ordinal()]));
        }

        int[] apartamentosEstado = servicio.apartamentosPorEstado();
        Json.Arreglo apartamentosPorEstado = Json.arreglo();
        for (EstadoApartamento estado : EstadoApartamento.values()) {
            apartamentosPorEstado.agregar(barra(estado.getDescripcion(),
                    apartamentosEstado[estado.ordinal()]));
        }

        int[] noches = servicio.nochesReservadasPorApartamento();
        Json.Arreglo nochesPorApartamento = Json.arreglo();
        int indice = 0;
        for (Apartamento apartamento : sistema.todosApartamentos()) {
            if (indice >= noches.length) {
                break;
            }
            nochesPorApartamento.agregar(barra(apartamento.getCodigo(), noches[indice++]));
        }

        return Json.objeto()
                .poner("indicadores", indicadores)
                .poner("series", Json.objeto()
                        .poner("reservasEstado", reservasPorEstado)
                        .poner("reservasCanal", reservasPorCanal)
                        .poner("ingresosCanal", ingresosPorCanal)
                        .poner("ingresosTemporada", ingresosPorTemporada)
                        .poner("apartamentosEstado", apartamentosPorEstado)
                        .poner("nochesPorApartamento", nochesPorApartamento));
    }

    private static Json.Objeto barra(String etiqueta, double valor) {
        return Json.objeto().poner("etiqueta", etiqueta).poner("valor", valor);
    }

    // ------------------------------------------------------------------
    // Canal externo
    // ------------------------------------------------------------------

    public static Json.Objeto canalExterno(SistemaAlojamiento sistema) {
        Json.Arreglo pendientes = Json.arreglo();
        for (SolicitudExterna solicitud : sistema.getProcesadorCanalExterno()
                .solicitudesPendientes()) {
            pendientes.agregar(solicitud(solicitud));
        }

        Json.Arreglo procesadas = Json.arreglo();
        for (SolicitudExterna solicitud : sistema.getProcesadorCanalExterno().getProcesadas()) {
            procesadas.agregar(solicitud(solicitud));
        }

        Json.Arreglo conflictos = Json.arreglo();
        for (ConflictoCanal conflicto : sistema.getProcesadorCanalExterno().getConflictos()) {
            conflictos.agregar(Json.objeto()
                    .poner("solicitud", conflicto.getIdentificadorSolicitud())
                    .poner("apartamento", conflicto.getCodigoApartamento())
                    .poner("motivo", conflicto.getMotivo())
                    .poner("reservaEnConflicto", conflicto.getReservaEnConflicto())
                    .poner("fecha", conflicto.getFecha().toString()));
        }

        int aceptadas = 0;
        int rechazadas = 0;
        for (SolicitudExterna solicitud : sistema.getProcesadorCanalExterno().getProcesadas()) {
            if (solicitud.getEstado() == SolicitudExterna.EstadoSolicitud.ACEPTADA) {
                aceptadas++;
            } else if (solicitud.getEstado() == SolicitudExterna.EstadoSolicitud.RECHAZADA) {
                rechazadas++;
            }
        }

        return Json.objeto()
                .poner("pendientes", pendientes)
                .poner("procesadas", procesadas)
                .poner("conflictos", conflictos)
                .poner("resumen", Json.objeto()
                        .poner("pendientes", pendientes.tamano())
                        .poner("aceptadas", aceptadas)
                        .poner("rechazadas", rechazadas)
                        .poner("conflictos", conflictos.tamano()));
    }

    public static Json.Objeto solicitud(SolicitudExterna solicitud) {
        return Json.objeto()
                .poner("identificador", solicitud.getIdentificador())
                .poner("nombre", solicitud.getNombreSolicitante())
                .poner("documento", solicitud.getDocumento())
                .poner("correo", solicitud.getCorreo())
                .poner("telefono", solicitud.getTelefono())
                .poner("apartamento", solicitud.getCodigoApartamento())
                .poner("entrada", fecha(solicitud.getFechaEntrada()))
                .poner("salida", fecha(solicitud.getFechaSalida()))
                .poner("personas", solicitud.getNumeroPersonas())
                .poner("recibida", solicitud.getFechaRecepcion().toString())
                .poner("estado", Json.objeto()
                        .poner("clave", solicitud.getEstado().name())
                        .poner("descripcion", solicitud.getEstado().getDescripcion()))
                .poner("motivo", solicitud.getMotivo());
    }

    // ------------------------------------------------------------------
    // Temporadas y cotizaciones
    // ------------------------------------------------------------------

    public static Json.Objeto temporadas(SistemaAlojamiento sistema) {
        Json.Arreglo periodos = Json.arreglo();
        for (CalendarioTemporadas.Periodo periodo : sistema.getServicioTemporadas().periodos()) {
            periodos.agregar(Json.objeto()
                    .poner("temporada", temporada(periodo.getTemporada()))
                    .poner("inicio", fecha(periodo.getInicio()))
                    .poner("fin", fecha(periodo.getFin())));
        }

        Json.Arreglo apartamentos = Json.arreglo();
        for (Apartamento apartamento : sistema.todosApartamentos()) {
            Json.Objeto tarifas = Json.objeto();
            for (Temporada temporada : Temporada.values()) {
                tarifas.poner(temporada.name(), apartamento.precioPorPersona(temporada));
            }
            apartamentos.agregar(Json.objeto()
                    .poner("codigo", apartamento.getCodigo())
                    .poner("nombre", apartamento.getNombre())
                    .poner("tarifas", tarifas));
        }

        return Json.objeto()
                .poner("hoy", fecha(LocalDate.now()))
                .poner("temporadaHoy", temporada(
                        sistema.getServicioTemporadas().temporadaDe(LocalDate.now())))
                .poner("porDefecto", temporada(
                        sistema.getCalendario().getTemporadaPorDefecto()))
                .poner("periodos", periodos)
                .poner("apartamentos", apartamentos);
    }

    public static Json.Objeto cotizacion(SistemaAlojamiento sistema, Apartamento apartamento,
                                         LocalDate entrada, LocalDate salida, int personas) {
        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        for (int i = 0; i < Math.max(1, personas); i++) {
            ocupantes.agregar(new Ocupante("Huésped " + (i + 1),
                    LocalDate.now().minusYears(30)));
        }
        ServicioLiquidacion servicio = new ServicioLiquidacion(sistema.getCalendario());
        Liquidacion liquidacion = servicio.liquidar(apartamento,
                new model.Estancia(entrada, salida), ocupantes);
        Json.Arreglo noches = Json.arreglo();
        for (Liquidacion.DetalleNoche noche : liquidacion.getNoches()) {
            noches.agregar(Json.objeto()
                    .poner("fecha", fecha(noche.getFecha()))
                    .poner("temporada", temporada(noche.getTemporada()))
                    .poner("personas", noche.getPersonasQuePagan())
                    .poner("tarifa", noche.getTarifaPorPersona())
                    .poner("valor", noche.getValor()));
        }
        return Json.objeto()
                .poner("apartamento", apartamento.getCodigo())
                .poner("entrada", fecha(entrada))
                .poner("salida", fecha(salida))
                .poner("noches", liquidacion.numeroNoches())
                .poner("total", liquidacion.getTotal())
                .poner("detalle", noches);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    public static String fecha(LocalDate fecha) {
        return fecha == null ? null : FECHA.format(fecha);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.toString();
    }
}
