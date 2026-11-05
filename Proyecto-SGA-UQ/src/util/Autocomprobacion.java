package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import datastructures.ArbolBinarioBusqueda;
import datastructures.Cola;
import datastructures.ColaPrioridad;
import datastructures.Conjunto;
import datastructures.Grafo;
import datastructures.ListaCircular;
import datastructures.ListaDobleEnlazada;
import datastructures.ListaEnlazada;
import datastructures.Pila;
import exceptions.CapacidadExcedidaException;
import exceptions.FechasInvalidasException;
import exceptions.ReservaSolapadaException;
import exceptions.TransicionEstadoInvalidaException;
import model.Apartamento;
import model.CalendarioTemporadas;
import model.Empleado;
import model.Estancia;
import model.EstadoApartamento;
import model.EstadoReserva;
import model.Huesped;
import model.Ocupante;
import model.Reserva;
import model.SolicitudExterna;
import model.Temporada;
import gui.HistorialNavegacion;
import service.GestorEdificio;
import service.ServicioEstadisticas;
import service.SistemaAlojamiento;

/**
 * Autocomprobación del SGA: verifica en tiempo de ejecución las reglas de negocio y
 * el comportamiento de las estructuras de datos propias.
 *
 * <p>Se puede ejecutar desde la línea de comandos con {@code java Main --consola}.
 * Imprime un resumen con el número de comprobaciones y los fallos encontrados.</p>
 */
public final class Autocomprobacion {

    private static int comprobaciones;
    private static int fallos;

    private Autocomprobacion() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Ejecuta todas las comprobaciones.
     *
     * @return {@code true} si todas pasaron
     */
    public static boolean ejecutar() {
        comprobaciones = 0;
        fallos = 0;

        probarEstructurasBasicas();
        probarArbolBinario();
        probarColaPrioridad();
        probarGrafo();
        probarEstanciaYNoches();
        probarLiquidacionPorTemporadas();
        probarSolapamientoYCapacidad();
        probarCicloDeVida();
        probarCanalExterno();
        probarBacktracking();
        probarFolioYDeshacer();
        probarEscenarioCompleto();
        probarAdministracionYReportes();

        System.out.println();
        System.out.println("=================================================");
        if (fallos == 0) {
            System.out.println(" AUTOCONTROL: " + comprobaciones + " comprobaciones, TODAS correctas.");
        } else {
            System.out.println(" AUTOCONTROL: " + comprobaciones + " comprobaciones, "
                    + fallos + " FALLOS.");
        }
        System.out.println("=================================================");
        return fallos == 0;
    }

    // ------------------------------------------------------------------
    // Estructuras propias
    // ------------------------------------------------------------------

    private static void probarEstructurasBasicas() {
        System.out.println("[1] Estructuras propias");

        // Lista enlazada.
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        for (int i = 1; i <= 5; i++) {
            lista.agregar(i);
        }
        comprobar("ListaEnlazada.tamano", lista.tamano() == 5);
        comprobar("ListaEnlazada.obtener(2)", lista.obtener(2) == 3);
        comprobar("ListaEnlazada.remover(0)", lista.remover(0) == 1 && lista.tamano() == 4);
        int suma = 0;
        for (Integer valor : lista) {
            suma += valor;
        }
        comprobar("ListaEnlazada es iterable", suma == 14);

        // Pila (LIFO).
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");
        comprobar("Pila LIFO", "c".equals(pila.desapilar()) && "b".equals(pila.verCima()));

        // Cola (FIFO).
        Cola<String> cola = new Cola<>();
        cola.encolar("primero");
        cola.encolar("segundo");
        comprobar("Cola FIFO", "primero".equals(cola.desencolar())
                && "segundo".equals(cola.desencolar()) && cola.estaVacia());

        // Conjunto sin duplicados.
        Conjunto<String> conjunto = new Conjunto<>();
        comprobar("Conjunto agrega", conjunto.agregar("x"));
        comprobar("Conjunto no duplica", !conjunto.agregar("x") && conjunto.tamano() == 1);

        // Lista circular: gira sobre sí misma.
        ListaCircular<Integer> circular = new ListaCircular<>();
        circular.agregar(1);
        circular.agregar(2);
        circular.agregar(3);
        comprobar("ListaCircular.actual", circular.actual() == 1);
        comprobar("ListaCircular.siguiente no avanza", circular.siguiente() == 2
                && circular.actual() == 1);
        circular.rotar();
        circular.rotar();
        comprobar("ListaCircular.rotar avanza el turno", circular.actual() == 3);
        circular.rotar();
        comprobar("ListaCircular vuelve al inicio", circular.actual() == 1);
        comprobar("ListaCircular.remover el actual", circular.remover(1)
                && circular.tamano() == 2 && circular.actual() == 2);

        // Lista doblemente enlazada: iterador inverso.
        ListaDobleEnlazada<String> doble = new ListaDobleEnlazada<>();
        doble.agregar("uno");
        doble.agregar("dos");
        doble.agregar("tres");
        StringBuilder inverso = new StringBuilder();
        java.util.Iterator<String> it = doble.iteradorInverso();
        while (it.hasNext()) {
            inverso.append(it.next());
        }
        comprobar("ListaDobleEnlazada iterador inverso", "tresdosuno".equals(inverso.toString()));
    }

