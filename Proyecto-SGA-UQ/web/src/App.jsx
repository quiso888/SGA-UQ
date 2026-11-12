import React, { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import { Icon } from './components/Icon.jsx'
import { Modal, Toast } from './components/ui.jsx'
import { fechaLarga, hoyISO } from './utils.js'
import Tablero from './paginas/Tablero.jsx'
import Reservas from './paginas/Reservas.jsx'
import Calendario from './paginas/Calendario.jsx'
import Huespedes from './paginas/Huespedes.jsx'
import Preparacion from './paginas/Preparacion.jsx'
import Mapa from './paginas/Mapa.jsx'
import Estadisticas from './paginas/Estadisticas.jsx'
import CanalExterno from './paginas/CanalExterno.jsx'

const SECCIONES = [
  { id: 'tablero', etiqueta: 'Tablero', icono: 'tablero', detalle: 'Estado operativo' },
  { id: 'reservas', etiqueta: 'Reservas', icono: 'reservas', detalle: 'Ciclo y folios' },
  { id: 'canal', etiqueta: 'Canal externo', icono: 'canal', detalle: 'Cola FIFO' },
  { id: 'calendario', etiqueta: 'Calendario', icono: 'calendario', detalle: 'Ocupación por noche' },
  { id: 'huespedes', etiqueta: 'Huéspedes', icono: 'huespedes', detalle: 'Fichas e historial' },
  { id: 'preparacion', etiqueta: 'Preparación', icono: 'preparacion', detalle: 'Prioridades y ruta' },
  { id: 'mapa', etiqueta: 'Mapa', icono: 'mapa', detalle: 'Grafo del edificio' },
  { id: 'estadisticas', etiqueta: 'Estadísticas', icono: 'estadisticas', detalle: 'Indicadores' },
]

export default function App() {
  const [seccion, setSeccion] = useState('tablero')
  const [version, setVersion] = useState(0)
  const [resumen, setResumen] = useState(null)
  const [toast, setToast] = useState(null)
  const [menuDatos, setMenuDatos] = useState(false)
  const [menuMovil, setMenuMovil] = useState(false)
  const [nuevaReserva, setNuevaReserva] = useState(false)
  const [reservaEnfocada, setReservaEnfocada] = useState(null)

  const cargarResumen = useCallback(async () => {
    try {
      const datos = await api.resumen()
      setResumen(datos)
    } catch (error) {
      console.error('No se pudo cargar el resumen', error)
    }
  }, [])

  useEffect(() => {
    cargarResumen()
  }, [cargarResumen, version])

  const notificar = (mensaje, tipo = 'exito') => {
    setToast({ mensaje, tipo })
    setVersion((actual) => actual + 1)
    cargarResumen()
  }

  const navegar = (destino) => {
    setSeccion(destino)
    setMenuMovil(false)
  }

  const irAReserva = (codigo) => {
    setReservaEnfocada(codigo)
    navegar('reservas')
  }

  const abrirNuevaReserva = () => {
    setNuevaReserva(true)
    navegar('reservas')
  }

  const turno = resumen?.turno
  const hoy = resumen?.hoy?.fecha || hoyISO()
  const temporada = resumen?.hoy?.temporada
  const solicitudes = resumen?.totales?.solicitudesPendientes || 0

  return (
    <div className="aplicacion">
      {menuMovil && (
        <button
          className="velo-menu"
          aria-label="Cerrar menú"
          onClick={() => setMenuMovil(false)}
        />
      )}

      <aside className={`barra-lateral ${menuMovil ? 'abierta' : ''}`}>
        <div className="marca">
          <span className="marca-sello">AS</span>
          <div>
            <strong>Aparta-Suites UQ</strong>
            <span>Salento · Quindío</span>
          </div>
          <button
            className="boton-icono marca-cerrar"
            onClick={() => setMenuMovil(false)}
            aria-label="Cerrar menú"
          >
            <Icon name="close" size={20} />
          </button>
        </div>

        <nav className="navegacion" aria-label="Secciones del sistema">
          <p className="navegacion-titulo">Operación</p>
          {SECCIONES.map((item) => (
            <button
              key={item.id}
              className={`navegacion-item ${seccion === item.id ? 'activo' : ''}`}
              onClick={() => navegar(item.id)}
            >
              <span className="navegacion-icono">
                <Icon name={item.icono} size={19} />
              </span>
              <span className="navegacion-texto">
                <strong>{item.etiqueta}</strong>
                <small>{item.detalle}</small>
              </span>
              {item.id === 'canal' && solicitudes > 0 && (
                <span className="navegacion-badge">{solicitudes}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="lateral-pie">
          <div className="mini-resumen">
            <div>
              <span>Apartamentos</span>
              <strong>{resumen?.totales?.apartamentos ?? '—'}</strong>
            </div>
            <div>
              <span>Reservas</span>
              <strong>{resumen?.totales?.reservas ?? '—'}</strong>
            </div>
            <div>
              <span>Por preparar</span>
              <strong>{resumen?.totales?.preparacionesPendientes ?? '—'}</strong>
            </div>
          </div>
          <button className="boton-datos" onClick={() => setMenuDatos(true)}>
            <Icon name="datos" size={17} />
            Datos del sistema
          </button>
          <p className="version">React · Java 17 · estructuras propias</p>
        </div>
      </aside>

      <div className="contenido">
        <header className="barra-superior">
          <div className="superior-izquierda">
            <button
              className="boton-icono menu-movil"
              onClick={() => setMenuMovil(true)}
              aria-label="Abrir menú"
            >
              <Icon name="menus" size={22} />
            </button>
            <div className="miga">
              <span>SGA</span>
              <Icon name="derecha" size={13} />
              <strong>{SECCIONES.find((item) => item.id === seccion)?.etiqueta}</strong>
            </div>
          </div>

          <div className="superior-derecha">
            <div className="fecha-superior">
              <span>Hoy</span>
              <strong>{fechaLarga(hoy)}</strong>
            </div>
            {temporada && (
              <span className={`pildora-temporada temporada-${temporada.clave.toLowerCase()}`}>
                Temporada {temporada.descripcion.toLowerCase()}
              </span>
            )}
            <button
              className="turno-chip"
              title="Rotar turno de recepción"
              onClick={async () => {
                try {
                  const datos = await api.rotarTurno()
                  notificar(datos.mensaje)
                } catch (error) {
                  notificar(error.message, 'error')
                }
              }}
            >
              <span className="turno-persona">
                <Icon name="llave" size={15} />
                {turno ? turno.nombre : 'Sin recepcionista'}
              </span>
              <Icon name="rotar" size={14} />
            </button>
            <button
              className="boton-icono"
              title="Actualizar datos"
              onClick={() => setVersion((actual) => actual + 1)}
            >
              <Icon name="refresh" size={18} />
            </button>
            <button className="boton primario compacto" onClick={abrirNuevaReserva}>
              <Icon name="plus" size={16} />
              <span className="solo-escritorio">Nueva reserva</span>
            </button>
          </div>
        </header>

        <main className="area-principal">
          {seccion === 'tablero' && (
            <Tablero
              version={version}
              notificar={notificar}
              irAReserva={irAReserva}
              irAPreparacion={() => navegar('preparacion')}
            />
          )}
          {seccion === 'reservas' && (
            <Reservas
              version={version}
              notificar={notificar}
              reservaEnfocada={reservaEnfocada}
              onEnfocar={setReservaEnfocada}
              abrirNueva={nuevaReserva}
              onNuevaAbierta={() => setNuevaReserva(false)}
              irACalendario={() => navegar('calendario')}
            />
          )}
          {seccion === 'canal' && (
            <CanalExterno version={version} notificar={notificar} />
          )}
          {seccion === 'calendario' && (
            <Calendario
              version={version}
              notificar={notificar}
              irAReserva={irAReserva}
            />
          )}
          {seccion === 'huespedes' && (
            <Huespedes version={version} notificar={notificar} irAReserva={irAReserva} />
          )}
          {seccion === 'preparacion' && (
            <Preparacion version={version} notificar={notificar} />
          )}
          {seccion === 'mapa' && (
            <Mapa version={version} notificar={notificar} />
          )}
          {seccion === 'estadisticas' && (
            <Estadisticas version={version} notificar={notificar} />
          )}
        </main>

        <footer className="pie-aplicacion">
          <span>Proyecto académico de Estructuras de Datos · Universidad del Quindío</span>
          <span>SGA 2.0 · React + API Java local</span>
        </footer>
      </div>

      <MenuDatos
        abierto={menuDatos}
        onCerrar={() => setMenuDatos(false)}
        notificar={notificar}
        resumen={resumen}
      />

      <Toast
        mensaje={toast?.mensaje}
        tipo={toast?.tipo}
        onCerrar={() => setToast(null)}
      />
    </div>
  )
}

function MenuDatos({ abierto, onCerrar, notificar, resumen }) {
  const [ruta, setRuta] = useState('data/escenario.txt')
  const [cargando, setCargando] = useState(false)

  const ejecutar = async (accion, datos = {}) => {
    setCargando(true)
    try {
      const respuesta = await api.cargarDatos(accion, datos)
      notificar(respuesta.mensaje)
      onCerrar()
    } catch (error) {
      notificar(error.message, 'error')
    } finally {
      setCargando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      titulo="Datos del sistema"
      subtitulo="Cambie el conjunto de datos que se muestra en toda la aplicación."
      onCerrar={onCerrar}
      ancho="mediano"
    >
      <div className="opciones-datos">
        <button
          className="opcion-dato"
          disabled={cargando}
          onClick={() => ejecutar('escenario')}
        >
          <span className="opcion-dato-icono">
            <Icon name="chispa" size={20} />
          </span>
          <div>
            <strong>Cargar escenario de prueba</strong>
            <p>12 apartamentos, huéspedes, reservas, mapa y canal externo.</p>
          </div>
          <Icon name="flecha" size={17} />
        </button>

        <button
          className="opcion-dato"
          disabled={cargando}
          onClick={() => ejecutar('vacio')}
        >
          <span className="opcion-dato-icono">
            <Icon name="datos" size={20} />
          </span>
          <div>
            <strong>Empezar con un sistema vacío</strong>
            <p>Sin apartamentos ni huéspedes registrados.</p>
          </div>
          <Icon name="flecha" size={17} />
        </button>
      </div>

      <div className="bloque-dato-archivo">
        <label className="campo">
          <span className="campo-etiqueta">Cargar desde archivo</span>
          <input
            value={ruta}
            onChange={(evento) => setRuta(evento.target.value)}
            placeholder="data/escenario.txt"
          />
          <span className="campo-ayuda">
            Ruta local del archivo de datos (relativa a la carpeta del proyecto).
          </span>
        </label>
        <button
          className="boton secundario"
          disabled={cargando || !ruta.trim()}
          onClick={() => ejecutar('archivo', { ruta })}
        >
          <Icon name="datos" size={16} />
          Cargar archivo
        </button>
      </div>

      {resumen?.totales && (
        <div className="resumen-carga">
          <div>
            <span>Apartamentos</span>
            <strong>{resumen.totales.apartamentos}</strong>
          </div>
          <div>
            <span>Huéspedes</span>
            <strong>{resumen.totales.huespedes}</strong>
          </div>
          <div>
            <span>Reservas</span>
            <strong>{resumen.totales.reservas}</strong>
          </div>
          <div>
            <span>Preparaciones</span>
            <strong>{resumen.totales.preparacionesPendientes}</strong>
          </div>
        </div>
      )}
    </Modal>
  )
}
