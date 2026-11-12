package api;

import datastructures.ListaEnlazada;

/**
 * Constructor y analizador JSON mínimo, sin dependencias externas y sin usar las
 * colecciones de { java.util}.
 *
 * La API web lo usa para leer los cuerpos de las peticiones y para escribir las
 * respuestas. Los objetos guardan sus campos en una {ListaEnlazada}, de modo
 * que toda la aplicación (núcleo y capa web) permanece fiel al requisito de
 * implementar las estructuras propias.
 */
public final class Json {

    private Json() {
        // Clase de utilidades.
    }

    // ------------------------------------------------------------------
    // Modelo de valores
    // ------------------------------------------------------------------

    /** Objeto JSON con campos ordenados por inserción. */
    public static final class Objeto {

        private static final class Campo {
            private final String clave;
            private final Object valor;

            Campo(String clave, Object valor) {
                this.clave = clave;
                this.valor = valor;
            }
        }

        private final ListaEnlazada<Campo> campos = new ListaEnlazada<>();

        public Objeto poner(String clave, Object valor) {
            if (clave == null) {
                throw new IllegalArgumentException("La clave JSON es obligatoria.");
            }
            campos.agregar(new Campo(clave, valor));
            return this;
        }

        public Objeto poner(String clave, String valor) {
            return poner(clave, (Object) valor);
        }

        public Objeto poner(String clave, double valor) {
            return poner(clave, (Object) valor);
        }

        public Objeto poner(String clave, int valor) {
            return poner(clave, (Object) valor);
        }

        public Objeto poner(String clave, long valor) {
            return poner(clave, (Object) valor);
        }

        public Objeto poner(String clave, boolean valor) {
            return poner(clave, (Object) valor);
        }

        public Objeto ponerNulo(String clave) {
            return poner(clave, (Object) null);
        }

        public boolean tiene(String clave) {
            return buscar(clave) != null;
        }

        public Object valor(String clave) {
            Campo campo = buscar(clave);
            return campo == null ? null : campo.valor;
        }

        public String texto(String clave) {
            Object valor = valor(clave);
            if (valor == null) {
                return null;
            }
            if (valor instanceof Double) {
                double numero = (Double) valor;
                if (numero == Math.rint(numero) && Math.abs(numero) < 1e15) {
                    return String.valueOf((long) numero);
                }
                return String.valueOf(numero);
            }
            return String.valueOf(valor);
        }

        public String texto(String clave, String porDefecto) {
            String valor = texto(clave);
            return valor == null || valor.trim().isEmpty() ? porDefecto : valor;
        }

        public double numero(String clave, double porDefecto) {
            Object valor = valor(clave);
            if (valor instanceof Double) {
                return (Double) valor;
            }
            if (valor instanceof String) {
                try {
                    return Double.parseDouble(((String) valor).trim().replace(".", "")
                            .replace(',', '.'));
                } catch (NumberFormatException excepcion) {
                    return porDefecto;
                }
            }
            return porDefecto;
        }

        public int entero(String clave, int porDefecto) {
            return (int) Math.round(numero(clave, porDefecto));
        }

        public boolean logico(String clave, boolean porDefecto) {
            Object valor = valor(clave);
            if (valor instanceof Boolean) {
                return (Boolean) valor;
            }
            if (valor instanceof String) {
                return Boolean.parseBoolean((String) valor);
            }
            return porDefecto;
        }

        public Objeto objeto(String clave) {
            Object valor = valor(clave);
            return valor instanceof Objeto ? (Objeto) valor : null;
        }

        public Arreglo arreglo(String clave) {
            Object valor = valor(clave);
            return valor instanceof Arreglo ? (Arreglo) valor : null;
        }

        private Campo buscar(String clave) {
            for (Campo campo : campos) {
                if (campo.clave.equals(clave)) {
                    return campo;
                }
            }
            return null;
        }

        public String serializar() {
            return Json.serializar(this);
        }
    }

    /** Arreglo JSON ordenado. */
    public static final class Arreglo {

        private final ListaEnlazada<Object> valores = new ListaEnlazada<>();

        public Arreglo agregar(Object valor) {
            valores.agregar(valor);
            return this;
        }

        public Arreglo agregar(String valor) {
            return agregar((Object) valor);
        }

        public Arreglo agregar(double valor) {
            return agregar((Object) valor);
        }

        public Arreglo agregar(int valor) {
            return agregar((Object) valor);
        }

        public Arreglo agregar(boolean valor) {
            return agregar((Object) valor);
        }

        public int tamano() {
            return valores.tamano();
        }

        public Object obtener(int indice) {
            return valores.obtener(indice);
        }