    private static void probarArbolBinario() {
        System.out.println("[2] Árbol binario de búsqueda");
        ArbolBinarioBusqueda<Integer> arbol = new ArbolBinarioBusqueda<>();
        int[] valores = {50, 30, 70, 20, 40, 60, 80};
        for (int valor : valores) {
            arbol.insertar(valor);
        }
        comprobar("Arbol.tamano", arbol.tamano() == 7);
        comprobar("Arbol.buscar", arbol.buscar(40) == 40 && arbol.buscar(99) == null);
        comprobar("Arbol.minimo/maximo", arbol.minimo() == 20 && arbol.maximo() == 80);
        ListaEnlazada<Integer> inOrden = arbol.recorridoInOrden();
        boolean ordenado = true;
        for (int i = 1; i < inOrden.tamano(); i++) {
            if (inOrden.obtener(i - 1) > inOrden.obtener(i)) {
                ordenado = false;
            }
        }
        comprobar("Arbol in-orden ordenado", ordenado);
        comprobar("Arbol.eliminar hoja", arbol.eliminar(20) && arbol.tamano() == 6);
        comprobar("Arbol.eliminar con dos hijos", arbol.eliminar(30) && !arbol.contiene(30));
    }

    private static void probarColaPrioridad() {
        System.out.println("[3] Cola de prioridad");
        ColaPrioridad<String> cola = new ColaPrioridad<>();
        cola.encolar("media", 2);
        cola.encolar("urgente", 1);
        cola.encolar("baja", 3);
        comprobar("ColaPrioridad ordena por prioridad",
                "urgente".equals(cola.desencolar())
                        && "media".equals(cola.desencolar())
                        && "baja".equals(cola.desencolar()));

        ColaPrioridad<String> conDesempate = new ColaPrioridad<>(
                (a, b) -> a.compareTo(b));
        conDesempate.encolar("zeta", 1);
        conDesempate.encolar("alfa", 1);
        comprobar("ColaPrioridad usa el comparador de desempate",
                "alfa".equals(conDesempate.desencolar()));

        ColaPrioridad<String> removible = new ColaPrioridad<>();
        removible.encolar("uno", 1);
        removible.encolar("dos", 2);
        removible.encolar("tres", 3);
        comprobar("ColaPrioridad.remover", removible.remover("dos") && removible.tamano() == 2);
    }

    private static void probarGrafo() {
        System.out.println("[4] Grafo y Dijkstra");
        Grafo<String> grafo = new Grafo<>();
        grafo.agregarNodo("A");
        grafo.agregarNodo("B");
        grafo.agregarNodo("C");
        grafo.agregarNodo("D");
        grafo.agregarArista("A", "B", 1);
        grafo.agregarArista("B", "C", 2);
        grafo.agregarArista("A", "C", 5);
        grafo.agregarArista("C", "D", 1);

        Grafo.Ruta<String> ruta = grafo.caminoMasCorto("A", "D");
        comprobar("Dijkstra encuentra ruta", ruta.esAlcanzable());
        comprobar("Dijkstra distancia mínima", ruta.getDistancia() == 4);
        comprobar("Dijkstra devuelve 4 nodos", ruta.getCamino().tamano() == 4);

        comprobar("Grafo.bfs alcanza todos", grafo.bfs("A").tamano() == 4);
        comprobar("Grafo.dfs alcanza todos", grafo.dfs("A").tamano() == 4);
        comprobar("Grafo.tieneCiclo", grafo.tieneCiclo());

        grafo.cerrarPasillo("C", "D");
        comprobar("Grafo cerrar pasillo deja D inalcanzable",
                !grafo.caminoMasCorto("A", "D").esAlcanzable()
                        && grafo.inalcanzables("A").tamano() == 1);
        grafo.abrirPasillo("C", "D");
        comprobar("Grafo reabrir pasillo restaura la ruta",
                grafo.caminoMasCorto("A", "D").esAlcanzable());
    }

