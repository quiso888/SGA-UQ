package util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.Empleado;
import model.Estancia;
import model.Huesped;
import model.Ocupante;
import model.Reserva;
import model.SolicitudExterna;
import model.Temporada;
import service.GestorEdificio;
import service.SistemaAlojamiento;

/**
 * Construye un escenario de prueba completo del Aparta-Suites UQ (Salento,
 * Quindío) para poder usar la aplicación sin cargar datos a mano.
 *
 * El escenario contiene:
 *   12 apartamentos distribuidos en tres bloques (A, B y C), con tarifas para
 *       las temporadas Baja, Media y Alta.
 *   El mapa del edificio: recepción, escaleras, cuarto de aseo y los
 *       pasillos que los conectan, con sus distancias en metros.
 *   Un calendario de temporadas relativo a la fecha actual, para que los
 *       ejemplos sigan teniendo sentido sin importar cuándo se ejecute.
 *   Huéspedes, reservas en distintos estados del ciclo de vida y solicitudes del
 *       canal externo (incluida una que genera un conflicto de canal).
 *
 */
public final class EscenarioPrueba {

    /** Distancia en metros de cada pasillo del mapa por defecto. */
    private static final int PASILLO_CORTO = 20;
    private static final int PASILLO_MEDIO = 30;
    private static final int PASILLO_LARGO = 45;

    private EscenarioPrueba() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Construye y devuelve el sistema con el escenario de prueba cargado.
     */
    public static SistemaAlojamiento construir() {
        SistemaAlojamiento sistema = new SistemaAlojamiento();
        LocalDate hoy = LocalDate.now();

        configurarTemporadas(sistema, hoy);
        registrarApartamentos(sistema);
        construirMapa(sistema);
        registrarEmpleados(sistema);

        ListaEnlazada<Huesped> huespedes = registrarHuespedes(sistema);
        crearReservasDeEjemplo(sistema, huespedes, hoy);
        encolarSolicitudesExternas(sistema, hoy);
        return sistema;
    }

    // ------------------------------------------------------------------
    // Temporadas
    // ------------------------------------------------------------------

    /**
     * Define las temporadas del alojamiento en relación con la fecha actual: una
     * temporada alta dentro de tres semanas y una temporada media justo antes.
     */
    private static void configurarTemporadas(SistemaAlojamiento sistema, LocalDate hoy) {
        // Temporada media: de hoy + 10 a hoy + 24.
        sistema.getServicioTemporadas().registrar(Temporada.MEDIA,
                hoy.plusDays(10), hoy.plusDays(24));
        // Temporada alta: de hoy + 25 a hoy + 55.
        sistema.getServicioTemporadas().registrar(Temporada.ALTA,
                hoy.plusDays(25), hoy.plusDays(55));
        // El resto del año queda en temporada baja (tarifa base).
        sistema.getCalendario().setTemporadaPorDefecto(Temporada.BAJA);
    }

    // ------------------------------------------------------------------
    // Apartamentos
    // ------------------------------------------------------------------