        public ListaEnlazada<Object> valores() {
            return valores;
        }

        public String serializar() {
            return Json.serializar(this);
        }
    }

    public static Objeto objeto() {
        return new Objeto();
    }

    public static Arreglo arreglo() {
        return new Arreglo();
    }

    // ------------------------------------------------------------------
    // Serialización
    // ------------------------------------------------------------------

    public static String serializar(Object valor) {
        StringBuilder texto = new StringBuilder(256);
        escribir(valor, texto);
        return texto.toString();
    }

    private static void escribir(Object valor, StringBuilder salida) {
        if (valor == null) {
            salida.append("null");
        } else if (valor instanceof String) {
            escapar((String) valor, salida);
        } else if (valor instanceof Boolean) {
            salida.append(((Boolean) valor) ? "true" : "false");
        } else if (valor instanceof Double || valor instanceof Float) {
            double numero = ((Number) valor).doubleValue();
            if (Double.isNaN(numero) || Double.isInfinite(numero)) {
                salida.append('0');
            } else if (numero == Math.rint(numero) && Math.abs(numero) < 1e15) {
                salida.append((long) numero);
            } else {
                salida.append(numero);
            }
        } else if (valor instanceof Number) {
            salida.append(((Number) valor).longValue());
        } else if (valor instanceof Objeto) {
            escribirObjeto((Objeto) valor, salida);
        } else if (valor instanceof Arreglo) {
            escribirArreglo((Arreglo) valor, salida);
        } else if (valor instanceof Iterable) {
            escribirIterable((Iterable<?>) valor, salida);
        } else {
            escapar(String.valueOf(valor), salida);
        }
    }

    private static void escribirObjeto(Objeto objeto, StringBuilder salida) {
        salida.append('{');
        boolean primero = true;
        for (Objeto.Campo campo : objeto.campos) {
            if (!primero) {
                salida.append(',');
            }
            primero = false;
            escapar(campo.clave, salida);
            salida.append(':');
            escribir(campo.valor, salida);
        }
        salida.append('}');
    }

    private static void escribirArreglo(Arreglo arreglo, StringBuilder salida) {
        salida.append('[');
        boolean primero = true;
        for (Object valor : arreglo.valores) {
            if (!primero) {
                salida.append(',');
            }
            primero = false;
            escribir(valor, salida);
        }
        salida.append(']');
    }

    private static void escribirIterable(Iterable<?> valores, StringBuilder salida) {
        salida.append('[');
        boolean primero = true;
        for (Object valor : valores) {
            if (!primero) {
                salida.append(',');
            }
            primero = false;
            escribir(valor, salida);
        }
        salida.append(']');
    }