    // ------------------------------------------------------------------
    // Reglas de negocio
    // ------------------------------------------------------------------

    private static void probarEstanciaYNoches() {
        System.out.println("[5] Estancia: la noche de salida no se ocupa");
        Estancia estancia = new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        comprobar("Estancia 10→12 son 2 noches", estancia.numeroNoches() == 2);
        comprobar("Ocupa la noche del 10", estancia.ocupaNoche(LocalDate.of(2026, 3, 10)));
        comprobar("Ocupa la noche del 11", estancia.ocupaNoche(LocalDate.of(2026, 3, 11)));
        comprobar("NO ocupa la noche del 12", !estancia.ocupaNoche(LocalDate.of(2026, 3, 12)));

        int noches = 0;
        for (LocalDate fecha : estancia) {
            noches++;
            comprobar("Noche iterada antes de la salida",
                    fecha.isBefore(LocalDate.of(2026, 3, 12)));
        }
        comprobar("El iterador devuelve 2 noches", noches == 2);

        boolean lanzo = false;
        try {
            new Estancia(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 10));
        } catch (FechasInvalidasException excepcion) {
            lanzo = true;
        }
        comprobar("Fecha de salida anterior lanza excepción", lanzo);
    }

    private static void probarLiquidacionPorTemporadas() {
        System.out.println("[6] Liquidación noche por noche");
        SistemaAlojamiento sistema = new SistemaAlojamiento();
        LocalDate base = LocalDate.of(2026, 6, 1);
        // Temporada alta del 3 al 4 de junio (noches 3 y 4).
        sistema.getServicioTemporadas().registrar(Temporada.ALTA, base.plusDays(2), base.plusDays(3));
        sistema.getCalendario().setTemporadaPorDefecto(Temporada.BAJA);

        Apartamento apartamento = new Apartamento("T-01", "Prueba", "T", 1, 1, 2, 0, 0);
        apartamento.definirTarifa(Temporada.BAJA, 100000);
        apartamento.definirTarifa(Temporada.MEDIA, 150000);
        apartamento.definirTarifa(Temporada.ALTA, 200000);
        sistema.registrarApartamento(apartamento);

        Huesped titular = new Huesped("999", "Prueba Titular",
                base.minusYears(30), "300", "prueba@correo.com");
        sistema.registrarHuesped(titular);

        // Estancia del 2 al 6 de junio: noches 2 (baja), 3 y 4 (alta), 5 (baja).
        Estancia estancia = new Estancia(base.plusDays(1), base.plusDays(5));
        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        ocupantes.agregar(new Ocupante("Adulto", base.minusYears(30), true));
        Reserva reserva = sistema.crearReservaDirecta(titular, apartamento, estancia,
                ocupantes, LocalTime.of(15, 0));

        double esperado = 100000 + 200000 + 200000 + 100000;
        comprobar("Liquidación cruza temporadas noche por noche",
                Math.abs(reserva.getTotalEstimado() - esperado) < 0.01);

        // Un menor de seis años ocupa cupo pero no paga.
        ListaEnlazada<Ocupante> conMenor = new ListaEnlazada<>();
        conMenor.agregar(new Ocupante("Adulto", base.minusYears(30), true));
        conMenor.agregar(new Ocupante("Bebé", base.minusYears(2)));
        Apartamento otro = new Apartamento("T-02", "Prueba 2", "T", 1, 1, 2, 0, 0);
        otro.definirTarifa(Temporada.BAJA, 100000);
        otro.definirTarifa(Temporada.MEDIA, 100000);
        otro.definirTarifa(Temporada.ALTA, 100000);
        sistema.registrarApartamento(otro);
        Reserva conBebe = sistema.crearReservaDirecta(titular, otro,
                new Estancia(base, base.plusDays(1)), conMenor, LocalTime.of(15, 0));
        comprobar("Menor de 6 años no paga pero ocupa cupo",
                conBebe.getOcupantes().tamano() == 2
                        && Math.abs(conBebe.getTotalEstimado() - 100000) < 0.01);
    }

    private static void probarSolapamientoYCapacidad() {
        System.out.println("[7] Solapamiento y capacidad");
        SistemaAlojamiento sistema = sistemaMinimo();
        Huesped titular = sistema.buscarHuesped("100");
        Apartamento apartamento = sistema.buscarApartamento("P-01");

        Reserva primera = sistema.crearReservaDirecta(titular, apartamento,
                new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)),
                unOcupante(), LocalTime.of(15, 0));
        comprobar("Reserva creada", primera != null);

        // Estancia 12→14 no se solapa (la noche de salida no se ocupa).
        boolean sinConflicto = true;
        try {
            sistema.crearReservaDirecta(titular, apartamento,
                    new Estancia(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 14)),
                    unOcupante(), LocalTime.of(15, 0));
        } catch (ReservaSolapadaException excepcion) {
            sinConflicto = false;
        }
        comprobar("Checkout/checkin el mismo día no se solapa", sinConflicto);

        // Estancia 11→13 sí se solapa.
        boolean lanzoSolapamiento = false;
        try {
            sistema.crearReservaDirecta(titular, apartamento,
                    new Estancia(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 13)),
                    unOcupante(), LocalTime.of(15, 0));
        } catch (ReservaSolapadaException excepcion) {
            lanzoSolapamiento = true;
        }
        comprobar("Estancias que comparten noche lanzan ReservaSolapada", lanzoSolapamiento);

        // Capacidad excedida.
        boolean lanzoCapacidad = false;
        try {
            ListaEnlazada<Ocupante> muchos = new ListaEnlazada<>();
            for (int i = 0; i < 5; i++) {
                muchos.agregar(new Ocupante("Persona " + i, LocalDate.of(1990, 1, 1)));
            }
            sistema.crearReservaDirecta(titular, sistema.buscarApartamento("P-02"),
                    new Estancia(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2)),
                    muchos, LocalTime.of(15, 0));
        } catch (CapacidadExcedidaException excepcion) {
            lanzoCapacidad = true;
        }
        comprobar("Capacidad excedida lanza excepción", lanzoCapacidad);

        // Cancelar libera las noches.
        sistema.cancelarReserva(primera.getCodigo(), "Prueba de liberación");
        boolean reutilizable = true;
        try {
            sistema.crearReservaDirecta(titular, apartamento,
                    new Estancia(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)),
                    unOcupante(), LocalTime.of(15, 0));
        } catch (ReservaSolapadaException excepcion) {
            reutilizable = false;
        }
        comprobar("Cancelar libera las noches", reutilizable);
    }

    private static void probarCicloDeVida() {
        System.out.println("[8] Ciclo de vida de la reserva");
        SistemaAlojamiento sistema = sistemaMinimo();
        Huesped titular = sistema.buscarHuesped("100");
        Apartamento apartamento = sistema.buscarApartamento("P-01");

        Reserva pendiente = sistema.crearReservaPortal(titular, apartamento,
                new Estancia(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)),
                unOcupante(), LocalTime.of(15, 0));
        comprobar("Reserva del portal nace Pendiente",
                pendiente.getEstado() == EstadoReserva.PENDIENTE);

        boolean lanzoSinAnticipo = false;
        try {
            sistema.getGestorReservas().confirmarReserva(pendiente.getCodigo());
        } catch (TransicionEstadoInvalidaException excepcion) {
            lanzoSinAnticipo = true;
        }
        comprobar("Confirmar sin anticipo lanza excepción", lanzoSinAnticipo);

        sistema.registrarAnticipo(pendiente.getCodigo(), pendiente.anticipoRequerido());
        comprobar("Anticipo del 30 % confirma la reserva",
                pendiente.getEstado() == EstadoReserva.CONFIRMADA);

        // Check-in sin apartamento preparado.
        Apartamento otro = sistema.buscarApartamento("P-02");
        Reserva segunda = sistema.crearReservaDirecta(sistema.buscarHuesped("100"), otro,
                new Estancia(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                unOcupante(), LocalTime.of(15, 0));
        sistema.registrarCheckIn(segunda.getCodigo());
        comprobar("Check-in deja el apartamento Ocupado",
                otro.getEstado() == EstadoApartamento.OCUPADO);
        sistema.registrarCheckOut(segunda.getCodigo());
        comprobar("Check-out deja la reserva Finalizada",
                segunda.getEstado() == EstadoReserva.FINALIZADA);
        comprobar("Check-out deja el apartamento Pendiente de preparación",
                otro.getEstado() == EstadoApartamento.PENDIENTE_PREPARACION);

        boolean lanzoCheckIn = false;
        try {
            Reserva tercera = sistema.crearReservaDirecta(sistema.buscarHuesped("100"), otro,
                    new Estancia(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4)),
                    unOcupante(), LocalTime.of(15, 0));
            sistema.registrarCheckIn(tercera.getCodigo());
        } catch (TransicionEstadoInvalidaException excepcion) {
            lanzoCheckIn = true;
        }
        comprobar("No se puede hacer check-in sin preparar el apartamento", lanzoCheckIn);

        // Preparación: la tarea está en la cola y al completarla el apartamento queda
        // Preparado.
        sistema.getGestorPreparacion().iniciar(otro);
        sistema.getGestorPreparacion().completar(otro);
        comprobar("Completar preparación deja el apartamento Preparado",
                otro.getEstado() == EstadoApartamento.PREPARADO);
    }

    private static void probarCanalExterno() {
        System.out.println("[9] Canal externo y conflictos");
        SistemaAlojamiento sistema = sistemaMinimo();
        Huesped titular = sistema.buscarHuesped("100");
        Apartamento apartamento = sistema.buscarApartamento("P-01");

        sistema.crearReservaDirecta(titular, apartamento,
                new Estancia(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12)),
                unOcupante(), LocalTime.of(15, 0));

        SolicitudExterna buena = new SolicitudExterna("EXT-1", "Solicitante Uno",
                "500", "uno@correo.com", "300", "P-01",
                new Estancia(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 14)), 1,
                LocalDateTime.now());
        SolicitudExterna conflictiva = new SolicitudExterna("EXT-2", "Solicitante Dos",
                "501", "dos@correo.com", "301", "P-01",
                new Estancia(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 13)), 1,
                LocalDateTime.now());

        sistema.encolarSolicitudExterna(buena);
        sistema.encolarSolicitudExterna(conflictiva);
        comprobar("Solicitudes en cola FIFO",
                sistema.getProcesadorCanalExterno().numeroSolicitudesPendientes() == 2);

        ListaEnlazada<Reserva> creadas = sistema.procesarCanalExterno();
        comprobar("Se crea la solicitud sin conflicto", creadas.tamano() == 1);
        comprobar("Se registra un conflicto de canal",
                sistema.getProcesadorCanalExterno().numeroConflictos() == 1);
        comprobar("La solicitud conflictiva queda rechazada",
                conflictiva.getEstado() == SolicitudExterna.EstadoSolicitud.RECHAZADA);
        comprobar("La primera solicitud queda aceptada",
                buena.getEstado() == SolicitudExterna.EstadoSolicitud.ACEPTADA);
    }

    private static void probarBacktracking() {
        System.out.println("[10] Asignación de grupos grandes (backtracking)");
        SistemaAlojamiento sistema = new SistemaAlojamiento();
        int[] capacidades = {2, 2, 4, 4, 6, 8};
        for (int i = 0; i < capacidades.length; i++) {
            Apartamento apartamento = new Apartamento("G-0" + (i + 1), "Grupo " + i, "G", 1,
                    1, capacidades[i], 0, 0);
            apartamento.definirTarifa(Temporada.BAJA, 100000);
            sistema.registrarApartamento(apartamento);
        }

        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
        ListaEnlazada<Apartamento> combinacion = sistema.asignarGrupo(estancia, 10);
        int capacidadTotal = 0;
        for (Apartamento apartamento : combinacion) {
            capacidadTotal += apartamento.getCapacidadMaxima();
        }
        comprobar("Backtracking encuentra combinación suficiente", capacidadTotal >= 10);
        comprobar("Backtracking usa el mínimo de apartamentos (2)",
                combinacion.tamano() == 2);

        boolean lanzo = false;
        try {
            sistema.asignarGrupo(estancia, 100);
        } catch (exceptions.SinAlojamientoDisponibleException excepcion) {
            lanzo = true;
        }
        comprobar("Grupo imposible lanza SinAlojamientoDisponible", lanzo);
    }

    private static void probarFolioYDeshacer() {
        System.out.println("[11] Folio: contra-movimientos y deshacer");
        SistemaAlojamiento sistema = sistemaMinimo();
        Huesped titular = sistema.buscarHuesped("100");
        Apartamento apartamento = sistema.buscarApartamento("P-01");
        Reserva reserva = sistema.crearReservaDirecta(titular, apartamento,
                new Estancia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)),
                unOcupante(), LocalTime.of(15, 0));

        double saldoInicial = sistema.getGestorFolios().saldo(reserva.getCodigo());
        sistema.getGestorFolios().registrarPago(reserva.getCodigo(), "Pago de prueba", 50000);
        double saldoConPago = sistema.getGestorFolios().saldo(reserva.getCodigo());
        comprobar("El pago reduce el saldo", saldoConPago < saldoInicial);

        int movimientosAntes = reserva.getFolio().numeroMovimientos();
        sistema.getGestorFolios().deshacerUltimaOperacion(reserva.getCodigo());
        comprobar("Deshacer agrega un movimiento inverso (no borra)",
                reserva.getFolio().numeroMovimientos() == movimientosAntes + 1);
        comprobar("Deshacer restaura el saldo",
                Math.abs(sistema.getGestorFolios().saldo(reserva.getCodigo()) - saldoInicial) < 0.01);
    }

    private static void probarEscenarioCompleto() {
        System.out.println("[12] Escenario completo");
        SistemaAlojamiento sistema = EscenarioPrueba.construir();

        comprobar("El escenario tiene 12 apartamentos",
                sistema.todosApartamentos().tamano() == 12);
        comprobar("El escenario tiene huéspedes", sistema.todosHuespedes().tamano() >= 6);
        comprobar("El escenario tiene reservas", sistema.todasReservas().tamano() >= 7);
        comprobar("El escenario tiene conflictos de canal",
                sistema.getServicioEstadisticas().numeroConflictosCanal() == 1);
        comprobar("Hay apartamentos pendientes de preparar",
                sistema.getServicioEstadisticas().apartamentosPorPreparar() >= 1);

        // Grafo del edificio.
        GestorEdificio edificio = sistema.getGestorEdificio();
        comprobar("El mapa tiene 17 nodos", edificio.numeroNodos() == 17);
        comprobar("El mapa por defecto no tiene ciclos", !edificio.tieneCiclo());

        Grafo.Ruta<String> ruta = edificio.rutaRecepcionApartamento("A-104");
        comprobar("Ruta recepción → A-104 existe", ruta.esAlcanzable());
        comprobar("Distancia recepción → A-104 = 125 m", ruta.getDistancia() == 125);

        comprobar("Ningún apartamento inalcanzable al inicio",
                edificio.apartamentosInalcanzables().estaVacia());

        // Cerrar el pasillo del bloque C deja sus apartamentos inalcanzables.
        edificio.cerrarPasillo(GestorEdificio.RECEPCION, "SERV-ESC-C");
        comprobar("Cerrar el pasillo aísla el bloque C",
                edificio.apartamentosInalcanzables().tamano() == 4);
        edificio.abrirPasillo(GestorEdificio.RECEPCION, "SERV-ESC-C");
        comprobar("Reabrir el pasillo reconecta el bloque C",
                edificio.apartamentosInalcanzables().estaVacia());

        // Ruta de limpieza.
        ListaEnlazada<Apartamento> rutaLimpieza = sistema.rutaDeLimpieza();
        comprobar("La ruta de limpieza incluye los pendientes",
                rutaLimpieza.tamano() == sistema.getGestorPreparacion().numeroTareasPendientes());

        // Estadísticas.
        comprobar("La ocupación de hoy es mayor que cero",
                sistema.getServicioEstadisticas().porcentajeOcupacion(LocalDate.now()) > 0);
        comprobar("Hay ingresos registrados",
                sistema.getServicioEstadisticas().ingresoTotal() > 0);
        comprobar("Hay ingresos en más de una temporada",
                contarPositivos(sistema.getServicioEstadisticas().ingresosPorTemporada()) >= 2);

        // Turno de recepción circular.
        comprobar("Hay un recepcionista de turno",
                sistema.recepcionistaDeTurno() != null);
        Empleado primero = sistema.recepcionistaDeTurno();
        Empleado siguiente = sistema.rotarTurnoRecepcion();
        comprobar("El turno rota al siguiente recepcionista",
                siguiente != null && !siguiente.getDocumento().equals(primero.getDocumento())
                        && sistema.recepcionistaDeTurno().equals(siguiente));
    }

    private static void probarAdministracionYReportes() {
        System.out.println("[13] Administración, navegación y reportes");

        // Navegación "atrás" con una pila.
        HistorialNavegacion navegacion = new HistorialNavegacion();
        navegacion.visitar("Tablero");
        navegacion.visitar("Reservas");
        navegacion.visitar("Mapa");
        comprobar("Atrás vuelve a la pantalla anterior", "Reservas".equals(navegacion.atras()));
        comprobar("Atrás apila de nuevo hasta agotar el historial",
                "Tablero".equals(navegacion.atras()) && navegacion.atras() == null
                        && !navegacion.puedeVolver());

        // Empleados en un árbol de búsqueda por documento.
        SistemaAlojamiento sistema = sistemaMinimo();
        sistema.registrarEmpleado(new Empleado("30", "Carla", Empleado.Cargo.RECEPCIONISTA));
        sistema.registrarEmpleado(new Empleado("10", "Ana", Empleado.Cargo.ADMINISTRADOR));
        sistema.registrarEmpleado(new Empleado("20", "Beto", Empleado.Cargo.RECEPCIONISTA));
        sistema.registrarEmpleado(new Empleado("20", "Duplicado", Empleado.Cargo.RECEPCIONISTA));
        comprobar("Los empleados quedan en un árbol sin duplicados",
                sistema.getArbolEmpleados().tamano() == 3);
        comprobar("Empleados ordenados por documento (Comparable)",
                "10".equals(sistema.getEmpleados().obtener(0).getDocumento())
                        && "30".equals(sistema.getEmpleados().obtener(2).getDocumento()));
        comprobar("Buscar empleado por documento", "Beto".equals(sistema.buscarEmpleado("20").getNombre()));
        sistema.actualizarEmpleado("10", "Ana", Empleado.Cargo.RECEPCIONISTA);
        sistema.eliminarEmpleado("30");
        int enTurno = 0;
        Empleado inicio = sistema.recepcionistaDeTurno();
        Empleado cursor = inicio;
        do {
            enTurno++;
            cursor = sistema.rotarTurnoRecepcion();
        } while (cursor != null && !cursor.equals(inicio) && enTurno < 10);
        comprobar("El turno circular sigue los cambios de cargo y bajas", enTurno == 2
                && sistema.getArbolEmpleados().tamano() == 2);

        // Baja de apartamentos y huéspedes.
        Apartamento ocupado = sistema.buscarApartamento("P-01");
        Huesped titular = sistema.buscarHuesped("100");
        sistema.crearReservaDirecta(titular, ocupado,
                new Estancia(LocalDate.now().plusDays(5), LocalDate.now().plusDays(7)),
                unOcupante(), LocalTime.of(15, 0));
        boolean rechazoApartamento = false;
        try {
            sistema.eliminarApartamento("P-01");
        } catch (IllegalStateException excepcion) {
            rechazoApartamento = true;
        }
        comprobar("No se elimina un apartamento con reservas", rechazoApartamento);
        sistema.eliminarApartamento("P-02");
        comprobar("Se elimina un apartamento sin reservas del catálogo y del mapa",
                sistema.getGestorReservas().buscarApartamentoOpcional("P-02") == null
                        && sistema.getGestorEdificio().getGrafo().buscarNodo("P-02") == null);
        boolean rechazoHuesped = false;
        try {
            sistema.eliminarHuesped("100");
        } catch (IllegalStateException excepcion) {
            rechazoHuesped = true;
        }
        sistema.registrarHuesped(new Huesped("200", "Sin reservas", null, null, null));
        sistema.eliminarHuesped("200");
        comprobar("Solo se elimina un huésped sin reservas",
                rechazoHuesped && sistema.buscarHuespedOpcional("200") == null);

        // Devolución al cancelar: completa antes de la entrada, nula el día del ingreso.
        Reserva futura = sistema.crearReservaPortal(titular, ocupado,
                new Estancia(LocalDate.now().plusDays(20), LocalDate.now().plusDays(22)),
                unOcupante(), LocalTime.of(15, 0));
        sistema.registrarAnticipo(futura.getCodigo(), futura.anticipoRequerido());
        double devolucionFutura = futura.montoDevolucion();
        sistema.cancelarReserva(futura.getCodigo(), "prueba");
        comprobar("Cancelar antes de la entrada devuelve el anticipo",
                devolucionFutura > 0 && Math.abs(devolucionFutura - futura.getAnticipoPagado()) < 0.01);
        Reserva dehoy = sistema.crearReservaPortal(titular, ocupado,
                new Estancia(LocalDate.now(), LocalDate.now().plusDays(1)),
                unOcupante(), LocalTime.of(15, 0));
        sistema.registrarAnticipo(dehoy.getCodigo(), dehoy.anticipoRequerido());
        int movimientosAntes = dehoy.getFolio().numeroMovimientos();
        comprobar("El día del ingreso la devolución estimada es cero", dehoy.montoDevolucion() == 0);
        sistema.cancelarReserva(dehoy.getCodigo(), "prueba");
        comprobar("Cancelar el día del ingreso no registra devolución en el folio",
                dehoy.getFolio().numeroMovimientos() == movimientosAntes);

        // Temporadas: se agregan y se quitan periodos.
        SistemaAlojamiento reportes = sistemaMinimo();
        LocalDate hoy = LocalDate.now();
        reportes.getServicioTemporadas().registrar(Temporada.ALTA, hoy.plusDays(3), hoy.plusDays(6));
        CalendarioTemporadas.Periodo periodo = reportes.getServicioTemporadas().periodos().obtener(0);
        comprobar("Un periodo define la temporada de sus fechas",
                reportes.getServicioTemporadas().temporadaDe(hoy.plusDays(4)) == Temporada.ALTA);
        // Estancia de 4 noches: 2 en temporada baja y 2 en alta (días 1-2 y 3-4 desde hoy+1... ).
        Apartamento unidad = reportes.buscarApartamento("P-01");
        reportes.crearReservaDirecta(reportes.buscarHuesped("100"), unidad,
                new Estancia(hoy.plusDays(1), hoy.plusDays(5)), unOcupante(), LocalTime.of(15, 0));
        ServicioEstadisticas estadisticas = reportes.getServicioEstadisticas();
        int[] noches = estadisticas.nochesOcupadasPorTemporada();
        comprobar("Ocupación por temporada reparte las noches (2 baja, 2 alta)",
                noches[Temporada.BAJA.ordinal()] == 2 && noches[Temporada.ALTA.ordinal()] == 2);
        double[] porcentajes = estadisticas.porcentajeOcupacionPorTemporada();
        comprobar("El porcentaje por temporada usa las noches y los apartamentos",
                Math.abs(porcentajes[Temporada.ALTA.ordinal()] - 50.0) < 0.01);
        double totalPorTemporada = 0;
        for (double valor : estadisticas.ingresosPorTemporada()) {
            totalPorTemporada += valor;
        }
        double totalPorMes = 0;
        int nochesPorMes = 0;
        for (ServicioEstadisticas.IngresoPeriodo mes : estadisticas.ingresosPorMes()) {
            totalPorMes += mes.getIngreso();
            nochesPorMes += mes.getNoches();
        }
        comprobar("Los ingresos por mes suman lo mismo que por temporada",
                nochesPorMes == 4 && Math.abs(totalPorMes - totalPorTemporada) < 0.01);
        comprobar("Ingresos entre dos fechas incluye solo esas noches",
                Math.abs(estadisticas.ingresosEntre(hoy.plusDays(5), hoy.plusDays(5)) - 0) < 0.01
                        && estadisticas.ingresosEntre(hoy.plusDays(1), hoy.plusDays(4)) > 0);
        reportes.getServicioTemporadas().eliminar(periodo);
        comprobar("Quitar el periodo devuelve las fechas a temporada baja",
                reportes.getServicioTemporadas().temporadaDe(hoy.plusDays(4)) == Temporada.BAJA);
    }

    private static int contarPositivos(double[] valores) {
        int cantidad = 0;
        for (double valor : valores) {
            if (valor > 0) {
                cantidad++;
            }
        }
        return cantidad;
    }

    // ------------------------------------------------------------------
    // Utilidades de prueba
    // ------------------------------------------------------------------

    private static SistemaAlojamiento sistemaMinimo() {
        SistemaAlojamiento sistema = new SistemaAlojamiento();
        Apartamento primero = new Apartamento("P-01", "Prueba 1", "P", 1, 1, 2, 0, 0);
        primero.definirTarifa(Temporada.BAJA, 100000);
        primero.definirTarifa(Temporada.MEDIA, 150000);
        primero.definirTarifa(Temporada.ALTA, 200000);
        sistema.registrarApartamento(primero);

        Apartamento segundo = new Apartamento("P-02", "Prueba 2", "P", 1, 2, 4, 0, 0);
        segundo.definirTarifa(Temporada.BAJA, 180000);
        segundo.definirTarifa(Temporada.MEDIA, 220000);
        segundo.definirTarifa(Temporada.ALTA, 300000);
        sistema.registrarApartamento(segundo);

        sistema.registrarHuesped(new Huesped("100", "Huésped de Prueba",
                LocalDate.of(1990, 1, 1), "300", "prueba@correo.com"));
        return sistema;
    }

    private static ListaEnlazada<Ocupante> unOcupante() {
        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        ocupantes.agregar(new Ocupante("Huésped de Prueba", LocalDate.of(1990, 1, 1), true));
        return ocupantes;
    }

    private static void comprobar(String nombre, boolean condicion) {
        comprobaciones++;
        if (condicion) {
            System.out.println("  ok   " + nombre);
        } else {
            fallos++;
            System.out.println("  FALLA " + nombre);
        }
    }
}
