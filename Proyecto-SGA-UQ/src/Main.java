import api.ServidorHttp;
import gui.VentanaPrincipal;
import service.SistemaAlojamiento;
import util.Autocomprobacion;
import util.EscenarioPrueba;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;

/**
 * Punto de entrada del Sistema de Gestión de Alojamiento (SGA) de Aparta-Suites
 * UQ (Salento, Quindío).
 *
 * <p>Sin argumentos abre la interfaz de escritorio Swing. La interfaz web React
 * (servidor local + API JSON) queda disponible con {@code --web}.</p>
 *
 * <ul>
 *   <li>{@code --consola}: ejecuta la autocomprobación de reglas y estructuras.</li>
 *   <li>{@code --web}: sirve el frontend React y la API JSON en el navegador.</li>
 *   <li>{@code --puerto=8081}: cambia el puerto del servidor web (8080 por defecto).</li>
 *   <li>{@code --sin-navegador}: inicia el servidor sin abrir el navegador.</li>
 *   <li>{@code --vacio}: inicia un sistema sin datos de ejemplo.</li>
 * </ul>
 */
public final class Main {

    private static final int PUERTO_POR_DEFECTO = 8080;

    private Main() {
        // Clase de entrada: no se instancia.
    }

    public static void main(String[] argumentos) {
        boolean modoConsola = false;
        boolean modoWeb = false;
        boolean abrirNavegador = true;
        boolean cargarEscenario = true;
        boolean ayuda = false;
        int puerto = PUERTO_POR_DEFECTO;

        if (argumentos != null) {
            for (int i = 0; i < argumentos.length; i++) {
                String argumento = argumentos[i];
                if (argumento == null) {
                    continue;
                }
                String minuscula = argumento.toLowerCase();
                if ("--consola".equals(minuscula) || "-c".equals(minuscula)) {
                    modoConsola = true;
                } else if ("--web".equals(minuscula) || "-w".equals(minuscula)) {
                    modoWeb = true;
                } else if ("--swing".equals(minuscula) || "-s".equals(minuscula)) {
                    modoWeb = false;
                } else if ("--sin-navegador".equals(minuscula)
                        || "--no-browser".equals(minuscula)) {
                    abrirNavegador = false;
                } else if ("--vacio".equals(minuscula) || "--empty".equals(minuscula)) {
                    cargarEscenario = false;
                } else if ("--ayuda".equals(minuscula) || "--help".equals(minuscula)
                        || "-h".equals(minuscula)) {
                    ayuda = true;
                } else if (minuscula.startsWith("--puerto=")) {
                    puerto = Integer.parseInt(argumento.substring("--puerto=".length()).trim());
                } else if ("--puerto".equals(minuscula) && i + 1 < argumentos.length) {
                    puerto = Integer.parseInt(argumentos[++i].trim());
                }
            }
        }

        if (ayuda) {
            imprimirAyuda();
            return;
        }

        if (modoConsola) {
            boolean correcto = Autocomprobacion.ejecutar();
            if (!correcto) {
                System.exit(1);
            }
            return;
        }

        if (modoWeb) {
            iniciarWeb(puerto, cargarEscenario, abrirNavegador);
            return;
        }

        abrirEscritorio(cargarEscenario);
    }

    private static void abrirEscritorio(boolean cargarEscenario) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SistemaAlojamiento sistema = cargarEscenario
                        ? EscenarioPrueba.construir() : new SistemaAlojamiento();
                VentanaPrincipal ventana = new VentanaPrincipal(sistema);
                ventana.setVisible(true);
            }
        });
    }

    private static void iniciarWeb(int puerto, boolean cargarEscenario, boolean abrirNavegador) {
        SistemaAlojamiento sistema = cargarEscenario
                ? EscenarioPrueba.construir() : new SistemaAlojamiento();
        File carpetaWeb = new File("web/dist");
        try {
            ServidorHttp servidor = new ServidorHttp(puerto, sistema, carpetaWeb);
            servidor.iniciar();

            System.out.println();
            System.out.println("  SGA · Aparta-Suites UQ · Salento, Quindío");
            System.out.println("  Interfaz web: " + servidor.getUrl());
            System.out.println("  API JSON:     " + servidor.getUrl() + "api/tablero");
            System.out.println("  Datos:        " + (cargarEscenario
                    ? "escenario de prueba" : "sistema vacío"));
            System.out.println();
            System.out.println("  Presione Ctrl+C para detener el servidor.");
            System.out.println();

            if (abrirNavegador) {
                abrirNavegador(servidor.getUrl());
            }
        } catch (java.net.BindException excepcion) {
            System.err.println("El puerto " + puerto + " ya está en uso.");
            System.err.println("Pruebe con: java -cp build Main --puerto=" + (puerto + 1));
        } catch (Exception excepcion) {
            System.err.println("No fue posible iniciar el servidor web:");
            excepcion.printStackTrace();
        }
    }

    private static void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception excepcion) {
            System.out.println("Abra manualmente esta dirección en el navegador: " + url);
        }
    }

    private static void imprimirAyuda() {
        System.out.println("Uso: java -cp build Main [opciones]");
        System.out.println();
        System.out.println("  (sin opciones)     Abre la interfaz de escritorio Swing.");
        System.out.println("  --web              Sirve la interfaz web React y la API en el puerto 8080.");
        System.out.println("  --puerto=8081      Cambia el puerto del servidor web.");
        System.out.println("  --sin-navegador    No abre el navegador automáticamente (solo con --web).");
        System.out.println("  --vacio            Inicia sin el escenario de prueba.");
        System.out.println("  --consola          Ejecuta la autocomprobación y termina.");
        System.out.println("  --ayuda            Muestra esta ayuda.");
    }
}