    private static void escapar(String texto, StringBuilder salida) {
        salida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            switch (caracter) {
                case '"':
                    salida.append("\\\"");
                    break;
                case '\\':
                    salida.append("\\\\");
                    break;
                case '\n':
                    salida.append("\\n");
                    break;
                case '\r':
                    salida.append("\\r");
                    break;
                case '\t':
                    salida.append("\\t");
                    break;
                case '\b':
                    salida.append("\\b");
                    break;
                case '\f':
                    salida.append("\\f");
                    break;
                default:
                    if (caracter < 0x20) {
                        salida.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        salida.append(caracter);
                    }
            }
        }
        salida.append('"');
    }

    // ------------------------------------------------------------------
    // Análisis
    // ------------------------------------------------------------------

    public static Objeto analizarObjeto(String texto) {
        Object valor = analizar(texto);
        if (!(valor instanceof Objeto)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON.");
        }
        return (Objeto) valor;
    }

    public static Object analizar(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El cuerpo JSON está vacío.");
        }
        Analizador analizador = new Analizador(texto);
        Object valor = analizador.valor();
        analizador.saltarEspacios();
        if (!analizador.alFinal()) {
            throw new IllegalArgumentException(
                    "Contenido inesperado al final del JSON (posición " + analizador.posicion + ").");
        }
        return valor;
    }

    private static final class Analizador {

        private final String texto;
        private int posicion;

        Analizador(String texto) {
            this.texto = texto;
        }

        Object valor() {
            saltarEspacios();
            if (alFinal()) {
                throw new IllegalArgumentException("JSON incompleto.");
            }
            char caracter = texto.charAt(posicion);
            switch (caracter) {
                case '{':
                    return objeto();
                case '[':
                    return arreglo();
                case '"':
                    return cadena();
                case 't':
                    return literal("true", Boolean.TRUE);
                case 'f':
                    return literal("false", Boolean.FALSE);
                case 'n':
                    return literal("null", null);
                default:
                    return numero();
            }
        }

        private Objeto objeto() {
            Objeto objeto = new Objeto();
            posicion++; // {
            saltarEspacios();
            if (consumir('}')) {
                return objeto;
            }
            while (true) {
                saltarEspacios();
                if (alFinal() || texto.charAt(posicion) != '"') {
                    throw error("Se esperaba el nombre de un campo");
                }
                String clave = cadena();
                saltarEspacios();
                if (!consumir(':')) {
                    throw error("Se esperaba ':' después de \"" + clave + "\"");
                }
                objeto.poner(clave, valor());
                saltarEspacios();
                if (consumir('}')) {
                    return objeto;
                }
                if (!consumir(',')) {
                    throw error("Se esperaba ',' o '}'");
                }
            }
        }

        private Arreglo arreglo() {
            Arreglo arreglo = new Arreglo();
            posicion++; // [
            saltarEspacios();
            if (consumir(']')) {
                return arreglo;
            }
            while (true) {
                arreglo.agregar(valor());
                saltarEspacios();
                if (consumir(']')) {
                    return arreglo;
                }
                if (!consumir(',')) {
                    throw error("Se esperaba ',' o ']'");
                }
            }
        }

        private String cadena() {
            posicion++; // "
            StringBuilder salida = new StringBuilder();
            while (!alFinal()) {
                char caracter = texto.charAt(posicion++);
                if (caracter == '"') {
                    return salida.toString();
                }
                if (caracter == '\\') {
                    if (alFinal()) {
                        throw error("Secuencia de escape incompleta");
                    }
                    char escape = texto.charAt(posicion++);
                    switch (escape) {
                        case '"':
                        case '\\':
                        case '/':
                            salida.append(escape);
                            break;
                        case 'b':
                            salida.append('\b');
                            break;
                        case 'f':
                            salida.append('\f');
                            break;
                        case 'n':
                            salida.append('\n');
                            break;
                        case 'r':
                            salida.append('\r');
                            break;
                        case 't':
                            salida.append('\t');
                            break;
                        case 'u':
                            if (posicion + 4 > texto.length()) {
                                throw error("Escape unicode incompleto");
                            }
                            String hexadecimal = texto.substring(posicion, posicion + 4);
                            posicion += 4;
                            try {
                                salida.append((char) Integer.parseInt(hexadecimal, 16));
                            } catch (NumberFormatException excepcion) {
                                throw error("Escape unicode inválido: \\u" + hexadecimal);
                            }
                            break;
                        default:
                            throw error("Escape no reconocido: \\" + escape);
                    }
                } else {
                    salida.append(caracter);
                }
            }
            throw error("Cadena sin cerrar");
        }

        private Object literal(String esperado, Object valor) {
            if (!texto.startsWith(esperado, posicion)) {
                throw error("Se esperaba " + esperado);
            }
            posicion += esperado.length();
            return valor;
        }

        private Double numero() {
            int inicio = posicion;
            if (!alFinal() && (texto.charAt(posicion) == '-' || texto.charAt(posicion) == '+')) {
                posicion++;
            }
            boolean digito = false;
            while (!alFinal() && Character.isDigit(texto.charAt(posicion))) {
                posicion++;
                digito = true;
            }
            if (!alFinal() && texto.charAt(posicion) == '.') {
                posicion++;
                while (!alFinal() && Character.isDigit(texto.charAt(posicion))) {
                    posicion++;
                    digito = true;
                }
            }
            if (digito && !alFinal()
                    && (texto.charAt(posicion) == 'e' || texto.charAt(posicion) == 'E')) {
                posicion++;
                if (!alFinal() && (texto.charAt(posicion) == '-' || texto.charAt(posicion) == '+')) {
                    posicion++;
                }
                while (!alFinal() && Character.isDigit(texto.charAt(posicion))) {
                    posicion++;
                }
            }
            if (!digito) {
                throw error("Se esperaba un valor JSON");
            }
            try {
                return Double.valueOf(texto.substring(inicio, posicion));
            } catch (NumberFormatException excepcion) {
                throw error("Número inválido: " + texto.substring(inicio, posicion));
            }
        }

        private boolean consumir(char esperado) {
            if (!alFinal() && texto.charAt(posicion) == esperado) {
                posicion++;
                return true;
            }
            return false;
        }

        void saltarEspacios() {
            while (!alFinal() && Character.isWhitespace(texto.charAt(posicion))) {
                posicion++;
            }
        }

        boolean alFinal() {
            return posicion >= texto.length();
        }

        IllegalArgumentException error(String mensaje) {
            return new IllegalArgumentException(mensaje + " (posición " + posicion + ").");
        }
    }
}
