import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Badge,
  CabeceraSeccion,
  Cargando,
  Vacio,
} from '../components/ui.jsx'
import { fechaHora, minutos, numero } from '../utils.js'

export default function Preparacion({ version, notificar }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [puntoPartida, setPuntoPartida] = useState('RECEPCION')
  const [rutaPersonalizada, setRutaPersonalizada] = useState(null)
  const [calculando, setCalculando] = useState(false)
  const [accionEnCurso, setAccionEnCurso] = useState(null)

  useEffect(() => {
    let vigente = true
    setError(null)
    api
      .preparacion()
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

  useEffect(() => {
    setRutaPersonalizada(null)
    setPuntoPartida('RECEPCION')
  }, [version])

  if (error) {
    return (
      <Vacio titulo="No se pudo cargar la preparación" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Ordenando la cola de preparación…" />
  }

  const resumen = datos.resumen
  const ruta = rutaPersonalizada
    ? rutaPersonalizada.tramos
    : datos.ruta.map((item) => ({
        codigo: item.codigo,
        nombre: item.nombre,
        estado: item.estado,
        desde: item.desde,
        distancia: item.distancia,
        alcanzable: item.alcanzable,
        camino: item.tramo,
        orden: item.orden,
        prioridad: item.prioridad,
        entradaHoy: item.entradaHoy,
        hora: item.hora,
      }))
  const distanciaTotal = rutaPersonalizada
    ? rutaPersonalizada.distanciaTotal
    : resumen.distanciaRuta

  const recalcular = async (punto) => {
    setPuntoPartida(punto)
    if (punto === 'RECEPCION') {
      setRutaPersonalizada(null)
      return
    }
    setCalculando(true)
    try {
      const respuesta = await api.rutaLimpieza(punto)
      setRutaPersonalizada(respuesta)
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setCalculando(false)
    }
  }

  const actuar = async (tarea, accion) => {
    setAccionEnCurso(`${tarea.codigo}-${accion}`)
    try {
      const respuesta = await api.accionPreparacion(tarea.codigo, accion)
      notificar(respuesta.mensaje)
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setAccionEnCurso(null)
    }
  }

  return (
    <div className="pagina preparacion">
      <CabeceraSeccion
        eyebrow="Housekeeping"
        titulo="Preparación de apartamentos"
        descripcion="Cola de prioridad: primero las llegadas de hoy por hora, luego el resto. La ruta se calcula con Dijkstra sobre el grafo del edificio."
      />

      <div className="indicadores-preparacion">
        <TarjetaIndicador
          icono="preparacion"
          etiqueta="Pendientes"
          valor={resumen.pendientes}
          detalle="unidades por limpiar"
          tono="amarillo"
        />
        <TarjetaIndicador
          icono="reloj"
          etiqueta="Llegadas hoy"
          valor={resumen.conEntradaHoy}
          detalle="con prioridad uno"
          tono="rojo"
        />
        <TarjetaIndicador
          icono="check"
          etiqueta="Completadas"
          valor={resumen.completadas}
          detalle="en el historial"
          tono="verde"
        />
        <TarjetaIndicador
          icono="ruta"
          etiqueta="Recorrido"
          valor={`${numero(distanciaTotal)} m`}
          detalle="desde el punto de partida"
          tono="azul"
        />
        <TarjetaIndicador
          icono="reloj"
          etiqueta="Tiempo promedio"
          valor={`${numero(resumen.promedioMinutos)} min`}
          detalle="por preparación"
          tono="neutro"
        />
      </div>

      <div className="disposicion-preparacion">
        <section className="panel cola-preparacion">
          <header className="panel-cabecera simple">
            <div>
              <h2>Cola de prioridad</h2>
              <p>
                Prioridad 1: entrada hoy. Prioridad 2: resto. El orden interno respeta
                hora de llegada y antigüedad.
              </p>
            </div>
            <Badge tono={datos.tareas.length ? 'amarillo' : 'verde'} punto>
              {datos.tareas.length} en cola
            </Badge>
          </header>

          {datos.tareas.length === 0 ? (
            <Vacio titulo="No hay apartamentos por preparar" icono="check">
              <p>La cola está vacía. Los check-out se encolarán automáticamente.</p>
            </Vacio>
          ) : (
            <div className="lista-tareas">
              {datos.tareas.map((tarea, indice) => (
                <article
                  key={tarea.codigo}
                  className={`tarea ${
                    tarea.prioridad === 1 ? 'prioridad-uno' : ''
                  }`}
                >
                  <span className="tarea-orden">{indice + 1}</span>
                  <div className="tarea-contenido">
                    <header>
                      <div>
                        <strong>
                          {tarea.codigo} · {tarea.nombre}
                        </strong>
                        <span>
                          <Badge
                            tono={tarea.prioridad === 1 ? 'rojo' : 'neutro'}
                          >
                            Prioridad {tarea.prioridad}
                          </Badge>
                          {tarea.entradaHoy && (
                            <Badge tono="amarillo">Entrada hoy</Badge>
                          )}
                          {tarea.reservaEntrante && (
                            <Badge tono="azul">
                              {tarea.reservaEntrante}
                            </Badge>
                          )}
                        </span>
                      </div>
                      <Badge tono="neutro">{tarea.estado.descripcion}</Badge>
                    </header>
                    <div className="tarea-meta">
                      <span>
                        <Icon name="reloj" size={14} />
                        Llegada estimada: {tarea.hora?.slice(0, 5) || '—'}
                      </span>
                      <span>
                        <Icon name="calendario" size={14} />
                        Pendiente desde {fechaHora(tarea.desde)}
                      </span>
                    </div>
                    {tarea.inicio && (
                      <p className="nota-pie">
                        Preparación iniciada {fechaHora(tarea.inicio)}
                      </p>
                    )}
                  </div>
                  <div className="tarea-acciones">
                    {tarea.estado.clave === 'EN_PREPARACION' ? (
                      <button
                        className="boton primario compacto"
                        onClick={() => actuar(tarea, 'completar')}
                        disabled={Boolean(accionEnCurso)}
                      >
                        <Icon name="check" size={15} />
                        {accionEnCurso === `${tarea.codigo}-completar`
                          ? 'Completando…'
                          : 'Completar'}
                      </button>
                    ) : (
                      <button
                        className="boton secundario compacto"
                        onClick={() => actuar(tarea, 'iniciar')}
                        disabled={Boolean(accionEnCurso)}
                      >
                        <Icon name="preparacion" size={15} />
                        {accionEnCurso === `${tarea.codigo}-iniciar`
                          ? 'Iniciando…'
                          : 'Iniciar'}
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside className="columna-ruta">
          <section className="panel ruta-panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Ruta de limpieza</h2>
                <p>Recorrido óptimo con distancias del grafo.</p>
              </div>
              <Icon name="ruta" size={20} />
            </header>

            <div className="selector-partida">
              <label>
                <span>Punto de partida</span>
                <select
                  value={puntoPartida}
                  onChange={(evento) => recalcular(evento.target.value)}
                  disabled={calculando}
                >
                  {datos.puntosPartida.map((punto) => (
                    <option key={punto.codigo} value={punto.codigo}>
                      {punto.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <span className="distancia-ruta">
                <strong>{numero(distanciaTotal)}</strong> m
              </span>
            </div>

            {calculando ? (
              <Cargando texto="Calculando ruta óptima…" />
            ) : ruta.length === 0 ? (
              <Vacio titulo="No hay paradas pendientes" icono="ruta" />
            ) : (
              <ol className="linea-ruta">
                <li className="parada-origen">
                  <span className="parada-punto" />
                  <div>
                    <strong>Inicio</strong>
                    <span>
                      {datos.puntosPartida.find((p) => p.codigo === puntoPartida)
                        ?.nombre || puntoPartida}
                    </span>
                  </div>
                </li>
                {ruta.map((parada, indice) => (
                  <li key={parada.codigo} className="parada">
                    <span className="parada-punto">{indice + 1}</span>
                    <div className="parada-contenido">
                      <header>
                        <strong>
                          {parada.codigo} · {parada.nombre}
                        </strong>
                        <span>
                          {parada.alcanzable
                            ? `${numero(parada.distancia)} m`
                            : 'Inalcanzable'}
                        </span>
                      </header>
                      <p>
                        {parada.camino?.length
                          ? parada.camino.join(' → ')
                          : `Desde ${parada.desde}`}
                      </p>
                      {parada.estado && (
                        <Badge tono="neutro">{parada.estado.descripcion}</Badge>
                      )}
                    </div>
                  </li>
                ))}
              </ol>
            )}

            <p className="nota-pie">
              La distancia se mide en metros sobre los pasillos activos. Cerrar un pasillo
              en el mapa puede volver inalcanzable una unidad.
            </p>
          </section>

          <section className="panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Historial</h2>
                <p>Preparaciones completadas y duración real.</p>
              </div>
            </header>
            {datos.historial.length === 0 ? (
              <Vacio titulo="Sin preparaciones completadas" icono="preparacion" />
            ) : (
              <div className="historial-preparacion">
                {datos.historial.map((tarea, indice) => (
                  <div key={`${tarea.codigo}-${indice}`} className="historial-item">
                    <span className="historial-check">
                      <Icon name="check" size={14} />
                    </span>
                    <div>
                      <strong>{tarea.codigo}</strong>
                      <span>{tarea.nombre}</span>
                    </div>
                    <span className="historial-tiempo">
                      {minutos(tarea.duracionSegundos)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>

      <footer className="leyenda-preparacion">
        <span>
          <Icon name="info" size={15} />
          {datos.prioridad.uno}
        </span>
        <span>{datos.prioridad.dos}</span>
      </footer>
    </div>
  )
}

function TarjetaIndicador({ icono, etiqueta, valor, detalle, tono }) {
  return (
    <article className={`tarjeta-indicador tono-${tono}`}>
      <span className="tarjeta-indicador-icono">
        <Icon name={icono} size={20} />
      </span>
      <div>
        <span>{etiqueta}</span>
        <strong>{valor}</strong>
        <small>{detalle}</small>
      </div>
    </article>
  )
}
