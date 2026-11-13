# SGA · Sistema de Gestión de Alojamiento

Aplicación de escritorio para **Aparta-Suites UQ** (Salento, Quindío), desarrollada
como proyecto académico de **Estructuras de Datos**. El núcleo del sistema funciona
únicamente con estructuras de datos genéricas implementadas por el equipo: no usa
`ArrayList`, `LinkedList`, `PriorityQueue`, `TreeMap`, `HashMap`, `HashSet`, `Stack`
ni ninguna otra colección de `java.util`.

La **aplicación de escritorio** está hecha en **Swing** y permite operar el
alojamiento de punta a punta: tablero de estados, reservas, calendario de ocupación,
huéspedes, preparación, mapa interactivo del edificio y estadísticas. Como extra, la
misma lógica se expone con `--web` mediante un servidor local que sirve una interfaz
**React** y una **API JSON**; el núcleo Java y las estructuras propias son idénticos
en ambos modos.

---

## 1. Cómo compilar y ejecutar

Requisitos: **JDK 17 o superior** (probado con Oracle JDK 17). No requiere Maven,
Gradle ni bibliotecas externas.

### Windows (PowerShell)

```powershell
# Compilar (UTF-8 es obligatorio: el código usa tildes y eñes)
$files = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d build $files

# Abrir la interfaz de escritorio (Swing)
java -Dfile.encoding=UTF-8 -cp build Main

# Abrir la interfaz web React + API JSON en el navegador
java -Dfile.encoding=UTF-8 -cp build Main --web

# Ejecutar la autocomprobación de consola (101 verificaciones)
java -Dfile.encoding=UTF-8 -cp build Main --consola
```

### Linux / macOS

```bash
find src -name "*.java" > fuentes.txt
javac -encoding UTF-8 -d build @fuentes.txt
java -Dfile.encoding=UTF-8 -cp build Main
java -Dfile.encoding=UTF-8 -cp build Main --web
java -Dfile.encoding=UTF-8 -cp build Main --consola
```

Sin argumentos se abre la **aplicación de escritorio Swing** en su propia ventana.
Con `--web` se levanta un servidor local que sirve la interfaz React y la API JSON
en el mismo puerto (`8080` por defecto). El frontend compilado vive en `web/dist`,
por lo que **no se necesita npm ni Node para ejecutar**.

La autocomprobación imprime `AUTOCONTROL: 101 comprobaciones, TODAS correctas.` si
todas las reglas del negocio y las estructuras funcionan.

### Abrir en IntelliJ IDEA

1. **File → Open** y selecciona la carpeta del proyecto.
2. En **Project Structure → Project**, usa un **SDK de Java 17**.
3. En **Settings → Editor → File Encodings**, selecciona **UTF-8** para el proyecto.
4. Marca `src` como *Sources Root* y `build` como *Excluded* (clic derecho →
   **Mark Directory as**). Así IntelliJ no abre los `.class` compilados.
5. Ejecuta `src/Main.java` con **Run 'Main.main()'**. Para la autocomprobación,
   agrega `--consola` en **Program arguments**.

> Si ves el aviso «Decompiled .class file, bytecode version…», estás abriendo un
> archivo de `build/` y no el código fuente: abre el `.java` correspondiente en
> `src/`.

### Carga de datos

La aplicación arranca con un **escenario de prueba** generado en memoria. Desde el
menú **Datos** también se puede:

- cargar el escenario de prueba,
- cargar un archivo plano (`data/escenario.txt` es un ejemplo completo),
- empezar con un sistema vacío.

El formato del archivo se documenta en el propio archivo y en
`src/util/CargadorDatos.java` (órdenes `TEMPORADA`, `APARTAMENTO`, `EMPLEADO`,
`HUESPED`, `NODO`, `PASILLO` y `RESERVA`, separadas por `|`).

---

## 2. Funcionalidades

### Roles y navegación
- Al abrir (y desde **Sesión → Cambiar de rol…**) se elige el rol y cada uno ve solo
  sus pantallas:
  - **Huésped**: *Mi cuenta* y *Calendario*.
  - **Recepcionista**: *Tablero, Reservas, Calendario, Huéspedes, Preparación y Mapa*.
  - **Administrador**: *Tablero, Calendario, Mapa, Administración y Estadísticas*.
- El botón **← Atrás** vuelve a la pantalla anterior usando una `Pila` propia
  (`HistorialNavegacion`).

### Mi cuenta (huésped)
- Sus reservas activas con el **saldo de cada folio**, favoritos (`Conjunto`) e
  historial de estancias.
