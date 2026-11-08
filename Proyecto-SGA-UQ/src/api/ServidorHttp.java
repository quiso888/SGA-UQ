package api;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import service.SistemaAlojamiento;

/**
 * Servidor HTTP local del SGA: publica la API JSON y sirve la aplicación React
 * compilada en {web/dist}.
 *
 * Se usa el servidor incluido en el JDK (com.sun.net.httpserver}), sin
 * frameworks ni dependencias externas. Un único hilo atiende las peticiones para
 * proteger las estructuras de datos del núcleo.
 */
public final class ServidorHttp {

    private static final String TIPO_HTML = "text/html; charset=utf-8";
    private static final String TIPO_JS = "text/javascript; charset=utf-8";
    private static final String TIPO_CSS = "text/css; charset=utf-8";

    private final int puerto;
    private final ApiSga api;
    private final Path carpetaWeb;
    private final HttpServer servidor;
    private final ExecutorService ejecutor;

    public ServidorHttp(int puerto, SistemaAlojamiento sistema, File carpetaWeb)
            throws IOException {
        this.puerto = puerto;
        this.api = new ApiSga(sistema);
        this.carpetaWeb = carpetaWeb == null ? null : carpetaWeb.toPath().toAbsolutePath()
                .normalize();
        this.servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        this.ejecutor = Executors.newSingleThreadExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "sga-http");
            hilo.setDaemon(false);
            return hilo;
        });

        servidor.createContext("/api/", new HttpHandler() {
            @Override
            public void handle(HttpExchange intercambio) throws IOException {
                api.manejar(intercambio);
            }
        });
        servidor.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange intercambio) throws IOException {
                servirEstatico(intercambio);
            }
        });
        servidor.setExecutor(ejecutor);
    }

    public void iniciar() {
        servidor.start();
    }

    public void detener() {
        servidor.stop(0);
        ejecutor.shutdownNow();
    }

    public int getPuerto() {
        return puerto;
    }

    public String getUrl() {
        return "http://localhost:" + puerto + "/";
    }

    public ApiSga getApi() {
        return api;
    }

    // ------------------------------------------------------------------
    // Archivos estáticos
    // ------------------------------------------------------------------

    private void servirEstatico(HttpExchange intercambio) throws IOException {
        if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(intercambio.getRequestMethod())) {
            responderTexto(intercambio, 405, "Método no permitido.", "text/plain; charset=utf-8");
            return;
        }

        String ruta = intercambio.getRequestURI().getPath();
        if (ruta == null || ruta.isEmpty() || "/".equals(ruta)) {
            ruta = "/index.html";
        }

        if (carpetaWeb == null || !Files.isDirectory(carpetaWeb)) {
            responderTexto(intercambio, 200, paginaSinCompilar(), TIPO_HTML);
            return;
        }

        Path archivo = carpetaWeb.resolve(ruta.substring(1)).normalize();
        if (!archivo.startsWith(carpetaWeb)) {
            responderTexto(intercambio, 403, "Acceso denegado.", "text/plain; charset=utf-8");
            return;
        }

        if (!Files.isRegularFile(archivo)) {
            // Las rutas de la SPA se resuelven con index.html.
            Path indice = carpetaWeb.resolve("index.html");
            if (Files.isRegularFile(indice) && !tieneExtension(ruta)) {
                archivo = indice;
            } else {
                responderTexto(intercambio, 404, "Archivo no encontrado.",
                        "text/plain; charset=utf-8");
                return;
            }
        }

        byte[] contenido = Files.readAllBytes(archivo);
        String tipo = tipoContenido(archivo.getFileName().toString());
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        boolean esHtml = tipo.startsWith("text/html");
        intercambio.getResponseHeaders().set("Cache-Control",
                esHtml ? "no-cache" : "public, max-age=31536000, immutable");
        if ("HEAD".equalsIgnoreCase(intercambio.getRequestMethod())) {
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
            return;
        }
        intercambio.sendResponseHeaders(200, contenido.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(contenido);
        }
    }

    private static boolean tieneExtension(String ruta) {
        int barra = ruta.lastIndexOf('/');
        int punto = ruta.lastIndexOf('.');
        return punto > barra;
    }

    private static String tipoContenido(String nombre) {
        String minuscula = nombre.toLowerCase();
        if (minuscula.endsWith(".html")) {
            return TIPO_HTML;
        }
        if (minuscula.endsWith(".js") || minuscula.endsWith(".mjs")) {
            return TIPO_JS;
        }
        if (minuscula.endsWith(".css")) {
            return TIPO_CSS;
        }
        if (minuscula.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (minuscula.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (minuscula.endsWith(".png")) {
            return "image/png";
        }
        if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (minuscula.endsWith(".webp")) {
            return "image/webp";
        }
        if (minuscula.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (minuscula.endsWith(".woff2")) {
            return "font/woff2";
        }
        return "application/octet-stream";
    }

    private static void responderTexto(HttpExchange intercambio, int estado, String texto,
                                       String tipo) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static String paginaSinCompilar() {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>SGA · interfaz no compilada</title>"
                + "<style>body{font-family:system-ui,sans-serif;background:#f4f1ea;color:#263238;"
                + "max-width:720px;margin:60px auto;padding:0 24px;line-height:1.6}"
                + "code{background:#fff;padding:2px 6px;border-radius:4px;border:1px solid #ddd}"
                + ".nota{background:#fff7e0;border-left:4px solid #f9a825;padding:16px}</style></head>"
                + "<body><h1>SGA · Aparta-Suites UQ</h1>"
                + "<div class=\"nota\"><b>La interfaz React todavía no está compilada.</b><br>"
                + "Compile el frontend con <code>npm.cmd install</code> y "
                + "<code>npm.cmd run build</code> dentro de la carpeta <code>web</code>."
                + "</div>"
                + "<p>La API ya está disponible en <code>/api/tablero</code>, "
                + "<code>/api/reservas</code>, <code>/api/huespedes</code>, "
                + "<code>/api/calendario</code>, <code>/api/preparacion</code>, "
                + "<code>/api/mapa</code> y <code>/api/estadisticas</code>.</p>"
                + "</body></html>";
    }
}
