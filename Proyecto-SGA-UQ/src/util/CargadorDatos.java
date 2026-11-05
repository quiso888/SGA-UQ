package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

import datastructures.ListaEnlazada;
import model.Apartamento;
import model.CanalReserva;
import model.Empleado;
import model.Estancia;
import model.EstadoReserva;
import model.Huesped;
import model.Ocupante;
import model.Reserva;
import model.Temporada;
import service.SistemaAlojamiento;

/**
 * Cargador de datos del SGA: lee un escenario desde un archivo de texto plano
 * separado por barras verticales.
 *
 * TEMPORADA|ALTA|2026-12-15|2027-01-15
 * APARTAMENTO|A-101|Suite Bambú|A|1|1|2|110|120|120000|160000|210000|WiFi;TV
 * EMPLEADO|1001|Camila Restrepo|RECEPCIONISTA
 * HUESPED|1020304|Ana María Rodríguez|1990-05-12|3001234567|ana@correo.com
 * NODO|SERV-RECEPCION|320|480
 * PASILLO|SERV-RECEPCION|SERV-ESC-A|45
 * RESERVA|1020304|A-101|2026-11-01|2026-11-03|DIRECTA|15:30|Ana María Rodríguez|1990-05-12|1
 *
 * Las órdenes se aplican en un orden seguro para las dependencias: primero las
 * temporadas, luego los apartamentos, el mapa, los empleados, los huéspedes y por
 * último las reservas
 */
public final class CargadorDatos {

    private CargadorDatos() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Carga un escenario desde un archivo de texto.
     *
     * @throws IllegalArgumentException si el archivo no existe o tiene órdenes
     *                                  incompletas
     */
    public static SistemaAlojamiento cargar(File archivo) {
        if (archivo == null || !archivo.exists()) {
            throw new IllegalArgumentException("El archivo de datos no existe.");
        }
        ListaEnlazada<String> lineas = leerLineas(archivo);
        return cargar(lineas);
    }

    /**
     * Carga un escenario a partir de las líneas del archivo.
     */
    public static SistemaAlojamiento cargar(ListaEnlazada<String> lineas) {
        SistemaAlojamiento sistema = new SistemaAlojamiento();

        ListaEnlazada<String[]> apartamentos = new ListaEnlazada<>();
        ListaEnlazada<String[]> empleados = new ListaEnlazada<>();
        ListaEnlazada<String[]> huespedes = new ListaEnlazada<>();
        ListaEnlazada<String[]> nodos = new ListaEnlazada<>();
        ListaEnlazada<String[]> pasillos = new ListaEnlazada<>();
        ListaEnlazada<String[]> reservas = new ListaEnlazada<>();

        for (String linea : lineas) {
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            String[] campos = limpia.split("\\|", -1);
            String orden = campos[0].trim().toUpperCase();
            switch (orden) {
                case "TEMPORADA":
                    aplicarTemporada(sistema, campos);
                    break;
                case "APARTAMENTO":
                    apartamentos.agregar(campos);
                    break;
                case "EMPLEADO":
                    empleados.agregar(campos);
                    break;
                case "HUESPED":
                    huespedes.agregar(campos);
                    break;
                case "NODO":
                    nodos.agregar(campos);
                    break;
                case "PASILLO":
                    pasillos.agregar(campos);
                    break;
                case "RESERVA":
                    reservas.agregar(campos);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Orden desconocida en el archivo de datos: " + orden);
            }
        }

        for (String[] campos : apartamentos) {
            sistema.registrarApartamento(crearApartamento(campos));
        }
        for (String[] campos : nodos) {
            aplicarNodo(sistema, campos);
        }
        for (String[] campos : pasillos) {
            aplicarPasillo(sistema, campos);
        }
        for (String[] campos : empleados) {
            sistema.registrarEmpleado(new Empleado(valor(campos, 1), valor(campos, 2),
                    Empleado.Cargo.valueOf(valor(campos, 3).toUpperCase())));
        }
        for (String[] campos : huespedes) {
            sistema.registrarHuesped(crearHuesped(campos));
        }
        for (String[] campos : reservas) {
            aplicarReserva(sistema, campos);
        }
        return sistema;
    }

    // ------------------------------------------------------------------
    // Órdenes individuales
    // ------------------------------------------------------------------

    private static void aplicarTemporada(SistemaAlojamiento sistema, String[] campos) {
        sistema.getServicioTemporadas().registrar(Temporada.valueOf(valor(campos, 1).toUpperCase()),
                LocalDate.parse(valor(campos, 2)), LocalDate.parse(valor(campos, 3)));
    }