    private static void registrarApartamentos(SistemaAlojamiento sistema) {
        // Bloque A — primer piso.
        sistema.registrarApartamento(apartamento("A-101", "Suite Bambú", "A", 1, 1, 2,
                110, 120, 120000, 160000, 210000, "WiFi", "TV", "Cafetera"));
        sistema.registrarApartamento(apartamento("A-102", "Suite Café", "A", 1, 1, 2,
                250, 120, 125000, 165000, 215000, "WiFi", "TV", "Balcón"));
        sistema.registrarApartamento(apartamento("A-103", "Suite Palma", "A", 1, 2, 4,
                390, 120, 180000, 230000, 300000, "WiFi", "TV", "Cocina", "Balcón"));
        sistema.registrarApartamento(apartamento("A-104", "Suite Quinde", "A", 1, 2, 4,
                530, 120, 175000, 225000, 290000, "WiFi", "TV", "Cocina"));

        // Bloque B — segundo piso.
        sistema.registrarApartamento(apartamento("B-201", "Suite Guadual", "B", 2, 1, 2,
                110, 250, 130000, 170000, 220000, "WiFi", "TV", "Vista al jardín"));
        sistema.registrarApartamento(apartamento("B-202", "Suite Yarumo", "B", 2, 2, 4,
                250, 250, 185000, 235000, 305000, "WiFi", "TV", "Cocina", "Jacuzzi"));
        sistema.registrarApartamento(apartamento("B-203", "Suite Heliconia", "B", 2, 2, 4,
                390, 250, 190000, 240000, 310000, "WiFi", "TV", "Cocina", "Balcón"));
        sistema.registrarApartamento(apartamento("B-204", "Suite Colibrí", "B", 2, 3, 6,
                530, 250, 240000, 300000, 390000, "WiFi", "TV", "Cocina", "Jacuzzi", "Balcón"));

        // Bloque C — tercer piso.
        sistema.registrarApartamento(apartamento("C-301", "Suite Mirador", "C", 3, 2, 4,
                110, 380, 195000, 245000, 315000, "WiFi", "TV", "Cocina", "Vista al valle"));
        sistema.registrarApartamento(apartamento("C-302", "Suite Fogón", "C", 3, 1, 2,
                250, 380, 135000, 175000, 225000, "WiFi", "TV", "Chimenea"));
        sistema.registrarApartamento(apartamento("C-303", "Suite Cafetal", "C", 3, 3, 6,
                390, 380, 245000, 305000, 395000, "WiFi", "TV", "Cocina", "Balcón", "Jacuzzi"));
        sistema.registrarApartamento(apartamento("C-304", "Suite Salento", "C", 3, 4, 8,
                530, 380, 320000, 400000, 520000, "WiFi", "TV", "Cocina", "Jacuzzi",
                "Balcón", "Vista al valle"));
    }

    private static Apartamento apartamento(String codigo, String nombre, String bloque, int piso,
                                           int dormitorios, int capacidad, double x, double y,
                                           double tarifaBaja, double tarifaMedia, double tarifaAlta,
                                           String... caracteristicas) {
        Apartamento apartamento = new Apartamento(codigo, nombre, bloque, piso, dormitorios,
                capacidad, x, y);
        apartamento.definirTarifa(Temporada.BAJA, tarifaBaja);
        apartamento.definirTarifa(Temporada.MEDIA, tarifaMedia);
        apartamento.definirTarifa(Temporada.ALTA, tarifaAlta);
        for (String caracteristica : caracteristicas) {
            apartamento.agregarCaracteristica(caracteristica);
        }
        return apartamento;
    }

    // ------------------------------------------------------------------
    // Mapa del edificio
    // ------------------------------------------------------------------

    /**
     * Construye un mapa en forma de árbol: la recepción conecta con las escaleras
     * de cada bloque y con el cuarto de aseo; cada escalera conecta con la cadena
     * de apartamentos de su bloque.
     */
    private static void construirMapa(SistemaAlojamiento sistema) {
        GestorEdificio edificio = sistema.getGestorEdificio();
        edificio.registrarInstalacion(GestorEdificio.RECEPCION, 320, 480);
        edificio.registrarInstalacion("SERV-ESC-A", 320, 120);
        edificio.registrarInstalacion("SERV-ESC-B", 320, 250);
        edificio.registrarInstalacion("SERV-ESC-C", 320, 380);
        edificio.registrarInstalacion("SERV-ASEO", 700, 480);

        // Recepción con escaleras y aseo.
        edificio.conectar(GestorEdificio.RECEPCION, "SERV-ESC-A", PASILLO_LARGO);
        edificio.conectar(GestorEdificio.RECEPCION, "SERV-ESC-B", PASILLO_MEDIO);
        edificio.conectar(GestorEdificio.RECEPCION, "SERV-ESC-C", PASILLO_CORTO);
        edificio.conectar(GestorEdificio.RECEPCION, "SERV-ASEO", PASILLO_CORTO);

        // Bloque A: escalera -> A-101 -> A-102 -> A-103 -> A-104.
        conectarCadena(edificio, "SERV-ESC-A", "A-101", "A-102", "A-103", "A-104");
        // Bloque B: escalera -> B-201 -> B-202 -> B-203 -> B-204.
        conectarCadena(edificio, "SERV-ESC-B", "B-201", "B-202", "B-203", "B-204");
        // Bloque C: escalera -> C-301 -> C-302 -> C-303 -> C-304.
        conectarCadena(edificio, "SERV-ESC-C", "C-301", "C-302", "C-303", "C-304");
    }

    private static void conectarCadena(GestorEdificio edificio, String escalera, String... codigos) {
        String anterior = escalera;
        for (String codigo : codigos) {
            edificio.conectar(anterior, codigo, PASILLO_CORTO);
            anterior = codigo;
        }
    }

