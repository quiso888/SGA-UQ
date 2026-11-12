package api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.sun.net.httpserver.HttpExchange;

import datastructures.Grafo;
import datastructures.ListaEnlazada;
import exceptions.SgaException;
import model.Apartamento;
import model.CanalReserva;
import model.Empleado;
import model.EstadoApartamento;
import model.Estancia;
import model.Huesped;
import model.Ocupante;
import model.Reserva;
import model.SolicitudExterna;
import model.Temporada;
import service.GestorEdificio;
import service.SistemaAlojamiento;
import util.CargadorDatos;
import util.EscenarioPrueba;

/**
 * Controlador HTTP de la API del SGA.
 *
 * Expone el mismo dominio que usaba la interfaz Swing mediante recursos JSON.
 * Todas las peticiones se atienden de forma serializada porque el núcleo del
 * sistema (árboles, colas y grafos) no está diseñado para acceso concurrente
 */
public final class ApiSga {

    private SistemaAlojamiento sistema;

    public ApiSga(SistemaAlojamiento sistema) {
        this.sistema = sistema == null ? new SistemaAlojamiento() : sistema;
    }

    public synchronized SistemaAlojamiento getSistema() {
        return sistema;
    }

    // ------------------------------------------------------------------
    // Despacho
    // ------------------------------------------------------------------