    private static Apartamento crearApartamento(String[] campos) {
        Apartamento apartamento = new Apartamento(valor(campos, 1), valor(campos, 2),
                valor(campos, 3), entero(campos, 4), entero(campos, 5), entero(campos, 6),
                decimal(campos, 7), decimal(campos, 8));
        apartamento.definirTarifa(Temporada.BAJA, decimal(campos, 9));
        apartamento.definirTarifa(Temporada.MEDIA, decimal(campos, 10));
        apartamento.definirTarifa(Temporada.ALTA, decimal(campos, 11));
        if (campos.length > 12 && !valor(campos, 12).isEmpty()) {
            for (String caracteristica : valor(campos, 12).split(";")) {
                apartamento.agregarCaracteristica(caracteristica);
            }
        }
        return apartamento;
    }

    private static Huesped crearHuesped(String[] campos) {
        return new Huesped(valor(campos, 1), valor(campos, 2),
                fecha(campos, 3), valor(campos, 4), valor(campos, 5));
    }

    private static void aplicarNodo(SistemaAlojamiento sistema, String[] campos) {
        sistema.getGestorEdificio().registrarInstalacion(valor(campos, 1),
                decimal(campos, 2), decimal(campos, 3));
    }

    private static void aplicarPasillo(SistemaAlojamiento sistema, String[] campos) {
        sistema.getGestorEdificio().conectar(valor(campos, 1), valor(campos, 2),
                entero(campos, 3));
    }

    private static void aplicarReserva(SistemaAlojamiento sistema, String[] campos) {
        Huesped titular = sistema.buscarHuesped(valor(campos, 1));
        Apartamento apartamento = sistema.buscarApartamento(valor(campos, 2));
        Estancia estancia = new Estancia(LocalDate.parse(valor(campos, 3)),
                LocalDate.parse(valor(campos, 4)));
        CanalReserva canal = CanalReserva.valueOf(valor(campos, 5).toUpperCase());
        LocalTime hora = LocalTime.parse(valor(campos, 6));
        String nombreOcupante = campos.length > 7 && !valor(campos, 7).isEmpty()
                ? valor(campos, 7) : titular.getNombre();
        LocalDate nacimiento = campos.length > 8 && !valor(campos, 8).isEmpty()
                ? LocalDate.parse(valor(campos, 8))
                : (titular.getFechaNacimiento() == null
                ? LocalDate.now().minusYears(30) : titular.getFechaNacimiento());
        int cantidad = campos.length > 9 && !valor(campos, 9).isEmpty()
                ? entero(campos, 9) : 1;

        ListaEnlazada<Ocupante> ocupantes = new ListaEnlazada<>();
        ocupantes.agregar(new Ocupante(nombreOcupante, nacimiento, true));
        for (int i = 1; i < cantidad; i++) {
            ocupantes.agregar(new Ocupante("Acompañante " + i, nacimiento, false));
        }

        Reserva reserva;
        if (canal == CanalReserva.PORTAL) {
            reserva = sistema.crearReservaPortal(titular, apartamento, estancia, ocupantes, hora);
        } else if (canal == CanalReserva.DIRECTA) {
            reserva = sistema.crearReservaDirecta(titular, apartamento, estancia, ocupantes, hora);
        } else {
            throw new IllegalArgumentException(
                    "Las reservas del canal externo se cargan como solicitudes.");
        }

        // Estado final opcional: nombre del estado entre paréntesis al final.
        if (campos.length > 10 && !valor(campos, 10).isEmpty()) {
            EstadoReserva estado = EstadoReserva.valueOf(valor(campos, 10).toUpperCase());
            if (estado == EstadoReserva.CONFIRMADA
                    && reserva.getEstado() == EstadoReserva.PENDIENTE) {
                double anticipo = reserva.anticipoRequerido();
                if (anticipo > 0) {
                    sistema.registrarAnticipo(reserva.getCodigo(), anticipo);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Lectura y conversión
    // ------------------------------------------------------------------

    /**
     * Lee un archivo de texto en UTF-8, línea por línea, usando la lista propia del
     * SGA (sin colecciones de {@code java.util}).
     */
    private static ListaEnlazada<String> leerLineas(File archivo) {
        ListaEnlazada<String> lineas = new ListaEnlazada<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.agregar(linea);
            }
        } catch (IOException excepcion) {
            throw new IllegalArgumentException(
                    "No se pudo leer el archivo: " + excepcion.getMessage(), excepcion);
        }
        return lineas;
    }

    private static String valor(String[] campos, int indice) {
        return indice < campos.length ? campos[indice].trim() : "";
    }

    private static int entero(String[] campos, int indice) {
        return Integer.parseInt(valor(campos, indice));
    }

    private static double decimal(String[] campos, int indice) {
        return Double.parseDouble(valor(campos, indice));
    }

    private static LocalDate fecha(String[] campos, int indice) {
        String texto = valor(campos, indice);
        return texto.isEmpty() ? null : LocalDate.parse(texto);
    }
}