- **Buscar y reservar**: disponibilidad para fechas y personas, ordenada por precio,
  capacidad o número de reservas, con cotización noche por noche (iterando la
  `Estancia`). Reserva siempre por el portal (nace Pendiente).
- **Pagar anticipo** y **cancelar** viendo antes la devolución estimada.

### Administración (administrador)
- **Empleados**, **apartamentos** y **huéspedes** en árboles binarios de búsqueda
  (alta, edición y baja), con selector de recorrido (in-orden, pre-orden,
  post-orden y por niveles) y altura del árbol. Un apartamento con reservas o un
  huésped con historial no se pueden eliminar.
- **Temporadas y tarifas**: periodos de cada temporada y tarifa por persona y noche
  de cada apartamento.

### Tablero
- Tarjeta por apartamento con el código de colores exigido:
  **verde** Preparado, **azul** Ocupado, **amarillo** Pendiente/En preparación y
  **rojo** Fuera de servicio.
- Resumen de estados, próxima reserva de cada unidad, fecha y **turno de recepción**
  (lista circular) con botón para rotarlo.

### Reservas
- Tabla de todas las reservas con selector de orden (fecha, titular, estado, total).
- Detalle con ocupantes, folio, movimientos y operaciones deshacibles.
- Creación de reservas por **portal** (nace Pendiente) o **recepción directa**
  (nace Confirmada), con búsqueda de disponibilidad ordenada por precio, capacidad
  o número de reservas.
- **Cotización noche por noche** dentro del diálogo de reserva.
- Anticipo del 30 %, check-in, check-out, cancelación con **devolución estimada**
  y registro de no-show.
- Procesamiento de **vencimientos** de reservas Pendientes (24 horas).
- **Folio**: registro de cargos y pagos y deshacer mediante contra-movimiento.
- **Canal externo**: cola FIFO, procesamiento individual o total, bitácora de
  solicitudes y conflictos de canal.
- **Grupos grandes**: combinación mínima de apartamentos por backtracking.

### Calendario de ocupación
- Cuadrícula de 30 días por apartamento, con temporada en la cabecera.
- Noches libres, ocupadas y bloqueadas (fuera de servicio), desplazamiento por día
  o por semana y detalle de la celda seleccionada.

### Huéspedes
- Búsqueda por nombre o documento, ficha completa, favoritos sin duplicados
  (`Conjunto`) e historial de estancias.

### Preparación
- Cola de prioridad: primero los apartamentos con entrada hoy, ordenados por hora
  estimada de llegada; después el resto.
- Ruta de limpieza calculada con **Dijkstra** sobre el grafo del edificio.
- Inicio y finalización de tareas; al completar, el apartamento queda Preparado.

### Mapa interactivo
- Grafo dibujado y arrastrable con el estado de cada apartamento en tiempo real.
- Ruta más corta desde recepción hasta un apartamento y ruta completa de limpieza.
- **Análisis del edificio** con BFS/DFS: bloques, componentes conexas y unidades
  inalcanzables.
- **Cerrar / abrir pasillos** para simular zonas aisladas y comprobar la
  conectividad.

### Estadísticas
- Tarjetas de ingresos, ocupación de hoy, reservas, cancelaciones, conflictos y
  tiempo medio de preparación.
- Gráficos: reservas por estado, reservas por canal (incluye canceladas y no-show),
  ingresos por canal, ingresos por temporada, ingresos por periodo (mes),
  apartamentos por estado, noches reservadas por apartamento y ocupación por
  temporada.

---

## 3. Estructuras de datos propias

Todas son genéricas, implementan `Iterable<T>` con su **propio iterador interno** y
viven en `src/datastructures/`.

| Estructura | Uso principal en el SGA |
|---|---|
| `Grafo<T>` | Mapa del edificio; Dijkstra, BFS, DFS, componentes conexas y ciclos. |
| `ColaPrioridad<T>` | Preparación (prioridad 1: entrada hoy) y vencimientos de reservas. |
| `Cola<T>` | Solicitudes del canal externo, en orden FIFO. |
| `Pila<T>` | Deshacer la última operación del folio y navegación «Atrás» entre pantallas. |
| `ListaEnlazada<T>` | Catálogos auxiliares y resultados de búsquedas. |
| `ListaDobleEnlazada<T>` | Ocupantes, movimientos del folio, desglose de noches e historial. |
| `ListaCircular<T>` | Rotación de los turnos de recepción. |
| `ArbolBinarioBusqueda<T extends Comparable<T>>` | Apartamentos por código, huéspedes y empleados por documento, y reservas por fecha. |
| `Conjunto<T>` | Apartamentos favoritos sin repetidos. |