    // ------------------------------------------------------------------
    // Empleados
    // ------------------------------------------------------------------

    private static void registrarEmpleados(SistemaAlojamiento sistema) {
        sistema.registrarEmpleado(new Empleado("1001", "Camila Restrepo",
                Empleado.Cargo.RECEPCIONISTA));
        sistema.registrarEmpleado(new Empleado("1002", "Andrés Salazar",
                Empleado.Cargo.ADMINISTRADOR));
        sistema.registrarEmpleado(new Empleado("1003", "Luisa Fernanda Gómez",
                Empleado.Cargo.AMA_DE_LLAVES));
        sistema.registrarEmpleado(new Empleado("1004", "Jorge Iván Marín",
                Empleado.Cargo.AMA_DE_LLAVES));
        sistema.registrarEmpleado(new Empleado("1005", "Natalia Cárdenas",
                Empleado.Cargo.RECEPCIONISTA));
    }

    // ------------------------------------------------------------------
    // Huéspedes
    // ------------------------------------------------------------------

    private static ListaEnlazada<Huesped> registrarHuespedes(SistemaAlojamiento sistema) {
        ListaEnlazada<Huesped> huespedes = new ListaEnlazada<>();
        huespedes.agregar(new Huesped("1.020-304.567", "Ana María Rodríguez",
                LocalDate.of(1990, 5, 12), "300 123 4567", "ana.rodriguez@correo.com"));
        huespedes.agregar(new Huesped("1.036-882.114", "Carlos Andrés Mejía",
                LocalDate.of(1985, 11, 3), "310 456 7890", "carlos.mejia@correo.com"));
        huespedes.agregar(new Huesped("43.211.908", "Diana Carolina Ospina",
                LocalDate.of(1993, 2, 27), "320 987 6543", "diana.ospina@correo.com"));
        huespedes.agregar(new Huesped("79.554.021", "Felipe Eduardo Ramírez",
                LocalDate.of(1979, 8, 19), "315 222 3344", "felipe.ramirez@correo.com"));
        huespedes.agregar(new Huesped("1.098-765.432", "María Fernanda Torres",
                LocalDate.of(1998, 4, 8), "301 555 7788", "maria.torres@correo.com"));
        huespedes.agregar(new Huesped("1.104-555.333", "Sebastián Gómez",
                LocalDate.of(1991, 12, 30), "304 111 2233", "sebastian.gomez@correo.com"));

        for (Huesped huesped : huespedes) {
            sistema.registrarHuesped(huesped);
        }
        return huespedes;
    }

    // ------------------------------------------------------------------
    // Reservas de ejemplo
    // ------------------------------------------------------------------