    public synchronized void manejar(HttpExchange intercambio) throws IOException {
        String metodo = intercambio.getRequestMethod();
        try {
            if ("OPTIONS".equalsIgnoreCase(metodo)) {
                responderVacio(intercambio, 204);
                return;
            }

            String[] partes = segmentos(intercambio.getRequestURI());
            if (partes.length < 2 || !"api".equals(partes[0])) {
                responderError(intercambio, 404, "Recurso no encontrado.");
                return;
            }

            String recurso = partes[1];
            if ("GET".equalsIgnoreCase(metodo)) {
                atenderGet(intercambio, recurso, partes);
            } else if ("POST".equalsIgnoreCase(metodo)) {
                atenderPost(intercambio, recurso, partes);
            } else if ("PUT".equalsIgnoreCase(metodo)) {
                atenderPut(intercambio, recurso, partes);
            } else if ("PATCH".equalsIgnoreCase(metodo)) {
                atenderPatch(intercambio, recurso, partes);
            } else if ("DELETE".equalsIgnoreCase(metodo)) {
                responderError(intercambio, 405, "Método no permitido.");
            } else {
                responderError(intercambio, 405, "Método no permitido.");
            }
        } catch (DateTimeParseException excepcion) {
            responderError(intercambio, 400,
                    "Fecha u hora inválida. Use el formato aaaa-mm-dd y HH:mm.");
        } catch (NumberFormatException excepcion) {
            responderError(intercambio, 400, "Valor numérico inválido.");
        } catch (SgaException excepcion) {
            responderError(intercambio, 409, excepcion.getMessage());
        } catch (IllegalArgumentException excepcion) {
            responderError(intercambio, 400, excepcion.getMessage());
        } catch (RuntimeException excepcion) {
            excepcion.printStackTrace();
            responderError(intercambio, 500,
                    "Error interno del servidor: " + excepcion.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // GET
    // ------------------------------------------------------------------

    private void atenderGet(HttpExchange intercambio, String recurso, String[] partes)
            throws IOException {
        String consulta = intercambio.getRequestURI().getRawQuery();

        switch (recurso) {
            case "resumen":
                responder(intercambio, 200, resumen());
                return;
            case "tablero":
                responder(intercambio, 200, Serializador.tablero(sistema));
                return;
            case "reservas":
                if (partes.length == 2) {
                    responder(intercambio, 200, reservas());
                } else {
                    Reserva reserva = sistema.getGestorReservas()
                            .buscarReserva(decodificar(partes[2]));
                    responder(intercambio, 200, Serializador.reserva(reserva, sistema));
                }
                return;
            case "disponibilidad":
                responder(intercambio, 200, disponibilidad(consulta));
                return;
            case "cotizacion":
                responder(intercambio, 200, cotizacion(consulta));
                return;
            case "huespedes":
                responder(intercambio, 200, huespedes(consulta));
                return;
            case "calendario":
                responder(intercambio, 200, calendario(consulta));
                return;
            case "preparacion":
                responder(intercambio, 200, Serializador.preparacion(sistema));
                return;
            case "mapa":
                if (partes.length >= 3 && "analisis".equals(partes[2])) {
                    Json.Objeto mapa = Serializador.mapa(sistema);
                    responder(intercambio, 200, mapa.objeto("analisis"));
                } else {
                    responder(intercambio, 200, Serializador.mapa(sistema));
                }
                return;
            case "estadisticas":
                responder(intercambio, 200, Serializador.estadisticas(sistema));
                return;
            case "canal-externo":
                responder(intercambio, 200, Serializador.canalExterno(sistema));
                return;
            case "temporadas":
                responder(intercambio, 200, Serializador.temporadas(sistema));
                return;
            case "empleados":
                responder(intercambio, 200, empleados());
                return;
            default:
                responderError(intercambio, 404, "Recurso no encontrado: /api/" + recurso);
        }
    }

    private Json.Objeto resumen() {
        Json.Objeto base = Serializador.tablero(sistema);
        return Json.objeto()
                .poner("hoy", base.valor("hoy"))
                .poner("turno", base.valor("turno"))
                .poner("totales", base.valor("totales"))
                .poner("conteoEstados", base.valor("conteoEstados"))
                .poner("preparacion", Serializador.preparacion(sistema).objeto("resumen"))
                .poner("canalExterno", Serializador.canalExterno(sistema).objeto("resumen"))
                .poner("version", "2.0.0");
    }

    private Json.Objeto empleados() {
        Json.Arreglo arreglo = Json.arreglo();
        for (Empleado empleado : sistema.getEmpleados()) {
            arreglo.agregar(Serializador.empleado(empleado));
        }
        return Json.objeto().poner("empleados", arreglo);
    }

    private Json.Objeto reservas() {
        Json.Arreglo arreglo = Json.arreglo();
        for (Reserva reserva : sistema.todasReservas()) {
            arreglo.agregar(Serializador.reserva(reserva, sistema));
        }

        int[] porEstado = sistema.getServicioEstadisticas().reservasPorEstado();
        Json.Arreglo estados = Json.arreglo();
        for (model.EstadoReserva estado : model.EstadoReserva.values()) {
            estados.agregar(Json.objeto()
                    .poner("clave", estado.name())
                    .poner("descripcion", estado.getDescripcion())
                    .poner("cantidad", porEstado[estado.ordinal()]));
        }

        return Json.objeto()
                .poner("reservas", arreglo)
                .poner("estados", estados)
                .poner("total", arreglo.tamano())
                .poner("totales", Serializador.totales(sistema));
    }

    private Json.Objeto disponibilidad(String consulta) {
        String entrada = parametro(consulta, "entrada");
        String salida = parametro(consulta, "salida");
        if (entrada == null || salida == null) {
            throw new IllegalArgumentException("Debe indicar las fechas de entrada y salida.");
        }
        LocalDate fechaEntrada = LocalDate.parse(entrada);
        LocalDate fechaSalida = LocalDate.parse(salida);
        Estancia estancia = new Estancia(fechaEntrada, fechaSalida);
        int personas = entero(parametro(consulta, "personas"), 1);
        String orden = parametro(consulta, "orden");
        if (orden == null) {
            orden = "precio";
        }

        ListaEnlazada<Apartamento> disponibles;
        switch (orden.toLowerCase()) {
            case "capacidad":
                disponibles = sistema.apartamentosDisponiblesPorCapacidad(estancia, personas);
                break;
            case "reservas":
                disponibles = sistema.apartamentosDisponiblesPorNumeroReservas(estancia, personas);
                break;
            case "precio":
            default:
                disponibles = sistema.apartamentosDisponiblesPorPrecio(estancia, personas,
                        sistema.getServicioTemporadas().temporadaDe(fechaEntrada));
                break;
        }

        Json.Arreglo arreglo = Json.arreglo();
        for (Apartamento apartamento : disponibles) {
            Json.Objeto item = Serializador.apartamento(apartamento);
            item.poner("totalEstimado", Serializador.cotizacion(sistema, apartamento,
                    fechaEntrada, fechaSalida, personas).valor("total"));
            arreglo.agregar(item);
        }

        return Json.objeto()
                .poner("entrada", Serializador.fecha(fechaEntrada))
                .poner("salida", Serializador.fecha(fechaSalida))
                .poner("noches", estancia.numeroNoches())
                .poner("personas", personas)
                .poner("orden", orden)
                .poner("apartamentos", arreglo)
                .poner("total", arreglo.tamano());
    }

    private Json.Objeto cotizacion(String consulta) {
        String codigo = parametro(consulta, "apartamento");
        String entrada = parametro(consulta, "entrada");
        String salida = parametro(consulta, "salida");
        if (codigo == null || entrada == null || salida == null) {
            throw new IllegalArgumentException(
                    "Debe indicar apartamento, entrada y salida para cotizar.");
        }
        Apartamento apartamento = sistema.buscarApartamento(codigo);
        LocalDate fechaEntrada = LocalDate.parse(entrada);
        LocalDate fechaSalida = LocalDate.parse(salida);
        int personas = Math.max(1, entero(parametro(consulta, "personas"), 1));
        Json.Objeto cotizacion = Serializador.cotizacion(sistema, apartamento,
                fechaEntrada, fechaSalida, personas);
        cotizacion.poner("disponible", sistema.getGestorReservas()
                .estaDisponible(apartamento, new Estancia(fechaEntrada, fechaSalida)));
        return cotizacion;
    }

    private Json.Objeto huespedes(String consulta) {
        String texto = parametro(consulta, "q");
        String normalizado = texto == null ? "" : Huesped.normalizarDocumento(texto).toLowerCase();

        Json.Arreglo arreglo = Json.arreglo();
        for (Huesped huesped : sistema.todosHuespedes()) {
            boolean coincide = texto == null || texto.trim().isEmpty()
                    || huesped.getNombre().toLowerCase().contains(texto.toLowerCase())
                    || Huesped.normalizarDocumento(huesped.getDocumento())
                    .toLowerCase().contains(normalizado);
            if (coincide) {
                arreglo.agregar(Serializador.huesped(huesped));
            }
        }
        return Json.objeto()
                .poner("huespedes", arreglo)
                .poner("total", arreglo.tamano())
                .poner("totalSistema", sistema.todosHuespedes().tamano());
    }

    private Json.Objeto calendario(String consulta) {
        String inicio = parametro(consulta, "inicio");
        LocalDate fechaInicio = inicio == null ? LocalDate.now() : LocalDate.parse(inicio);
        int dias = Math.min(60, Math.max(1, entero(parametro(consulta, "dias"), 30)));
        return Serializador.calendario(sistema, fechaInicio, dias);
    }

    // ------------------------------------------------------------------
    // POST
    // ------------------------------------------------------------------

    private void atenderPost(HttpExchange intercambio, String recurso, String[] partes)
            throws IOException {
        switch (recurso) {
            case "reservas":
                if (partes.length >= 3 && "vencimientos".equals(partes[2])) {
                    procesarVencimientos(intercambio);
                } else if (partes.length >= 4) {
                    accionReserva(intercambio, decodificar(partes[2]), partes[3]);
                } else {
                    crearReserva(intercambio);
                }
                return;
            case "huespedes":
                if (partes.length >= 4 && "favoritos".equals(partes[3])) {
                    alternarFavorito(intercambio, decodificar(partes[2]));
                } else {
                    crearHuesped(intercambio);
                }
                return;
            case "preparacion":
                accionPreparacion(intercambio, decodificar(partes[2]), partes[3]);
                return;
            case "mapa":
                accionMapa(intercambio, partes);
                return;
            case "turno":
                if (partes.length >= 3 && "rotar".equals(partes[2])) {
                    rotarTurno(intercambio);
                } else {
                    responderError(intercambio, 404, "Recurso no encontrado.");
                }
                return;
            case "canal-externo":
                if (partes.length >= 3 && "procesar".equals(partes[2])) {
                    procesarCanal(intercambio);
                } else {
                    crearSolicitud(intercambio);
                }
                return;
            case "temporadas":
                crearTemporada(intercambio);
                return;
            case "grupos":
                asignarGrupo(intercambio);
                return;
            case "datos":
                cargarDatos(intercambio, partes);
                return;
            default:
                responderError(intercambio, 404, "Recurso no encontrado: /api/" + recurso);
        }
    }

    private void crearReserva(HttpExchange intercambio) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        String documento = requerido(cuerpo, "titularDocumento");
        Apartamento apartamento = sistema.buscarApartamento(
                requerido(cuerpo, "apartamento"));
        LocalDate entrada = LocalDate.parse(requerido(cuerpo, "entrada"));
        LocalDate salida = LocalDate.parse(requerido(cuerpo, "salida"));
        Estancia estancia = new Estancia(entrada, salida);

        Huesped titular = sistema.buscarHuespedOpcional(documento);
        if (titular == null) {
            String nombre = requerido(cuerpo, "titularNombre");
            LocalDate nacimiento = fechaOpcional(cuerpo.texto("titularFechaNacimiento"));
            titular = new Huesped(documento, nombre, nacimiento,
                    cuerpo.texto("titularTelefono"), cuerpo.texto("titularCorreo"));
            sistema.registrarHuesped(titular);
        } else {
            actualizarContacto(titular, cuerpo);
        }

        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        Json.Arreglo arreglo = cuerpo.arreglo("ocupantes");
        boolean algunTitular = false;
        if (arreglo != null) {
            for (Object valor : arreglo.valores()) {
                if (!(valor instanceof Json.Objeto)) {
                    continue;
                }
                Json.Objeto item = (Json.Objeto) valor;
                String nombre = item.texto("nombre");
                if (nombre == null || nombre.trim().isEmpty()) {
                    continue;
                }
                LocalDate nacimiento = fechaOpcional(item.texto("fechaNacimiento"));
                boolean esTitular = item.logico("titular", false);
                algunTitular = algunTitular || esTitular;
                ocupantes.agregar(new Ocupante(nombre.trim(), nacimiento, esTitular));
            }
        }
        if (ocupantes.estaVacia()) {
            LocalDate nacimiento = titular.getFechaNacimiento() == null
                    ? LocalDate.now().minusYears(30) : titular.getFechaNacimiento();
            ocupantes.agregar(new Ocupante(titular.getNombre(), nacimiento, true));
        } else if (!algunTitular) {
            Ocupante primero = ocupantes.remover(0);
            ocupantes.agregarAlInicio(new Ocupante(primero.getNombre(),
                    primero.getFechaNacimiento(), true));
        }

        LocalTime hora = hora(cuerpo.texto("hora"));
        String canalTexto = cuerpo.texto("canal", "DIRECTA").toUpperCase();
        if ("EXTERNA".equals(canalTexto)) {
            throw new IllegalArgumentException(
                    "Las reservas externas se crean procesando el canal externo.");
        }
        CanalReserva canal = CanalReserva.valueOf(canalTexto);

        Reserva reserva = canal == CanalReserva.PORTAL
                ? sistema.crearReservaPortal(titular, apartamento, estancia, ocupantes, hora)
                : sistema.crearReservaDirecta(titular, apartamento, estancia, ocupantes, hora);

        responder(intercambio, 201, Json.objeto()
                .poner("mensaje", "Reserva " + reserva.getCodigo() + " creada correctamente.")
                .poner("reserva", Serializador.reserva(reserva, sistema)));
    }

    private void actualizarContacto(Huesped huesped, Json.Objeto cuerpo) {
        if (cuerpo.tiene("titularTelefono")) {
            huesped.setTelefono(cuerpo.texto("titularTelefono"));
        }
        if (cuerpo.tiene("titularCorreo")) {
            huesped.setCorreo(cuerpo.texto("titularCorreo"));
        }
        String nacimiento = cuerpo.texto("titularFechaNacimiento");
        if (nacimiento != null && !nacimiento.trim().isEmpty()) {
            huesped.setFechaNacimiento(LocalDate.parse(nacimiento));
        }
    }

    private void procesarVencimientos(HttpExchange intercambio) throws IOException {
        ListaEnlazada<Reserva> canceladas =
                sistema.procesarVencimientos(LocalDateTime.now());
        Json.Arreglo arreglo = Json.arreglo();
        for (Reserva reserva : canceladas) {
            arreglo.agregar(Serializador.resumenReserva(reserva));
        }
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", canceladas.estaVacia()
                        ? "No hay reservas pendientes vencidas en este momento."
                        : "Se cancelaron automáticamente " + canceladas.tamano()
                        + " reserva(s) por superar las 24 horas sin anticipo.")
                .poner("canceladas", arreglo));
    }