### Conceptos de Java exigidos

- **Genéricos**: ninguna estructura conoce clases del dominio.
- **`Comparable`**: `Apartamento` por código, `Huesped` y `Empleado` por documento y
  `Reserva` por fecha de entrada (con código como desempate).
- **`Comparator`**: precio por temporada, capacidad, número de reservas y prioridad
  de preparación (`Comparadores` + `thenComparing`).
- **`Iterable` / `Iterator`**: todas las estructuras y `Estancia` (que itera las
  noches que se cobran).
- **Recursividad**: inserción, búsqueda, eliminación, altura y recorridos del árbol;
  detección de solapamientos; DFS del grafo; liquidación noche por noche;
  backtracking para grupos grandes.
- **Excepciones propias**: `ReservaSolapadaException`, `CapacidadExcedidaException`,
  `FechasInvalidasException`, `TransicionEstadoInvalidaException`,
  `EstructuraVaciaException`, además de `SgaException` como base y otras auxiliares.

---

## 4. Reglas del negocio implementadas

1. Se cobra **por persona y por noche**; los menores de 6 años ocupan cupo pero no
   pagan.
2. La **noche de salida no se ocupa ni se cobra** (del 10 al 12 son 2 noches).
3. Estancias que cruzan temporadas se liquidan **noche por noche**.
4. Un apartamento no puede tener dos reservas activas que compartan una noche,
   **sin importar el canal**.
5. Tras el check-out el apartamento queda **Pendiente de preparación** y no puede
   recibir huéspedes hasta estar **Preparado**.
6. Los movimientos del folio son **inalterables**; las correcciones se registran
   como movimientos contrarios.
7. Al cancelar, el anticipo se devuelve completo si es antes de la fecha de entrada
   y no se devuelve el mismo día del ingreso.
8. Las reservas Pendientes vencen **24 horas** después de su creación y liberan sus
   noches.
9. Las solicitudes del canal externo se procesan **en orden de llegada**; si chocan
   con una reserva vigente se rechazan y se registra el conflicto.

---

## 5. Organización del código

```
src/
├── Main.java                    Punto de entrada (escritorio, web, consola)
├── model/                       Entidades del dominio y comparadores
├── datastructures/              Estructuras genéricas propias
├── exceptions/                  Excepciones del dominio
├── service/                     Lógica de negocio y fachada SistemaAlojamiento
├── util/                        Escenario de prueba, cargador y autocomprobación
├── gui/                         Paneles y diálogos Swing (modo escritorio)
└── api/                         Servidor HTTP, controlador REST y JSON (modo web)
web/
├── src/                         Código fuente React (páginas, componentes, API)
└── dist/                        Build estático servido por Java (sin npm en runtime)
data/
└── escenario.txt                Ejemplo de archivo de carga
docs/
├── requisitos-extraidos.txt     Especificación del proyecto
├── diagrama-clases.puml         Diagrama de clases (PlantUML)
├── diagrama-estructuras.puml    Diagrama de estructuras propias (PlantUML)
└── diagramas/                   Versiones renderizadas (PNG y SVG)
```

### Diagramas

**Diagrama de clases**

![Diagrama de clases](docs/diagramas/diagrama-clases.png)

**Diagrama de estructuras de datos propias**

![Diagrama de estructuras](docs/diagramas/diagrama-estructuras.png)

Para regenerarlos, con PlantUML instalado:

```bash
plantuml -tpng -o docs/diagramas docs/diagrama-clases.puml docs/diagrama-estructuras.puml
```

`SistemaAlojamiento` es la **fachada** que consume la GUI. Los servicios
especializados son `GestorReservas`, `GestorEdificio`, `GestorPreparacion`,
`GestorFolios`, `ProcesadorCanalExterno`, `AsignadorGrupos`, `ServicioTemporadas`,
`ServicioLiquidacion` y `ServicioEstadisticas`.

Los diagramas `.puml` se pueden abrir con [PlantUML](https://plantuml.com/) o con la
extensión de PlantUML de Visual Studio Code.

---

## 6. Verificación

```bash
java -cp build Main --consola
```

La autocomprobación (`src/util/Autocomprobacion.java`) cubre las nueve estructuras,
la liquidación, el solapamiento, el ciclo de vida, el canal externo, el backtracking,
el folio, el escenario completo, la conectividad del mapa y las estadísticas.