    private static void crearReservasDeEjemplo(SistemaAlojamiento sistema,
                                               ListaEnlazada<Huesped> huespedes,
                                               LocalDate hoy) {
        // 1) Reserva en curso: el grupo llegó hace dos días y se va en dos días.
        Reserva enCurso = sistema.crearReservaDirecta(huespedes.obtener(0),
                sistema.buscarApartamento("A-101"),
                new Estancia(hoy.minusDays(2), hoy.plusDays(2)),
                ocupantes(new Ocupante(huespedes.obtener(0).getNombre(),
                        LocalDate.of(1990, 5, 12), true)), LocalTime.of(15, 30));
        sistema.registrarCheckIn(enCurso.getCodigo());

        // 2) Reserva finalizada: salió hace dos días; el apartamento quedó pendiente
        //    de preparación y su tarea entró en la cola de prioridad.
        Reserva finalizada = sistema.crearReservaDirecta(huespedes.obtener(1),
                sistema.buscarApartamento("A-102"),
                new Estancia(hoy.minusDays(6), hoy.minusDays(2)),
                ocupantes(new Ocupante(huespedes.obtener(1).getNombre(),
                        LocalDate.of(1985, 11, 3), true)), LocalTime.of(14, 0));
        sistema.registrarCheckIn(finalizada.getCodigo());
        sistema.registrarCheckOut(finalizada.getCodigo());

        // 3) Reserva confirmada para dentro de una semana.
        Reserva confirmada = sistema.crearReservaDirecta(huespedes.obtener(2),
                sistema.buscarApartamento("B-202"),
                new Estancia(hoy.plusDays(7), hoy.plusDays(10)),
                ocupantes(
                        new Ocupante(huespedes.obtener(2).getNombre(),
                                LocalDate.of(1993, 2, 27), true),
                        new Ocupante("Sofía Ospina", LocalDate.of(2015, 6, 15))),
                LocalTime.of(16, 0));
        confirmada.getFolio().registrarPago("Pago total de la estancia",
                confirmada.getTotalEstimado());

        // 4) Reserva pendiente del portal: aún no ha pagado el anticipo del 30 %.
        sistema.crearReservaPortal(huespedes.obtener(3),
                sistema.buscarApartamento("C-301"),
                new Estancia(hoy.plusDays(14), hoy.plusDays(17)),
                ocupantes(new Ocupante(huespedes.obtener(3).getNombre(),
                        LocalDate.of(1979, 8, 19), true)), LocalTime.of(13, 30));

        // 5) Reserva cancelada: se libera el apartamento y se devuelve el anticipo.
        Reserva cancelada = sistema.crearReservaPortal(huespedes.obtener(4),
                sistema.buscarApartamento("C-302"),
                new Estancia(hoy.plusDays(21), hoy.plusDays(23)),
                ocupantes(new Ocupante(huespedes.obtener(4).getNombre(),
                        LocalDate.of(1998, 4, 8), true)), LocalTime.of(18, 0));
        sistema.registrarAnticipo(cancelada.getCodigo(), cancelada.anticipoRequerido());
        sistema.cancelarReserva(cancelada.getCodigo(),
                "El huésped cambió de planes de viaje.");

        // 6) Reserva directa de un grupo familiar grande en el apartamento mayor.
        sistema.crearReservaDirecta(huespedes.obtener(5),
                sistema.buscarApartamento("C-304"),
                new Estancia(hoy.plusDays(3), hoy.plusDays(6)),
                ocupantes(
                        new Ocupante(huespedes.obtener(5).getNombre(),
                                LocalDate.of(1991, 12, 30), true),
                        new Ocupante("Valentina Gómez", LocalDate.of(1994, 3, 21)),
                        new Ocupante("Emilio Gómez", LocalDate.of(2018, 9, 2)),
                        new Ocupante("Lucía Gómez", LocalDate.of(2021, 1, 14))),
                LocalTime.of(17, 0));

        // 7) Una reserva en temporada alta, para que las estadísticas por temporada
        //    tengan datos en las tres tarifas.
        sistema.crearReservaDirecta(huespedes.obtener(1),
                sistema.buscarApartamento("B-204"),
                new Estancia(hoy.plusDays(30), hoy.plusDays(34)),
                ocupantes(
                        new Ocupante(huespedes.obtener(1).getNombre(),
                                LocalDate.of(1985, 11, 3), true),
                        new Ocupante("Martín Mejía", LocalDate.of(2014, 10, 10))),
                LocalTime.of(15, 0));
    }

    private static ListaEnlazada<Ocupante> ocupantes(Ocupante... ocupantes) {
        ListaEnlazada<Ocupante> lista = new ListaEnlazada<>();
        for (Ocupante ocupante : ocupantes) {
            lista.agregar(ocupante);
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Canal externo
    // ------------------------------------------------------------------

    /**
     * Encola dos solicitudes de la plataforma externa:
     * una que se puede aceptar (apartamento libre en esas fechas),
     * una que choca con la reserva confirmada de B-202 y queda registrada como conflicto de canal.
     */
    private static void encolarSolicitudesExternas(SistemaAlojamiento sistema, LocalDate hoy) {
        sistema.encolarSolicitudExterna(new SolicitudExterna("EXT-2026-001",
                "Ricardo Peña", "1.152-334.889", "ricardo.pena@correo.com", "312 444 5566",
                "A-103", new Estancia(hoy.plusDays(3), hoy.plusDays(5)), 2,
                LocalDateTime.now()));

        // Esta solicitud coincide con la estancia confirmada de B-202.
        sistema.encolarSolicitudExterna(new SolicitudExterna("EXT-2026-002",
                "Paula Andrea Vélez", "1.143-220.775", "paula.velez@correo.com",
                "313 777 8899", "B-202", new Estancia(hoy.plusDays(8), hoy.plusDays(9)), 3,
                LocalDateTime.now()));

        // Se procesan: la primera se acepta y la segunda queda como conflicto.
        sistema.procesarCanalExterno();
    }
}
