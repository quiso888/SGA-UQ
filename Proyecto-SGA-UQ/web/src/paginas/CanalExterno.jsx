import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Badge,
  CabeceraSeccion,
  Cargando,
  Campo,
  Vacio,
} from '../components/ui.jsx'
import { diferenciaDias, fecha, fechaHora, hoyISO, moneda, sumarDias } from '../utils.js'

const TONOS = {
  PENDIENTE: 'amarillo',
  ACEPTADA: 'verde',
  RECHAZADA: 'rojo',
}

export default function CanalExterno({ version, notificar }) {
  const [datos, setDatos] = useState(null)
  const [apartamentos, setApartamentos] = useState([])
  const [error, setError] = useState(null)
  const [procesando, setProcesando] = useState(false)
  const [identificador, setIdentificador] = useState('')
  const [nombre, setNombre] = useState('')
  const [documento, setDocumento] = useState('')
  const [correo, setCorreo] = useState('')
  const [telefono, setTelefono] = useState('')
  const [apartamento, setApartamento] = useState('')
  const [entrada, setEntrada] = useState(sumarDias(hoyISO(), 3))
  const [salida, setSalida] = useState(sumarDias(hoyISO(), 5))
  const [personas, setPersonas] = useState(2)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vigente = true
    setError(null)
    Promise.all([api.canalExterno(), api.temporadas()])
      .then(([canal, temporadas]) => {
        if (!vigente) return
        setDatos(canal)
        setApartamentos(temporadas.apartamentos || [])
        setApartamento((actual) => actual || temporadas.apartamentos?.[0]?.codigo || '')
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version])

  if (error) {
    return (
      <Vacio titulo="No se pudo cargar el canal externo" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Consultando solicitudes del canal externo…" />
  }

  const resumen = datos.resumen
  const nombreApartamento = (codigo) =>
    apartamentos.find((item) => item.codigo === codigo)?.nombre || ''

  const procesar = async () => {
    setProcesando(true)
    try {
      const respuesta = await api.procesarCanal()
      const detalle = respuesta.creadas?.length
        ? ` ${respuesta.creadas.map((r) => r.codigo).join(', ')}.`
        : ''
      notificar(`${respuesta.mensaje}${detalle}`)
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setProcesando(false)
    }
  }

  const encolar = async (evento) => {
    evento.preventDefault()
    if (!nombre.trim() || !apartamento) {
      notificar('Indique el nombre del solicitante y el apartamento.', 'error')
      return
    }
    if (diferenciaDias(entrada, salida) <= 0) {
      notificar('La salida debe ser posterior a la entrada.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.crearSolicitud({
        identificador: identificador.trim() || null,
        nombre: nombre.trim(),
        documento: documento.trim(),
        correo,
        telefono,
        apartamento,
        entrada,
        salida,
        personas: Number(personas),
      })
      notificar(respuesta.mensaje)
      setIdentificador('')
      setNombre('')
      setDocumento('')
      setCorreo('')
      setTelefono('')
      setPersonas(2)
    } catch (problema) {
      notificar(problema.message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina canal">
      <CabeceraSeccion
        eyebrow="Integración con plataformas"
        titulo="Canal externo"
        descripcion="Las solicitudes se atienden estrictamente en orden de llegada (FIFO). Un conflicto no bloquea la cola: se registra y se continúa."
        acciones={
          <button
            className="boton primario"
            onClick={procesar}
            disabled={procesando || resumen.pendientes === 0}
          >
            <Icon name="rotar" size={16} />
            {procesando
              ? 'Procesando…'
              : `Procesar cola (${resumen.pendientes})`}
          </button>
        }
      />

      <div className="indicadores-canal">
        <IndicadorCanal
          etiqueta="En espera"
          valor={resumen.pendientes}
          tono="amarillo"
          icono="reloj"
        />
        <IndicadorCanal
          etiqueta="Aceptadas"
          valor={resumen.aceptadas}
          tono="verde"
          icono="check"
        />
        <IndicadorCanal
          etiqueta="Rechazadas"
          valor={resumen.rechazadas}
          tono="rojo"
          icono="close"
        />
        <IndicadorCanal
          etiqueta="Conflictos registrados"
          valor={resumen.conflictos}
          tono="neutro"
          icono="alerta"
        />
      </div>

      <div className="disposicion-canal">
        <div className="columna-canal">
          <section className="panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Cola de solicitudes</h2>
                <p>Se procesan de arriba hacia abajo, una sola vez.</p>
              </div>
              <Badge tono={resumen.pendientes ? 'amarillo' : 'verde'} punto>
                {resumen.pendientes ? 'Con pendientes' : 'Cola vacía'}
              </Badge>
            </header>

            {datos.pendientes.length === 0 ? (
              <Vacio titulo="No hay solicitudes en espera" icono="canal">
                <p>Encole una solicitud desde el formulario para probar el FIFO.</p>
              </Vacio>
            ) : (
              <ol className="cola-solicitudes">
                {datos.pendientes.map((solicitud, indice) => (
                  <li key={solicitud.identificador}>
                    <span className="cola-turno">{indice + 1}</span>
                    <div className="cola-contenido">
                      <div className="cola-cabecera">
                        <strong>{solicitud.nombre}</strong>
                        <Badge tono="amarillo">{solicitud.identificador}</Badge>
                      </div>
                      <p>
                        {solicitud.apartamento} · {fecha(solicitud.entrada)} →{' '}
                        {fecha(solicitud.salida)} · {solicitud.personas} personas
                      </p>
                      <small>Recibida {fechaHora(solicitud.recibida)}</small>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>

          <section className="panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Historial de procesamiento</h2>
                <p>Trazabilidad de cada solicitud atendida.</p>
              </div>
            </header>
            {datos.procesadas.length === 0 ? (
              <Vacio titulo="Aún no se han procesado solicitudes" icono="canal" />
            ) : (
              <div className="lista-procesadas">
                {datos.procesadas.map((solicitud) => (
                  <article key={solicitud.identificador} className="procesada">
                    <header>
                      <strong>{solicitud.identificador}</strong>
                      <Badge
                        tono={TONOS[solicitud.estado.clave] || 'neutro'}
                        punto
                      >
                        {solicitud.estado.descripcion}
                      </Badge>
                    </header>
                    <p>
                      {solicitud.nombre} · {solicitud.apartamento} ·{' '}
                      {fecha(solicitud.entrada)} → {fecha(solicitud.salida)}
                    </p>
                    <small>{solicitud.motivo}</small>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="panel">
            <header className="panel-cabecera simple">
              <div>
                <h2>Conflictos detectados</h2>
                <p>Solicitudes rechazadas y reserva que ocupaba el inventario.</p>
              </div>
              <Badge tono={datos.conflictos.length ? 'rojo' : 'neutro'}>
                {datos.conflictos.length}
              </Badge>
            </header>
            {datos.conflictos.length === 0 ? (
              <Vacio titulo="Sin conflictos" icono="check">
                <p>No se han presentado solapamientos en el canal externo.</p>
              </Vacio>
            ) : (
              <div className="lista-conflictos">
                {datos.conflictos.map((conflicto, indice) => (
                  <article key={`${conflicto.solicitud}-${indice}`} className="conflicto">
                    <span className="conflicto-icono">
                      <Icon name="alerta" size={17} />
                    </span>
                    <div>
                      <header>
                        <strong>{conflicto.solicitud}</strong>
                        <span>{conflicto.apartamento}</span>
                      </header>
                      <p>{conflicto.motivo}</p>
                      <small>
                        Reserva en conflicto: {conflicto.reservaEnConflicto || '—'} ·{' '}
                        {fechaHora(conflicto.fecha)}
                      </small>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>

        <aside className="columna-canal">
          <form className="panel formulario-canal" onSubmit={encolar}>
            <header className="panel-cabecera simple">
              <div>
                <h2>Nueva solicitud</h2>
                <p>Simula la llegada de una reserva desde una plataforma.</p>
              </div>
              <Icon name="canal" size={20} />
            </header>

            <div className="formulario-apilado">
              <Campo etiqueta="Identificador externo" ayuda="Opcional. Se genera uno automático.">
                <input
                  value={identificador}
                  onChange={(evento) => setIdentificador(evento.target.value)}
                  placeholder="EXT-2026-003"
                />
              </Campo>
              <Campo etiqueta="Nombre del solicitante">
                <input
                  value={nombre}
                  onChange={(evento) => setNombre(evento.target.value)}
                  placeholder="Ricardo Peña"
                  required
                />
              </Campo>
              <div className="formulario-fila">
                <Campo etiqueta="Documento">
                  <input
                    value={documento}
                    onChange={(evento) => setDocumento(evento.target.value)}
                    placeholder="1.152-334.889"
                  />
                </Campo>
                <Campo etiqueta="Personas">
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={personas}
                    onChange={(evento) =>
                      setPersonas(Number(evento.target.value) || 1)
                    }
                  />
                </Campo>
              </div>
              <div className="formulario-fila">
                <Campo etiqueta="Correo">
                  <input
                    type="email"
                    value={correo}
                    onChange={(evento) => setCorreo(evento.target.value)}
                    placeholder="correo@plataforma.com"
                  />
                </Campo>
                <Campo etiqueta="Teléfono">
                  <input
                    value={telefono}
                    onChange={(evento) => setTelefono(evento.target.value)}
                  />
                </Campo>
              </div>
              <Campo etiqueta="Apartamento solicitado">
                <select
                  value={apartamento}
                  onChange={(evento) => setApartamento(evento.target.value)}
                  required
                >
                  <option value="">Seleccione…</option>
                  {apartamentos.map((item) => (
                    <option key={item.codigo} value={item.codigo}>
                      {item.codigo} · {item.nombre}
                    </option>
                  ))}
                </select>
              </Campo>
              <div className="formulario-fila">
                <Campo etiqueta="Entrada">
                  <input
                    type="date"
                    value={entrada}
                    min={hoyISO()}
                    onChange={(evento) => setEntrada(evento.target.value)}
                  />
                </Campo>
                <Campo etiqueta="Salida">
                  <input
                    type="date"
                    value={salida}
                    min={sumarDias(entrada, 1)}
                    onChange={(evento) => setSalida(evento.target.value)}
                  />
                </Campo>
              </div>
            </div>

            <div className="aviso info">
              <Icon name="info" size={17} />
              <p>
                La solicitud se encola, no se reserva de inmediato. Use «Procesar cola»
                para ejecutar el FIFO y detectar conflictos.
              </p>
            </div>

            <button className="boton primario ancho" type="submit" disabled={enviando}>
              <Icon name="plus" size={16} />
              {enviando ? 'Encolando…' : 'Encolar solicitud'}
            </button>
          </form>

          <section className="panel resumen-canal">
            <h3>Salud del canal</h3>
            <p>
              {resumen.pendientes === 0
                ? 'La cola está al día. Las nuevas solicitudes se atenderán en el próximo procesamiento.'
                : `Hay ${resumen.pendientes} solicitud(es) esperando. Se atenderán en orden de llegada.`}
            </p>
            <div className="mini-datos">
              <div>
                <span>Tasa de aceptación</span>
                <strong>
                  {resumen.aceptadas + resumen.rechazadas > 0
                    ? `${Math.round(
                        (resumen.aceptadas /
                          (resumen.aceptadas + resumen.rechazadas)) *
                          100,
                      )} %`
                    : '—'}
                </strong>
              </div>
              <div>
                <span>Conflictos</span>
                <strong>{resumen.conflictos}</strong>
              </div>
            </div>
            <p className="nota-pie">
              El valor de referencia de una solicitud aceptada es el que calcula el motor
              de tarifas al crear la reserva.
            </p>
          </section>
        </aside>
      </div>
    </div>
  )
}

function IndicadorCanal({ etiqueta, valor, tono, icono }) {
  return (
    <article className={`indicador-canal tono-${tono}`}>
      <span className="indicador-canal-icono">
        <Icon name={icono} size={20} />
      </span>
      <div>
        <strong>{valor}</strong>
        <span>{etiqueta}</span>
      </div>
    </article>
  )
}