    private void accionReserva(HttpExchange intercambio, String codigo, String accion)
            throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        switch (accion) {
            case "anticipo": {
                double monto = cuerpo.numero("monto", 0);
                if (monto <= 0) {
                    throw new IllegalArgumentException("El monto del anticipo debe ser mayor que cero.");
                }
                Reserva reserva = sistema.registrarAnticipo(codigo, monto);
                responder(intercambio, 200, respuestaReserva("Anticipo registrado.", reserva));
                return;
            }
            case "checkin": {
                Reserva reserva = sistema.registrarCheckIn(codigo);
                responder(intercambio, 200, respuestaReserva(
                        "Check-in registrado. El apartamento quedó ocupado.", reserva));
                return;
            }
            case "checkout": {
                Reserva reserva = sistema.registrarCheckOut(codigo);
                responder(intercambio, 200, respuestaReserva(
                        "Check-out registrado. El apartamento quedó pendiente de preparación.",
                        reserva));
                return;
            }
            case "cancelar": {
                String motivo = cuerpo.texto("motivo", "Cancelación registrada por recepción");
                double devolucion = Serializador.devolucionEstimada(
                        sistema.getGestorReservas().buscarReserva(codigo));
                Reserva reserva = sistema.cancelarReserva(codigo, motivo);
                responder(intercambio, 200, respuestaReserva(
                        "Reserva cancelada. Noches liberadas. Devolución estimada: $"
                                + String.format("%,.0f", devolucion), reserva));
                return;
            }
            case "noshow": {
                String motivo = cuerpo.texto("motivo", "No-show registrado por recepción");
                Reserva reserva = sistema.registrarNoShow(codigo, motivo);
                responder(intercambio, 200, respuestaReserva("No-show registrado.", reserva));
                return;
            }
            case "movimientos": {
                String tipo = requerido(cuerpo, "tipo").toUpperCase();
                String concepto = requerido(cuerpo, "concepto");
                double valor = cuerpo.numero("valor", 0);
                if (valor <= 0) {
                    throw new IllegalArgumentException("El valor del movimiento debe ser mayor que cero.");
                }
                switch (tipo) {
                    case "CARGO":
                        sistema.getGestorFolios().registrarCargo(codigo, concepto, valor);
                        break;
                    case "PAGO":
                        sistema.getGestorFolios().registrarPago(codigo, concepto, valor);
                        break;
                    case "ABONO":
                        sistema.getGestorFolios().registrarAbono(codigo, concepto, valor);
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Tipo de movimiento inválido. Use CARGO, PAGO o ABONO.");
                }
                Reserva reserva = sistema.getGestorReservas().buscarReserva(codigo);
                responder(intercambio, 200, respuestaReserva(
                        "Movimiento registrado en el folio.", reserva));
                return;
            }
            case "deshacer": {
                model.Movimiento inverso = sistema.getGestorFolios()
                        .deshacerUltimaOperacion(codigo);
                Reserva reserva = sistema.getGestorReservas().buscarReserva(codigo);
                responder(intercambio, 200, Json.objeto()
                        .poner("mensaje", "Se registró el contra-movimiento.")
                        .poner("movimiento", Serializador.movimiento(inverso))
                        .poner("reserva", Serializador.reserva(reserva, sistema)));
                return;
            }
            default:
                responderError(intercambio, 404, "Acción de reserva no reconocida.");
        }
    }

    private Json.Objeto respuestaReserva(String mensaje, Reserva reserva) {
        return Json.objeto()
                .poner("mensaje", mensaje)
                .poner("reserva", Serializador.reserva(reserva, sistema));
    }

    private void crearHuesped(HttpExchange intercambio) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        String documento = requerido(cuerpo, "documento");
        if (sistema.buscarHuespedOpcional(documento) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un huésped con el documento " + documento + ".");
        }
        Huesped huesped = new Huesped(documento, requerido(cuerpo, "nombre"),
                fechaOpcional(cuerpo.texto("fechaNacimiento")),
                cuerpo.texto("telefono"), cuerpo.texto("correo"));
        sistema.registrarHuesped(huesped);
        responder(intercambio, 201, Json.objeto()
                .poner("mensaje", "Huésped registrado correctamente.")
                .poner("huesped", Serializador.huesped(huesped)));
    }

    private void alternarFavorito(HttpExchange intercambio, String documento) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        String codigo = requerido(cuerpo, "apartamento");
        sistema.buscarApartamento(codigo);
        Huesped huesped = sistema.buscarHuesped(documento);
        boolean favorito = huesped.alternarFavorito(codigo);
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", (favorito ? "Se marcó " : "Se quitó ")
                        + codigo + (favorito ? " como favorito." : " de favoritos."))
                .poner("favorito", favorito)
                .poner("huesped", Serializador.huesped(huesped)));
    }

    private void accionPreparacion(HttpExchange intercambio, String codigo, String accion)
            throws IOException {
        Apartamento apartamento = sistema.buscarApartamento(codigo);
        if ("iniciar".equals(accion)) {
            model.TareaPreparacion tarea =
                    sistema.getGestorPreparacion().iniciar(apartamento);
            responder(intercambio, 200, Json.objeto()
                    .poner("mensaje", "Preparación iniciada para " + codigo + ".")
                    .poner("tarea", Serializador.tarea(tarea)));
        } else if ("completar".equals(accion)) {
            long segundos = sistema.getGestorPreparacion().completar(apartamento);
            responder(intercambio, 200, Json.objeto()
                    .poner("mensaje", "Preparación completada. El apartamento quedó preparado.")
                    .poner("duracionSegundos", segundos)
                    .poner("apartamento", Serializador.apartamento(apartamento)));
        } else {
            responderError(intercambio, 404, "Acción de preparación no reconocida.");
        }
    }

    private void accionMapa(HttpExchange intercambio, String[] partes) throws IOException {
        if (partes.length < 3) {
            responderError(intercambio, 404, "Recurso de mapa no reconocido.");
            return;
        }
        Json.Objeto cuerpo = cuerpo(intercambio);
        switch (partes[2]) {
            case "ruta": {
                String origen = requerido(cuerpo, "origen");
                String destino = requerido(cuerpo, "destino");
                Grafo.Ruta<String> ruta = sistema.getGestorEdificio().ruta(origen, destino);
                responder(intercambio, 200, Serializador.ruta(ruta));
                return;
            }
            case "limpieza": {
                String partida = cuerpo.texto("puntoPartida", GestorEdificio.RECEPCION);
                Json.Arreglo tramos = Json.arreglo();
                String actual = partida;
                int total = 0;
                for (Apartamento apartamento : sistema.getGestorPreparacion()
                        .rutaDeLimpieza(partida)) {
                    Grafo.Ruta<String> tramo = sistema.getGestorEdificio()
                            .ruta(actual, apartamento.getCodigo());
                    Json.Objeto item = Json.objeto()
                            .poner("codigo", apartamento.getCodigo())
                            .poner("nombre", apartamento.getNombre())
                            .poner("estado", Serializador.estadoApartamento(apartamento.getEstado()))
                            .poner("desde", actual)
                            .poner("alcanzable", tramo.esAlcanzable())
                            .poner("distancia", tramo.esAlcanzable() ? tramo.getDistancia() : null);
                    Json.Arreglo camino = Json.arreglo();
                    for (String etiqueta : tramo.getEtiquetas()) {
                        camino.agregar(etiqueta);
                    }
                    item.poner("camino", camino);
                    tramos.agregar(item);
                    if (tramo.esAlcanzable()) {
                        total += tramo.getDistancia();
                        actual = apartamento.getCodigo();
                    }
                }
                responder(intercambio, 200, Json.objeto()
                        .poner("puntoPartida", partida)
                        .poner("distanciaTotal", total)
                        .poner("tramos", tramos));
                return;
            }
            case "pasillo": {
                String a = requerido(cuerpo, "a");
                String b = requerido(cuerpo, "b");
                boolean abrir = cuerpo.logico("abrir", false);
                boolean cambio = abrir
                        ? sistema.getGestorEdificio().abrirPasillo(a, b)
                        : sistema.getGestorEdificio().cerrarPasillo(a, b);
                if (!cambio) {
                    throw new IllegalArgumentException(
                            "No se encontró el pasillo entre " + a + " y " + b + ".");
                }
                responder(intercambio, 200, Json.objeto()
                        .poner("mensaje", "Pasillo " + (abrir ? "abierto" : "cerrado")
                                + " entre " + a + " y " + b + ".")
                        .poner("mapa", Serializador.mapa(sistema)));
                return;
            }
            default:
                responderError(intercambio, 404, "Acción de mapa no reconocida.");
        }
    }

    private void rotarTurno(HttpExchange intercambio) throws IOException {
        Empleado siguiente = sistema.rotarTurnoRecepcion();
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", siguiente == null
                        ? "No hay recepcionistas activos para el turno."
                        : "Turno asignado a " + siguiente.getNombre() + ".")
                .poner("turno", siguiente == null ? null : Serializador.empleado(siguiente)));
    }

    private void crearSolicitud(HttpExchange intercambio) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        String identificador = cuerpo.texto("identificador");
        if (identificador == null || identificador.trim().isEmpty()) {
            identificador = "WEB-" + System.currentTimeMillis();
        }
        SolicitudExterna solicitud = new SolicitudExterna(identificador,
                requerido(cuerpo, "nombre"),
                cuerpo.texto("documento", "EXT-" + System.currentTimeMillis()),
                cuerpo.texto("correo"), cuerpo.texto("telefono"),
                requerido(cuerpo, "apartamento"),
                new Estancia(LocalDate.parse(requerido(cuerpo, "entrada")),
                        LocalDate.parse(requerido(cuerpo, "salida"))),
                Math.max(1, cuerpo.entero("personas", 1)),
                LocalDateTime.now());
        sistema.encolarSolicitudExterna(solicitud);
        responder(intercambio, 201, Json.objeto()
                .poner("mensaje", "Solicitud " + identificador + " encolada.")
                .poner("canalExterno", Serializador.canalExterno(sistema)));
    }

    private void procesarCanal(HttpExchange intercambio) throws IOException {
        ListaEnlazada<Reserva> creadas = sistema.procesarCanalExterno();
        Json.Arreglo arreglo = Json.arreglo();
        for (Reserva reserva : creadas) {
            arreglo.agregar(Serializador.resumenReserva(reserva));
        }
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", creadas.estaVacia()
                        ? "No se crearon reservas; revise los conflictos de canal."
                        : "Se crearon " + creadas.tamano()
                        + " reserva(s) desde el canal externo.")
                .poner("creadas", arreglo)
                .poner("canalExterno", Serializador.canalExterno(sistema)));
    }

    private void crearTemporada(HttpExchange intercambio) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        Temporada temporada = Temporada.valueOf(requerido(cuerpo, "temporada").toUpperCase());
        LocalDate inicio = LocalDate.parse(requerido(cuerpo, "inicio"));
        LocalDate fin = LocalDate.parse(requerido(cuerpo, "fin"));
        sistema.getServicioTemporadas().registrar(temporada, inicio, fin);
        responder(intercambio, 201, Json.objeto()
                .poner("mensaje", "Temporada " + temporada.getDescripcion()
                        + " registrada del " + Serializador.fecha(inicio)
                        + " al " + Serializador.fecha(fin) + ".")
                .poner("temporadas", Serializador.temporadas(sistema)));
    }

    private void asignarGrupo(HttpExchange intercambio) throws IOException {
        Json.Objeto cuerpo = cuerpo(intercambio);
        Estancia estancia = new Estancia(LocalDate.parse(requerido(cuerpo, "entrada")),
                LocalDate.parse(requerido(cuerpo, "salida")));
        int personas = Math.max(1, cuerpo.entero("personas", 1));
        ListaEnlazada<Apartamento> asignados = sistema.asignarGrupo(estancia, personas);
        Json.Arreglo arreglo = Json.arreglo();
        int capacidad = 0;
        for (Apartamento apartamento : asignados) {
            arreglo.agregar(Serializador.apartamento(apartamento));
            capacidad += apartamento.getCapacidadMaxima();
        }
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", asignados.estaVacia()
                        ? "No hay combinación de apartamentos disponible para ese grupo."
                        : "Combinación mínima de " + asignados.tamano()
                        + " apartamento(s) con capacidad para " + capacidad + ".")
                .poner("apartamentos", arreglo)
                .poner("capacidadTotal", capacidad)
                .poner("personas", personas));
    }

    private void cargarDatos(HttpExchange intercambio, String[] partes) throws IOException {
        if (partes.length < 3) {
            responderError(intercambio, 404, "Acción de carga no reconocida.");
            return;
        }
        String accion = partes[2];
        String mensaje;
        switch (accion) {
            case "escenario":
                sistema = EscenarioPrueba.construir();
                mensaje = "Escenario de prueba cargado con 12 apartamentos y datos de ejemplo.";
                break;
            case "vacio":
                sistema = new SistemaAlojamiento();
                mensaje = "Sistema vacío. Registre apartamentos y huéspedes.";
                break;
            case "archivo": {
                Json.Objeto cuerpo = cuerpo(intercambio);
                String ruta = requerido(cuerpo, "ruta");
                File archivo = new File(ruta);
                if (!archivo.isFile()) {
                    throw new IllegalArgumentException("No existe el archivo: " + ruta);
                }
                sistema = CargadorDatos.cargar(archivo);
                mensaje = "Datos cargados desde " + archivo.getAbsolutePath();
                break;
            }
            default:
                responderError(intercambio, 404, "Acción de carga no reconocida.");
                return;
        }
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", mensaje)
                .poner("resumen", resumen()));
    }

    // ------------------------------------------------------------------
    // PUT / PATCH
    // ------------------------------------------------------------------

    private void atenderPut(HttpExchange intercambio, String recurso, String[] partes)
            throws IOException {
        if (!"huespedes".equals(recurso) || partes.length < 3) {
            responderError(intercambio, 404, "Recurso no encontrado.");
            return;
        }
        Json.Objeto cuerpo = cuerpo(intercambio);
        Huesped huesped = sistema.buscarHuesped(decodificar(partes[2]));
        if (cuerpo.tiene("nombre")) {
            huesped.setNombre(requerido(cuerpo, "nombre"));
        }
        if (cuerpo.tiene("fechaNacimiento")) {
            huesped.setFechaNacimiento(fechaOpcional(cuerpo.texto("fechaNacimiento")));
        }
        if (cuerpo.tiene("telefono")) {
            huesped.setTelefono(cuerpo.texto("telefono"));
        }
        if (cuerpo.tiene("correo")) {
            huesped.setCorreo(cuerpo.texto("correo"));
        }
        if (cuerpo.tiene("rutaFotografia")) {
            huesped.setRutaFotografia(cuerpo.texto("rutaFotografia"));
        }
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", "Ficha del huésped actualizada.")
                .poner("huesped", Serializador.huesped(huesped)));
    }

    private void atenderPatch(HttpExchange intercambio, String recurso, String[] partes)
            throws IOException {
        if (!"apartamentos".equals(recurso) || partes.length < 4
                || !"estado".equals(partes[3])) {
            responderError(intercambio, 404, "Recurso no encontrado.");
            return;
        }
        Json.Objeto cuerpo = cuerpo(intercambio);
        Apartamento apartamento = sistema.buscarApartamento(decodificar(partes[2]));
        EstadoApartamento estado = EstadoApartamento.valueOf(
                requerido(cuerpo, "estado").toUpperCase());
        sistema.cambiarEstadoApartamento(apartamento.getCodigo(), estado,
                cuerpo.texto("motivo"));
        responder(intercambio, 200, Json.objeto()
                .poner("mensaje", "El apartamento " + apartamento.getCodigo()
                        + " pasó a " + estado.getDescripcion() + ".")
                .poner("apartamento", Serializador.apartamento(apartamento)));
    }

    // ------------------------------------------------------------------
    // Utilidades HTTP y de parámetros
    // ------------------------------------------------------------------

    private static String[] segmentos(URI uri) {
        String ruta = uri.getPath();
        if (ruta == null || ruta.equals("/")) {
            return new String[0];
        }
        if (ruta.startsWith("/")) {
            ruta = ruta.substring(1);
        }
        if (ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        if (ruta.isEmpty()) {
            return new String[0];
        }
        String[] crudos = ruta.split("/");
        String[] limpios = new String[crudos.length];
        for (int i = 0; i < crudos.length; i++) {
            limpios[i] = decodificar(crudos[i]);
        }
        return limpios;
    }

    private static String decodificar(String texto) {
        try {
            return URLDecoder.decode(texto, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException excepcion) {
            return texto;
        }
    }

    private static String parametro(String consulta, String nombre) {
        if (consulta == null || consulta.isEmpty()) {
            return null;
        }
        for (String par : consulta.split("&")) {
            int igual = par.indexOf('=');
            String clave = igual < 0 ? par : par.substring(0, igual);
            if (nombre.equals(decodificar(clave))) {
                return igual < 0 ? "" : decodificar(par.substring(igual + 1));
            }
        }
        return null;
    }

    private static int entero(String texto, int porDefecto) {
        if (texto == null || texto.trim().isEmpty()) {
            return porDefecto;
        }
        return Integer.parseInt(texto.trim());
    }

    private static Json.Objeto cuerpo(HttpExchange intercambio) throws IOException {
        InputStream entrada = intercambio.getRequestBody();
        String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        if (texto.trim().isEmpty()) {
            return Json.objeto();
        }
        return Json.analizarObjeto(texto);
    }

    private static String requerido(Json.Objeto cuerpo, String clave) {
        String valor = cuerpo.texto(clave);
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + clave + "' es obligatorio.");
        }
        return valor.trim();
    }

    private static LocalDate fechaOpcional(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(texto.trim());
    }

    private static LocalTime hora(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return LocalTime.of(15, 0);
        }
        return LocalTime.parse(texto.trim());
    }

    private static void responder(HttpExchange intercambio, int estado, Json.Objeto cuerpo)
            throws IOException {
        responderTexto(intercambio, estado, cuerpo.serializar(), "application/json; charset=utf-8");
    }

    private static void responderError(HttpExchange intercambio, int estado, String mensaje)
            throws IOException {
        Json.Objeto cuerpo = Json.objeto()
                .poner("error", mensaje == null ? "Error inesperado." : mensaje)
                .poner("estado", estado);
        responderTexto(intercambio, estado, cuerpo.serializar(),
                "application/json; charset=utf-8");
    }

    private static void responderVacio(HttpExchange intercambio, int estado) throws IOException {
        intercambio.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        intercambio.getResponseHeaders().set("Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, OPTIONS");
        intercambio.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        intercambio.sendResponseHeaders(estado, -1);
        intercambio.close();
    }

    private static void responderTexto(HttpExchange intercambio, int estado, String texto,
                                       String tipo) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        intercambio.getResponseHeaders().set("Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, OPTIONS");
        intercambio.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
