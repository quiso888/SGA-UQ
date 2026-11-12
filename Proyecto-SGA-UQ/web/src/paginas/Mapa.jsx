import React, { useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Badge,
  CabeceraSeccion,
  Cargando,
  Vacio,
} from '../components/ui.jsx'
import { colorEstado, numero } from '../utils.js'

const ANCHO_APARTAMENTO = 104
const ALTO_APARTAMENTO = 48

function clavePar(a, b) {
  return [a, b].sort().join('|')
}

export default function Mapa({ version, notificar }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [origen, setOrigen] = useState('RECEPCION')
  const [destino, setDestino] = useState('')
  const [ruta, setRuta] = useState(null)
  const [rutaLimpieza, setRutaLimpieza] = useState(null)
  const [calculando, setCalculando] = useState(false)

  useEffect(() => {
    let vigente = true
    setError(null)
    api
      .mapa()
      .then((respuesta) => {
        if (vigente) setDatos(respuesta)
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version])

  const nodos = datos?.nodos || []
  const aristas = datos?.aristas || []
  const mapaNodos = useMemo(() => {
    const resultado = {}
    nodos.forEach((nodo) => {
      resultado[nodo.id] = nodo
    })
    return resultado
  }, [nodos])

  const aristasRuta = useMemo(() => {
    const conjunto = new Set()
    const caminos = []
    if (ruta?.camino) caminos.push(ruta.camino)
    if (rutaLimpieza?.tramos) {
      rutaLimpieza.tramos.forEach((tramo) => {
        if (tramo.camino) caminos.push(tramo.camino)
      })
    }
    caminos.forEach((camino) => {
      for (let i = 0; i < camino.length - 1; i++) {
        conjunto.add(clavePar(camino[i], camino[i + 1]))
      }
    })
    return conjunto
  }, [ruta, rutaLimpieza])

  const nodosRuta = useMemo(() => {
    const conjunto = new Set()
    if (ruta?.camino) ruta.camino.forEach((id) => conjunto.add(id))
    if (rutaLimpieza?.tramos) {
      rutaLimpieza.tramos.forEach((tramo) =>
        (tramo.camino || []).forEach((id) => conjunto.add(id)),
      )
    }
    return conjunto
  }, [ruta, rutaLimpieza])

  if (error) {
    return (
      <Vacio titulo="No se pudo cargar el mapa" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Leyendo el grafo del edificio…" />
  }

  const calcularRuta = async () => {
    if (!origen || !destino) {
      notificar('Seleccione un origen y un destino en el mapa.', 'error')
      return
    }
    if (origen === destino) {
      notificar('El origen y el destino deben ser diferentes.', 'error')
      return
    }
    setCalculando(true)
    setRutaLimpieza(null)
    try {
      const respuesta = await api.rutaMapa(origen, destino)
      setRuta(respuesta)
      if (!respuesta.alcanzable) {
        notificar('No existe un camino activo entre esos puntos.', 'error')
      }
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setCalculando(false)
    }
  }

  const calcularLimpieza = async () => {
    setCalculando(true)
    setRuta(null)
    try {
      const respuesta = await api.rutaLimpieza(datos.recepcion)
      setRutaLimpieza(respuesta)
      notificar(
        `Ruta de limpieza calculada: ${respuesta.distanciaTotal} m en ${respuesta.tramos.length} parada(s).`,
      )
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setCalculando(false)
    }
  }

  const alternarPasillo = async (arista) => {
    try {
      const respuesta = await api.pasillo(
        arista.origen,
        arista.destino,
        !arista.activa,
      )
      setRuta(null)
      setRutaLimpieza(null)
      notificar(respuesta.mensaje)
    } catch (problema) {
      notificar(problema.message, 'error')
    }
  }

  const seleccionarNodo = (nodo) => {
    if (!origen || (origen && destino)) {
      setOrigen(nodo.id)
      setDestino('')
      setRuta(null)
      setRutaLimpieza(null)
      return
    }
    if (nodo.id === origen) return
    setDestino(nodo.id)
    setRuta(null)
  }

  const limpiar = () => {
    setOrigen(datos.recepcion)
    setDestino('')
    setRuta(null)
    setRutaLimpieza(null)
  }

  return (
    <div className="pagina mapa-pagina">
      <CabeceraSeccion
        eyebrow="Grafo del edificio"
        titulo="Mapa y rutas"
        descripcion="Nodos, pasillos ponderados y análisis de conectividad. El camino mínimo se calcula con Dijkstra y el recorrido de limpieza con el algoritmo voraz sobre el grafo."
        acciones={
          <div className="grupo-botones">
            <button className="boton fantasma" onClick={limpiar}>
              <Icon name="close" size={15} />
              Limpiar
            </button>
            <button className="boton secundario" onClick={calcularLimpieza} disabled={calculando}>
              <Icon name="ruta" size={16} />
              Ruta de limpieza
            </button>
          </div>
        }
      />

      <div className="disposicion-mapa">
        <section className="panel mapa-panel">
          <div className="mapa-encabezado">
            <div className="mapa-leyenda">
              <span>
                <i style={{ background: colorEstado('PREPARADO') }} /> Preparado
              </span>
              <span>
                <i style={{ background: colorEstado('OCUPADO') }} /> Ocupado
              </span>
              <span>
                <i style={{ background: colorEstado('PENDIENTE_PREPARACION') }} /> Por
                preparar
              </span>
              <span>
                <i style={{ background: colorEstado('FUERA_DE_SERVICIO') }} /> Fuera de
                servicio
              </span>
              <span>
                <i className="linea-cerrada" /> Pasillo cerrado
              </span>
            </div>
            {ruta && (
              <Badge tono={ruta.alcanzable ? 'azul' : 'rojo'} punto>
                {ruta.alcanzable
                  ? `Ruta: ${numero(ruta.distancia)} m`
                  : 'Sin camino activo'}
              </Badge>
            )}
            {rutaLimpieza && (
              <Badge tono="verde" punto>
                Limpieza: {numero(rutaLimpieza.distanciaTotal)} m
              </Badge>
            )}
          </div>

          <div className="mapa-svg-envoltura">
            <svg
              className="mapa-svg"
              viewBox="35 70 740 450"
              role="img"
              aria-label="Plano del edificio con apartamentos, recepción, escaleras y pasillos"
            >
              <defs>
                <pattern
                  id="cuadricula"
                  width="20"
                  height="20"
                  patternUnits="userSpaceOnUse"
                >
                  <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#e8e1d4" strokeWidth="1" />
                </pattern>
                <filter id="sombra-nodo" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="2" stdDeviation="3" floodOpacity="0.12" />
                </filter>
              </defs>

              <rect x="35" y="70" width="740" height="450" fill="url(#cuadricula)" />

              <g className="mapa-pisos">
                <rect x="55" y="90" width="610" height="75" rx="12" />
                <text x="70" y="104">BLOQUE A · PISO 1</text>
                <rect x="55" y="220" width="610" height="75" rx="12" />
                <text x="70" y="234">BLOQUE B · PISO 2</text>
                <rect x="55" y="350" width="610" height="75" rx="12" />
                <text x="70" y="364">BLOQUE C · PISO 3</text>
              </g>

              <g className="mapa-aristas">
                {aristas.map((arista) => {
                  const inicio = mapaNodos[arista.origen]
                  const fin = mapaNodos[arista.destino]
                  if (!inicio || !fin) return null
                  const resaltada = aristasRuta.has(
                    clavePar(arista.origen, arista.destino),
                  )
                  return (
                    <g key={`${arista.origen}-${arista.destino}`}>
                      <line
                        x1={inicio.x}
                        y1={inicio.y}
                        x2={fin.x}
                        y2={fin.y}
                        className={`arista ${arista.activa ? 'activa' : 'cerrada'} ${
                          resaltada ? 'resaltada' : ''
                        }`}
                      />
                      <rect
                        x={(inicio.x + fin.x) / 2 - 16}
                        y={(inicio.y + fin.y) / 2 - 9}
                        width="32"
                        height="17"
                        rx="8"
                        className="peso-fondo"
                      />
                      <text
                        x={(inicio.x + fin.x) / 2}
                        y={(inicio.y + fin.y) / 2 + 4}
                        className={`peso ${resaltada ? 'resaltado' : ''}`}
                      >
                        {arista.peso}
                      </text>
                    </g>
                  )
                })}
              </g>

              <g className="mapa-nodos">
                {nodos.map((nodo) => (
                  <NodoMapa
                    key={nodo.id}
                    nodo={nodo}
                    seleccionado={nodo.id === origen || nodo.id === destino}
                    esOrigen={nodo.id === origen}
                    esDestino={nodo.id === destino}
                    enRuta={nodosRuta.has(nodo.id)}
                    onClick={() => seleccionarNodo(nodo)}
                  />
                ))}
              </g>
            </svg>
          </div>

          <p className="nota-pie mapa-ayuda">
            Haga clic en un nodo para fijar el origen y luego otro para el destino. Los
            pesos de los pasillos están en metros.
          </p>
        </section>

        <aside className="columna-mapa">
          <section className="panel calculadora-ruta">
            <header className="panel-cabecera simple">
              <div>
                <h2>Camino mínimo</h2>
                <p>Grafo no dirigido, pesos positivos.</p>
              </div>
              <Icon name="ruta" size={20} />
            </header>

            <div className="selectores-ruta">
              <label>
                <span>Origen</span>
                <select value={origen} onChange={(evento) => setOrigen(evento.target.value)}>
                  {nodos.map((nodo) => (
                    <option key={nodo.id} value={nodo.id}>
                      {nodo.id}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Destino</span>
                <select
                  value={destino}
                  onChange={(evento) => setDestino(evento.target.value)}
                >
                  <option value="">Seleccione…</option>
                  {nodos
                    .filter((nodo) => nodo.id !== origen)
                    .map((nodo) => (
                      <option key={nodo.id} value={nodo.id}>
                        {nodo.id}
                      </option>
                    ))}
                </select>
              </label>
            </div>

            <button
              className="boton primario ancho"
              onClick={calcularRuta}
              disabled={calculando || !destino}
            >
              <Icon name="ruta" size={16} />
              {calculando ? 'Calculando…' : 'Calcular ruta'}
            </button>

            {ruta && (
              <div className="resultado-ruta">
                <div className="resultado-ruta-distancia">
                  <span>Distancia total</span>
                  <strong>{ruta.alcanzable ? `${numero(ruta.distancia)} m` : '—'}</strong>
                </div>
                {ruta.alcanzable ? (
                  <div className="camino">
                    {ruta.camino.map((id, indice) => (
                      <React.Fragment key={`${id}-${indice}`}>
                        <span className="camino-nodo">{id}</span>
                        {indice < ruta.camino.length - 1 && (
                          <Icon name="flecha" size={13} />
                        )}
                      </React.Fragment>
                    ))}
                  </div>
                ) : (
                  <p className="nota-pie">
                    No hay un recorrido con pasillos activos. Revise los pasillos cerrados
                    más abajo.
                  </p>
                )}
              </div>
            )}

            {rutaLimpieza && (
              <div className="resultado-limpieza">
                <strong>Ruta de limpieza</strong>
                <ol>
                  {rutaLimpieza.tramos.map((tramo, indice) => (
                    <li key={tramo.codigo}>
                      <span>{indice + 1}</span>
                      <div>
                        <strong>{tramo.codigo}</strong>
                        <small>
                          {tramo.alcanzable
                            ? `${numero(tramo.distancia)} m desde ${tramo.desde}`
                            : 'Inalcanzable'}
                        </small>
                      </div>
                    </li>
                  ))}
                </ol>
              </div>
            )}
          </section>

          <section className="panel analisis-mapa">
            <header className="panel-cabecera simple">
              <div>
                <h2>Análisis del grafo</h2>
                <p>Conectividad y agrupación por bloque.</p>
              </div>
            </header>
            <div className="mini-datos analisis">
              <div>
                <span>Nodos</span>
                <strong>{datos.analisis.nodos}</strong>
              </div>
              <div>
                <span>Pasillos</span>
                <strong>{datos.analisis.pasillos}</strong>
              </div>
              <div>
                <span>Componentes</span>
                <strong>{datos.analisis.componentes.length}</strong>
              </div>
              <div>
                <span>Ciclos</span>
                <strong>{datos.analisis.tieneCiclo ? 'Sí' : 'No'}</strong>
              </div>
            </div>

            <div className="bloques-mapa">
              {datos.analisis.bloques.map((bloque) => (
                <div key={bloque.nombre} className="bloque-mapa">
                  <span>Bloque {bloque.nombre}</span>
                  <strong>{bloque.cantidad} unidades</strong>
                  <small>{bloque.apartamentos.join(' · ')}</small>
                </div>
              ))}
            </div>

            <div
              className={`aviso ${
                datos.analisis.inalcanzables.length ? 'advertencia' : 'exito'
              }`}
            >
              <Icon
                name={datos.analisis.inalcanzables.length ? 'alerta' : 'check'}
                size={17}
              />
              <p>
                {datos.analisis.inalcanzables.length
                  ? `Unidades inalcanzables desde recepción: ${datos.analisis.inalcanzables.join(', ')}.`
                  : 'Todas las unidades son alcanzables desde recepción.'}
              </p>
            </div>
          </section>

          <section className="panel pasillos-panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Pasillos</h2>
                <p>Cierre o abra tramos para simular mantenimiento.</p>
              </div>
            </header>
            <div className="lista-pasillos">
              {aristas.map((arista) => (
                <div
                  key={`${arista.origen}-${arista.destino}`}
                  className={`pasillo ${arista.activa ? '' : 'cerrado'}`}
                >
                  <span className="pasillo-par">
                    <strong>{arista.origen}</strong>
                    <Icon name="flecha" size={12} />
                    <strong>{arista.destino}</strong>
                  </span>
                  <span className="pasillo-peso">{arista.peso} m</span>
                  <button
                    className={`boton mini ${arista.activa ? 'fantasma' : 'secundario'}`}
                    onClick={() => alternarPasillo(arista)}
                  >
                    {arista.activa ? 'Cerrar' : 'Abrir'}
                  </button>
                </div>
              ))}
            </div>
          </section>
        </aside>
      </div>
    </div>
  )
}

function NodoMapa({ nodo, seleccionado, esOrigen, esDestino, enRuta, onClick }) {
  const esApartamento = nodo.tipo === 'apartamento'
  const color = nodo.estado?.color || '#5b7c6b'
  const ancho = esApartamento ? ANCHO_APARTAMENTO : 44
  const alto = esApartamento ? ALTO_APARTAMENTO : 44
  const x = nodo.x - ancho / 2
  const y = nodo.y - alto / 2
  const clase = `nodo ${seleccionado ? 'seleccionado' : ''} ${
    esOrigen ? 'origen' : ''
  } ${esDestino ? 'destino' : ''} ${enRuta ? 'en-ruta' : ''}`

  return (
    <g
      className={clase}
      onClick={onClick}
      tabIndex={0}
      onKeyDown={(evento) => {
        if (evento.key === 'Enter' || evento.key === ' ') {
          evento.preventDefault()
          onClick()
        }
      }}
      role="button"
      aria-label={`${nodo.id} ${nodo.nombre || ''}`}
    >
      {esApartamento ? (
        <>
          <rect
            x={x}
            y={y}
            width={ancho}
            height={alto}
            rx="9"
            className="nodo-cuerpo"
            style={{ stroke: enRuta || seleccionado ? color : '#d8d0c0' }}
          />
          <rect x={x} y={y} width="6" height={alto} rx="3" fill={color} />
          <text x={nodo.x - 32} y={nodo.y - 6} className="nodo-codigo">
            {nodo.id}
          </text>
          <text x={nodo.x - 32} y={nodo.y + 9} className="nodo-nombre">
            {(nodo.nombre || '').slice(0, 17)}
          </text>
          <text x={nodo.x - 32} y={nodo.y + 21} className="nodo-detalle">
            {nodo.capacidad} pers. · {nodo.bloque}
          </text>
        </>
      ) : (
        <>
          <rect
            x={x}
            y={y}
            width={ancho}
            height={alto}
            rx={nodo.tipo === 'recepcion' ? 12 : 22}
            className="nodo-cuerpo instalacion"
            style={{ stroke: enRuta || seleccionado ? '#c9a227' : '#d8d0c0' }}
          />
          <text x={nodo.x} y={nodo.y + 1} className="nodo-instalacion">
            {nodo.tipo === 'recepcion'
              ? 'REC'
              : nodo.tipo === 'escalera'
                ? 'ESC'
                : 'ASEO'}
          </text>
          <text x={nodo.x} y={nodo.y + 13} className="nodo-instalacion-sub">
            {nodo.tipo === 'recepcion'
              ? 'Recepción'
              : nodo.tipo === 'escalera'
                ? nodo.id.slice(-1)
                : ''}
          </text>
        </>
      )}
      {esOrigen && (
        <text x={nodo.x} y={y - 9} className="etiqueta-extremo origen-texto">
          ORIGEN
        </text>
      )}
      {esDestino && (
        <text x={nodo.x} y={y - 9} className="etiqueta-extremo destino-texto">
          DESTINO
        </text>
      )}
    </g>
  )
}
